---
title: Runtime library
description: What ships in `:jvm-runtime` and how generated code uses it at execution time.
headline: Documentation
---

# Runtime library

The `:jvm-runtime` module is the only thing the generated validation code depends on at
execution time. It is deliberately small: there is no rule engine, no descriptor scanning,
and no reflection-driven dispatch. The module ships the contracts that generated message
and builder classes implement, the Protobuf types used to describe violations, the
exception raised when a `build()` call fails, and a single registry through which custom
validators reach generated code.

This page is the reverse view of “[Java code generation](java-code-generation.md)”. That
page describes what the renderer emits; this one describes the surface that the emitted
code calls into.

## What ships in `:jvm-runtime`

Five groups of types live in the runtime library:

| Group                  | Types                                                                                | Role                                                                                          |
|------------------------|--------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| Generated-class mixins | `ValidatableMessage`, `ValidatingBuilder`                                            | Interfaces the generated message and its builder implement.                                   |
| Violation Protobuf     | `ValidationError`, `ConstraintViolation`, `TemplateString`                           | The structured shape of a violation report.                                                   |
| Exception              | `ValidationException`                                                                | Thrown by `Builder.build()` when validation fails.                                            |
| Markers                | `@Validated`, `@NonValidated`                                                        | Documentary annotations placed on `build()` and `buildPartial()` return types; not retained at runtime. |
| Validator extension    | `MessageValidator`, `ValidatorRegistry`, `DetectedViolation`, `RuntimeErrorPlaceholder` | Runtime SPI for attaching custom checks to a message type, including third-party messages. |

Two utility entry points round the surface out: the static method `Validate.check(message)`
and the Kotlin extensions `M.checkValid()` and `M.copy { … }` in
[`MessageExtensions.kt`][message-extensions].

## The generated-class contracts

Every message that goes through the Java renderer becomes a `ValidatableMessage`, and its
builder becomes a `ValidatingBuilder<M>`. These two interfaces are the seam between
generated code and runtime code.

`ValidatableMessage` declares the `validate(parentPath, parentName)` method whose body
the renderer assembles from the snippets that each `OptionGenerator` produced. It returns
`Optional<ValidationError>` rather than throwing, so a built message can be re-validated
without paying for an exception:

<embed-code
  file="$runtime/src/main/java/io/spine/validation/ValidatableMessage.java"
  start="public interface ValidatableMessage"
  end="^\}">
</embed-code>
```java
public interface ValidatableMessage extends Message {

    default Optional<ValidationError> validate() {
        var noParentPath = FieldPath.getDefaultInstance();
        return validate(noParentPath, null);
    }

    Optional<ValidationError> validate(FieldPath parentPath, @Nullable TypeName parentName);
}
```

The two-argument overload is what `(validate)`-driven nested validation calls. When the
outer message's generated code descends into a nested field, it passes its own
`parentPath` and `parentName` so that any violation reported by the nested type carries
the path back to the validation root. The single-argument default is what application code
typically calls when it wants to verify an already-built message.

`ValidatingBuilder` is the builder-side counterpart:

<embed-code
  file="$runtime/src/main/java/io/spine/validation/ValidatingBuilder.java"
  start="public interface ValidatingBuilder"
  end="^\}">
</embed-code>
```java
public interface ValidatingBuilder<M extends Message> extends Message.Builder {

    @Override
    @Validated M build() throws ValidationException;

    @Override
    @NonValidated M buildPartial();

    @Deprecated
    default @Validated M vBuild() throws ValidationException {
        return build();
    }
}
```

The injector wraps the existing `build()` so it calls `validate()` and throws
`ValidationException` if any violation is reported. `buildPartial()` is deliberately left
unwrapped — it is the documented escape hatch for callers who need to assemble a message
that does not yet satisfy its constraints. The `@Validated` and `@NonValidated` markers
make the difference visible at every call site.

## Source markers: `@Validated` and `@NonValidated`

Both annotations have `RetentionPolicy.CLASS` and `TYPE_USE`/`TYPE_PARAMETER` targets.
They are documentation, not behavior — they are not consulted by the runtime, the
generator, or any classloader. Their job is to tell readers and IDEs that one method
returns a checked message and another does not. The injector applies them to the
generated `build()` and `buildPartial()` signatures; user code is free to apply them to
its own methods that wrap a builder.

Because the retention is `CLASS`, they are recorded in the bytecode but not visible at
runtime via reflection. Treat them as a typed comment.

## The validation entry point

