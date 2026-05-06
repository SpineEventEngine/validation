---
title: Extension points
description: The public extension surface of Spine Validation, viewed end-to-end.
headline: Documentation
---

# Extension points

Spine Validation exposes two extension points and only two. They sit on opposite sides of
the compile-time / runtime split:

- The [`ValidationOption`][validation-option-spi] SPI (build time) — adds a *new
  validation option* with its own model, codegen, and runtime helpers.
- The [`MessageValidator`][message-validator] SPI (runtime) — adds a *custom check on a
  specific message type*, executed alongside the compiled constraints.

Each surface has a corresponding User's Guide section that explains how to *use* it:
[Custom validation](../05-custom-validation/) for `ValidationOption`, and
[Using validators](../04-validators/) for `MessageValidator`. This page is the
contributor-side view: what each surface guarantees, how discovery works, what an
implementation may and may not do, and why.

The earlier sections of the Developer's Guide cover each surface in detail —
[The validation model](validation-model.md) and [Java code generation](java-code-generation.md)
for the build-time half, and [Runtime library](runtime-library.md) for the runtime half.
This page consolidates the two into a single picture.

## The two surfaces at a glance

| Aspect            | `ValidationOption`                                                    | `MessageValidator`                                                      |
|-------------------|-----------------------------------------------------------------------|-------------------------------------------------------------------------|
| Granularity       | A new `.proto` option, applicable to many messages.                   | A custom check on one specific message type.                            |
| When it runs      | Build time (codegen) plus optional runtime helpers it ships itself.   | Runtime: after compiled checks for local messages; via `ValidatorRegistry` for external/direct validation. |
| Inputs            | Reads the model in `:context`, emits Java via `:java`.                | Receives a built `Message`, returns `List<DetectedViolation>`.          |
| Discovery         | `ServiceLoader<ValidationOption>` in the plugin's classpath.          | `ServiceLoader<MessageValidator>` in the consumer's classpath.          |
| Required by       | Adding a new constraint vocabulary (`(when)`, `(currency)`, …).       | Constraints that cannot be expressed declaratively, or external types.  |
| Lives in          | Defined in `:java`; implementations live in their own modules.        | Defined in `:jvm-runtime`; implementations live in any consumer module. |

The two are deliberately not interchangeable. A `ValidationOption` is the right choice
when the same constraint vocabulary applies across many messages and benefits from
declarative configuration in `.proto` files. A `MessageValidator` is the right choice
when the constraint is specific to one message type, or when the message type is external
and cannot carry options at all.

## The `ValidationOption` SPI end-to-end

The [`ValidationOption`][validation-option-spi] SPI is intentionally narrow. A custom
option contributes exactly three things, matching the build-time pipeline:

1. `reactions` — reaction instances that subscribe to the upstream
   `FieldOptionDiscovered` / `OneofOptionDiscovered` / `MessageOptionDiscovered` events,
   filter by `OPTION_NAME`, validate applicability, and emit a `*Discovered` domain
   event. See “[The validation model](validation-model.md#the-lifecycle-of-an-option)”.
2. `view` — Protobuf-declared projections that fold those domain events into queryable
   state. See “[The validation model](validation-model.md#the-projection)”.
3. `generator` — an [`OptionGenerator`][option-generator] subclass that queries the
   projection and emits one `SingleOptionCode` per option application. See
   “[Java code generation](java-code-generation.md#the-optiongenerator-spi)”.

`JavaValidationPlugin` discovers SPI implementations through `ServiceLoader` and folds
them into the same plugin registration that brings in the built-ins:

```kotlin
public open class JavaValidationPlugin : ValidationPlugin(
    renderers = listOf(
        JavaValidationRenderer(customGenerators = customOptions.map { it.generator }),
        SetOnceRenderer()
    ),
    views = customOptions.flatMap { it.view }.toSet(),
    reactions = customOptions.flatMap { it.reactions }.toSet(),
)

private val customOptions: List<ValidationOption> by lazy {
    ServiceLoader.load(ValidationOption::class.java)
        .filterNotNull()
}
```

From the model's point of view, custom reactions and views are indistinguishable from
the built-ins. From the renderer's point of view, the custom `generator` receives the
same `Querying` and `TypeSystem` as the built-ins and contributes to the same
`validate()` method. Built-ins and custom options share one pipeline, not two.

### Discovery

A `ValidationOption` implementation is discovered through the standard Java
`ServiceLoader` SPI:

- The implementing class must be on the **plugin's classpath**, not the application's.
  In a Gradle build that consumes Validation, this means the module declaring the option
  is added to the Spine Compiler classpath (see
  [Pass the option to the Compiler](../05-custom-validation/pass-to-compiler.md)).
- A `META-INF/services/io.spine.tools.validation.java.ValidationOption` entry must list
  the implementing class. The conventional way to generate it is the
  `@AutoService(ValidationOption::class)` annotation processor; any other mechanism that
  produces the same descriptor is equivalent.
- The class must have a public no-arg constructor — the `ServiceLoader` contract.

In addition to the SPI implementation itself, two more pieces of build-time wiring are
required for the option to work: the option's Protobuf descriptor must be discoverable
through `OptionsProvider` (so the Compiler recognises the option when parsing
`.proto` files), and the consumer's build must place the option's module on the
Compiler's classpath. Both are covered in the User's Guide; the SPI itself does not
attempt to encode them.

### Lifecycle

Implementations are constructed eagerly when `JavaValidationPlugin` is loaded — the
`customOptions` lazy is dereferenced in the constructor of the plugin. From that point
on, the same instance is used for the entire build:

- Each `Reaction` instance returned by `reactions` is registered with the Bounded
  Context exactly once. Reactions are stateless by contract.
- Each `View` class returned by `view` is registered once and instantiated by the
  framework as needed. Views accumulate state per projection key.
- The single `generator` instance is the one passed to `JavaValidationRenderer`. The
  renderer calls `inject(querying, typeSystem)` on it before the first `codeFor()`
  invocation, and `codeFor(type)` is called once per message type in the
  `SourceFileSet`. The instance must therefore be safe to reuse across messages within a
  single build — see [Java code generation](java-code-generation.md#the-render-lifecycle).

### Ordering

The order in which custom generators contribute to a generated `validate()` is
unspecified. Generators must not rely on running before or after any built-in or any
other custom generator: each contribution is a self-contained `if (…) { violations.add(…) }`
block, and accumulating violations (rather than short-circuiting) is what lets the
ordering stay free.

The same is true on the model side. Reactions and views run in event-delivery order; a
custom view that needs to fold both its primary event and a companion event must accept
either ordering, exactly the way `RequiredFieldView` accepts `RequiredFieldDiscovered`
and `IfMissingOptionDiscovered` in either order (see
[Companion options](validation-model.md#companion-options)).

## The `MessageValidator` SPI

The runtime extension surface is [`MessageValidator<M>`][message-validator]. Use it for
checks that cannot be expressed in `.proto` options at all — because the rule depends on
multiple fields, on external state, or on a message type whose source the consumer cannot
modify. The full mechanics — the registry, the local-versus-external distinction, the
`${validator}` placeholder, the `DetectedViolation` shape — are described in
[Runtime library](runtime-library.md#the-validator-extension-hook). The summary below
covers only what is specific to *being* an extension point.

### Discovery

`ValidatorRegistry` loads `MessageValidator` implementations through `ServiceLoader` on
first access:

- The implementing class must be on the consumer application's runtime classpath.
- A `META-INF/services/io.spine.validation.MessageValidator` entry must list the
  implementing class. `@AutoService(MessageValidator::class)` is the conventional way to
  produce it; any equivalent mechanism works.
- The class must have a public no-arg constructor.
- The concrete `M` type parameter must be recoverable from the validator class.
  Direct implementations such as `MessageValidator<MyType>` are the clearest shape;
  base classes are fine as long as Guava's `TypeToken` can still resolve `M` to a
  concrete `Message` class.

There is **no** `@Validator` annotation in this library. The discovery contract is the
`ServiceLoader` SPI plus the `MessageValidator` interface. `@AutoService` is one
convenient way to satisfy it on the JVM, not a Validation-specific annotation.

Validators may also be registered explicitly via `ValidatorRegistry.add(...)`, removed
via `remove(...)`, or cleared via `clear()`. Several validators per message type are
allowed; the order in which they run is unspecified and their reports are concatenated.
This is documented in the User's Guide
([Using `ValidatorRegistry`](../04-validators/validator-registry.md)).

### Lifecycle

`MessageValidator` instances are constructed by `ServiceLoader` on first access to
`ValidatorRegistry`, or explicitly by application code before it calls `add(...)`.
Once registered, an instance is retained until `remove(...)` or `clear()` removes it and
is invoked on every matching `validate(...)` call. The registry itself is annotated
`@ThreadSafe` and dispatches concurrently: implementations must therefore be safe to
invoke from multiple threads at once.

### Ordering and composition

`ValidatorRegistry` stores validators keyed by qualified message class name. When a
message of type `M` is validated:

- For a **local** message (one whose generated class is produced by the Java renderer in
  this build), the generated `validate()` first runs every compiled constraint and then
  consults `ValidatorRegistry` for any registered validators on `M`. Compiled checks and
  validators all contribute to the same `ValidationError`.
- For an **external** message (one whose generated class is not produced in this build),
  the registry is the only entry point. A local message reaches external validators only
  through fields marked `(validate) = true`; a standalone external instance is not
  validated unless the caller invokes `Validate.check` or `ValidatorRegistry.validate`
  directly.

The order in which validators of the same message type run is unspecified. As with
custom generators, every validator simply appends to the accumulated list of violations,
which is what keeps the ordering question moot.

## Constraints on what extensions can do

Both SPIs are deliberately narrow. The constraints below are not arbitrary; they fall
out of the compile-time / runtime split that the rest of the architecture is built on
(see [Architecture](architecture.md)).

### `ValidationOption`

- **No file I/O at generation time.** The generator must derive everything from the view
  state populated by reactions. Reading `.proto` files, querying descriptors at runtime,
  or scanning the file system from inside `codeFor()` defeats the model's reason to
  exist — the renderer is supposed to be replaceable with a renderer for another target
  language without touching `:context`.
- **No mutation of the message PSI directly.** A generator returns `SingleOptionCode`;
  placement is the [`ValidationCodeInjector`][validation-code-injector]'s job. Adding
  methods, fields, or interface implementations from inside a generator bypasses the
  conventions for the shape of generated validators (see
  [Java code generation](java-code-generation.md#injecting-the-code-into-the-psi)).
- **No silent failure.** A misapplied option must fail compilation through
  `Compilation.check` / `Compilation.error`, not be quietly skipped. Reactions that
  decide an option does not apply must return `NoReaction`, not throw.
- **No interpreting at runtime.** If an option needs runtime helpers, they must ship in
  a separate module that the generated code calls into — like `:jvm-runtime` does for
  the built-ins. The runtime must not parse `.proto` descriptors to recover what the
  generator already knew.

### `MessageValidator`

- **No descriptor scanning.** The runtime is intentionally free of descriptor-driven
  rule discovery. A validator that wants to apply different rules to different fields
  must do so in code, not by re-deriving a model at runtime.
- **No reflection-driven dispatch in the hot path.** The registry does a single
  `ConcurrentHashMap` lookup keyed by class name. The reflection that recovers `M` from
  the validator's class runs once, at registration. Validators must not extend that
  reflection cost into per-call dispatch.
- **No assumptions about ordering or peers.** A validator must produce a correct report
  regardless of which other validators (built-in, custom, registered explicitly,
  registered through `ServiceLoader`) run alongside it.
- **Thread-safety is on the implementer.** The registry is `@ThreadSafe`; validators
  must be too. Per-call mutable state must be local to the call.
- **Use `DetectedViolation`, not `ConstraintViolation`.** The registry is responsible
  for translating `DetectedViolation` to `ConstraintViolation`, packing values into
  `Any`, prefixing field paths with the parent path, and stamping the type name. A
  validator that bypasses `DetectedViolation` cannot participate in nested validation
  correctly.

### Why these constraints exist

The compile-time / runtime split is what lets the runtime stay small (see
[Runtime library](runtime-library.md#constraints-on-the-runtime-surface)) and the
language-agnostic model stay portable (see
[The validation model](validation-model.md)). Both extension points are designed so
that a well-behaved implementation reinforces that split:

- A `ValidationOption` adds a new constraint vocabulary without forcing a runtime rule
  engine into existence — the constraint becomes inlined Java like every built-in.
- A `MessageValidator` adds a runtime-only check without leaking knowledge of the
  Compiler, the model, or codegen — it is opaque to everything below the
  `MessageValidator` boundary.

The constraints above are how each SPI keeps that property; they are also why neither
SPI exposes more knobs than it does. New extension points should be evaluated against
the same split before they are added.

## What's next

- [Adding a new built-in validation option](adding-a-built-in-option.md) — the
  contributor walkthrough that exercises the `ValidationOption` SPI end-to-end.
- [The validation model](validation-model.md) — what a custom reaction and view look
  like in detail.
- [Java code generation](java-code-generation.md) — what a custom `OptionGenerator`
  hands back to the renderer.
- [Runtime library](runtime-library.md) — what a `MessageValidator` is plugged into.
- User's Guide — [Custom validation](../05-custom-validation/) and
  [Using validators](../04-validators/) for the consumer-facing view of the same SPIs.

[validation-option-spi]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/ValidationOption.kt
[option-generator]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/OptionGenerator.kt
[validation-code-injector]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/ValidationCodeInjector.kt
[message-validator]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/MessageValidator.kt