For a message that is *already* built, application code typically goes through
[`Validate`][validate-class]. `Validate.check(message)` is the throwing form;
`Validate.violationsOf(message)` is the non-throwing form:

<embed-code
  file="$runtime/src/main/java/io/spine/validation/Validate.java"
  start="public static <M extends Message> M check"
  end="^    \}">
</embed-code>
```java
public static <M extends Message> M check(M message) throws ValidationException {
    checkNotNull(message);
    var violations = violationsOf(message);
    if (!violations.isEmpty()) {
        throw new ValidationException(violations);
    }
    return message;
}
```

`violationsOf` distinguishes two cases:

- If the message implements `ValidatableMessage`, `Validate` calls its `validate()` and
  returns the violations from the resulting `ValidationError`. All built-in and custom
  options run; so do any registered `MessageValidator`s, because the generated
  `validate()` consults `ValidatorRegistry` at the end.
- Otherwise — typically a message generated outside this build — `Validate` skips
  straight to `ValidatorRegistry.validate(message)`, which applies registered validators
  but no compiled options.

If the input is a packed `google.protobuf.Any`, `Validate` unpacks it before dispatching,
falling back to a stderr warning when the type URL is not in `KnownTypes`.

The Kotlin extension `M.checkValid()` in [`MessageExtensions.kt`][message-extensions]
delegates to `Validate.check`. The same file also provides `M.copy { … }`, a helper that
creates a builder from an existing message, applies a configuration block, and calls
`build()` — so any modification made through `copy` is validated like any other build.

## How violations are surfaced

A violation is described by `ConstraintViolation` (in
[`validation_error.proto`][validation-error-proto]). The message is intentionally
self-contained so that violations can be returned across processes:

```proto
message ConstraintViolation {
    TemplateString message = 8;
    string type_name = 7;
    base.FieldPath field_path = 3;
    google.protobuf.Any field_value = 4;
    // ... deprecated fields elided
}
```

- `message` is a `TemplateString` — the placeholder format that the generator and runtime
  both speak. Values are filled into the template's `placeholder_value` map at the moment
  the violation is created; rendering to a final string happens later, when a reader calls
  `TemplateString.format()`. Carrying values rather than rendered text keeps the violation
  inspectable: a caller can still read `field.value` or `regex.pattern` without parsing
  the message.
- `type_name` and `field_path` describe **where** the violation occurred. For nested
  validation triggered by `(validate)`, `type_name` is the *root* type that initiated the
  walk and `field_path` is the full dotted path to the offending field. This is why
  `validate(parentPath, parentName)` exists on `ValidatableMessage`: a nested call uses
  the outer caller's path and name as the prefix.
- `field_value` is the offending value, packed into `Any`. Primitive fields use the
  matching wrapper type (`StringValue`, `Int32Value`, …). Message-valued fields are
  packed directly.

`ValidationError` is the multi-violation envelope:

```proto
message ValidationError {
    repeated ConstraintViolation constraint_violation = 1;
}
```

Violation accumulation is additive. A generated `validate()` does not short-circuit on the
first failure; it appends to a `List<ConstraintViolation>` and only at the end decides
whether to wrap the list in a `ValidationError`. This is what lets the runtime report
every problem in one pass.

`TemplateString` (in [`error_message.proto`][error-message-proto]) is the format every
generator emits and every reader resolves. Substitution happens via
`TemplateString.format()` (in Kotlin, [`TemplateStringExts.kt`][template-string-exts]) or
the static `TemplateStrings.format(...)` (in Java). The placeholder names the runtime
itself fills in are enumerated by [`RuntimeErrorPlaceholder`][runtime-error-placeholder]
— `field.path`, `field.value`, `field.type`, `message.type`, `parent.type`, plus
option-specific entries such as `regex.pattern` and `range.value`. Note that this enum
mirrors `io.spine.tools.validation.ErrorPlaceholder` in `:context`; the two must be kept
in sync.

`ViolationText` ([`ViolationText.java`][violation-text]) is the diagnostic formatter the
exception uses to produce a human-readable string from a list of `ConstraintViolation`s.

## `ValidationException`

`ValidationException` is what falls out of a failing `Builder.build()`. It is a
`RuntimeException`, so the `throws` declaration on generated `build()` methods is
documentation, not a checked contract.

The exception stores an immutable copy of the reported `ConstraintViolation`s, exposes
them through `getConstraintViolations()`, formats diagnostics with `ViolationText`, and
implements `ErrorWithMessage<ValidationError>`. Framework code can therefore obtain the
serialisable Protobuf form via `asMessage()` and ship the report across a wire without
forcing clients to link against `:jvm-runtime`.

For richer error envelopes — for example, attaching an error code or a typed
`MessageClass` to the report — `:jvm-runtime` provides
[`ExceptionFactory`][exception-factory]. It is `@Internal` and intended for frameworks
built on top of Validation (Spine Server uses it to raise `CommandValidationError` and
`EventValidationError`); application code should keep using `ValidationException`.

## The validator extension hook

The runtime extension surface is a single SPI: [`MessageValidator<M>`][message-validator].
A `MessageValidator` knows nothing about the generator, the model, or `.proto` options. It
receives a built message and returns a list of `DetectedViolation`s.

<embed-code
  file="$runtime/src/main/kotlin/io/spine/validation/MessageValidator.kt"
  start="@SPI"
  end="^\}">
</embed-code>
```kotlin
@SPI
public interface MessageValidator<M : Message> {

    public fun validate(message: M): List<DetectedViolation>
}
```

[`DetectedViolation`][detected-violation] is the validator-side analogue of
`ConstraintViolation`. It carries a `TemplateString`, an optional `FieldPath`, and an
optional offending value:

<embed-code
  file="$runtime/src/main/kotlin/io/spine/validation/DetectedViolation.kt"
  start="public abstract class DetectedViolation"
  end="^\)">
</embed-code>
```kotlin
public abstract class DetectedViolation(
    public val message: TemplateString,
    public val fieldPath: FieldPath?,
    public val fieldValue: Any?,
)
```

Two concrete subclasses cover the common cases: `FieldViolation` (a violation tied to a
specific field) and `MessageViolation` (a message-level rule that does not point at a
single field). The library converts each `DetectedViolation` to a `ConstraintViolation`
before reporting; the validator does not need to know about `Any`-packing, parent paths,
or type names.

### `ValidatorRegistry`

`ValidatorRegistry` is the singleton through which generated code reaches every
registered validator. It is loaded eagerly from `ServiceLoader<MessageValidator>` on first
access:

```kotlin
@VisibleForTesting
internal fun loadFromServiceLoader() {
    val loader = ServiceLoader.load(MessageValidator::class.java)
    loader.forEach { validator ->
        @Suppress("UNCHECKED_CAST")
        val casted = validator as MessageValidator<Message>
        val messageType = casted.messageClass()
        add(messageType, casted)
    }
}
```

An internal extension function, `MessageValidator<M>.messageClass()` (declared in
[`ValidatorRegistry.kt`][validator-registry]), recovers the `M` type parameter via Guava's
`TypeToken`. It is not part of the SPI surface that implementers see. For `ServiceLoader`
discovery, the concrete message type must be recoverable from the validator class: direct
implementations such as `MessageValidator<SomeConcreteType>` are the clearest shape, and
base classes are fine as long as `TypeToken` still resolves `M` to a concrete message
class.

The registry exposes `add`, `remove`, `get`, `clear`, and two `validate` overloads —
`validate(message)` for top-level use and `validate(message, parentPath, parentName)` for
the nested case used by generated code. The two-argument variant prefixes every reported
field path with `parentPath` and stamps the report with `parentName ?: TypeName.of(message)`,
mirroring what `ValidatableMessage.validate(parentPath, parentName)` does for compiled
constraints. Generated code therefore uses the registry uniformly whether the validated
type is locally defined or external.

A reserved placeholder, `VALIDATOR_PLACEHOLDER` (the literal `"validator"`), is filled in
automatically with the fully-qualified class name of the validator that produced the
violation. Validators whose template strings reference `${validator}` see that name in
the rendered diagnostic without having to look it up themselves.

### Local versus external messages

The KDoc on `MessageValidator` is explicit about the two scenarios it serves and is the
canonical reference for behavior questions; the short version is:

- **Local messages** — types defined in the consumer's own `.proto` files. The generated
  `validate()` for a local message both runs its compiled constraints and consults
  `ValidatorRegistry` at the end. Adding a `MessageValidator<MyMessage>` therefore layers
  a custom check on top of the generated one.
- **External messages** — types whose generated classes are out of the consumer's reach
  (third-party Protobufs, well-known types). They never go through the Java renderer, so
  there is no compiled `validate()` to invoke. A local message reaches their validators
  only through fields marked with `(validate) = true`; the generated `(validate)` code
  calls `ValidatorRegistry.validate(...)` for singular fields, repeated fields, and map
  values of that external type. A standalone instance of an external type passed to a
  non-local API is **not** validated automatically; callers must invoke `Validate.check`
  or `ValidatorRegistry.validate(...)` themselves.

The bundled `TimestampValidator` ([`TimestampValidator.kt`][timestamp-validator]) is a
small, real example that ships with `:jvm-runtime`: it is `@AutoService`-registered for
`com.google.protobuf.Timestamp`, returns `FieldViolation`s when seconds or nanos fall
outside `Timestamps.MIN_VALUE`/`MAX_VALUE`, and otherwise stays out of the way. A consumer
that marks a `Timestamp` field in a local message with `(validate) = true` gets the check
for free as soon as `:jvm-runtime` is on the classpath.

### Discovery and registration

`ValidatorRegistry` accepts validators in three ways:

1. **`ServiceLoader`** — the registry's `init` block calls `loadFromServiceLoader()`. The
   convenient way to wire this up on the JVM is `@AutoService(MessageValidator::class)`,
   which generates the `META-INF/services/io.spine.validation.MessageValidator` entry at
   compile time. This is what the bundled `TimestampValidator` uses, and what the User's
   Guide recommends.
2. **Explicit `add(...)`** — `ValidatorRegistry.add(MyMessage::class, MyValidator())` (or
   the `Class<?>` overload for Java callers). Useful in tests or in startup code that
   wants tight control over which validators are active.
3. **Removal and replacement** — `remove(cls)` clears all validators for a type;
   `clear()` resets the registry. Several validators per message type are allowed; their
   ordering is unspecified, and their reports are concatenated.

Validators discovered through `ServiceLoader` must have a public no-arg constructor.
Whether discovered or registered explicitly, validator instances must be safe to invoke
concurrently: `ValidatorRegistry` is annotated `@ThreadSafe` and makes no per-call
locking guarantees beyond the registry's own bookkeeping.

There is no `@Validator` annotation in the library itself; the discovery contract is the
`ServiceLoader` SPI plus the `MessageValidator` interface. `@AutoService(MessageValidator::class)`
from Google AutoService is the convenient way to generate the corresponding
`META-INF/services` entry on the JVM, but any other mechanism that produces the same
service descriptor works equivalently.

## Constraints on the runtime surface

A few invariants are worth keeping in mind when working on `:jvm-runtime`:

- **No descriptor scanning.** The runtime never reads `.proto` descriptors to discover
  rules. Everything that can be known at build time lives in generated code; the runtime
  carries only what must be carried (registered validators, the violation Protobuf types,
  the exception).
- **No reflection-driven dispatch in the hot path.** `ValidatorRegistry` does a single
  `ConcurrentHashMap` lookup keyed by qualified class name. The reflection in
  `messageClass()` runs once at registration time, not per validation.
- **Stable wire shape.** `ValidationError`, `ConstraintViolation`, and `TemplateString`
  are public Protobuf types with type URLs at `type.spine.io`. They cross process
  boundaries; field numbers are not free to reshuffle.
- **No dependency on logging.** `Validate` deliberately uses `System.err` for the rare
  warning path so that `:jvm-runtime` does not pull Spine Logging into a consumer's
  classpath.

These constraints are why the runtime stays small and why the design centre of gravity is
in `:context` and `:java`: anything that *can* be decided at build time *should* be.

## What's next

- [Extension points](extension-points.md) — how `MessageValidator`, `ValidatorRegistry`,
  and the `ValidationOption` SPI together form Validation's public extension surface.
- [Adding a new built-in validation option](adding-a-built-in-option.md) — the
  contributor walkthrough that touches the runtime when an option needs new helpers or
  error placeholders.
- User's Guide — [`MessageValidator` overview](../user/04-validators/) and
  [Using `ValidatorRegistry`](../user/04-validators/validator-registry.md) for the
  consumer-facing view of the same APIs.

[message-validator]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/MessageValidator.kt
[validator-registry]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/ValidatorRegistry.kt
[detected-violation]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/DetectedViolation.kt
[validation-error-proto]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/proto/spine/validation/validation_error.proto
[error-message-proto]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/proto/spine/validation/error_message.proto
[violation-text]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/java/io/spine/validation/ViolationText.java
[validate-class]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/java/io/spine/validation/Validate.java
[exception-factory]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/java/io/spine/validation/ExceptionFactory.java
[message-extensions]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/MessageExtensions.kt
[template-string-exts]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/TemplateStringExts.kt
[runtime-error-placeholder]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/RuntimeErrorPlaceholder.kt
[timestamp-validator]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/TimestampValidator.kt
