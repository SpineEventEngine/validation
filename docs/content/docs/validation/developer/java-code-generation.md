---
title: Java code generation
description: How the Spine Compiler plugin in `:java` produces validation code from the model.
headline: Documentation
---

# Java code generation

The `:java` module turns the populated projections produced by `:context` (see
“[The validation model](validation-model.md)”) into Java code that is injected into
Protobuf-generated message and builder classes. By the time the Spine Compiler invokes
this module, every constraint discovered in the consumer's `.proto` files already lives
in a view, and `:java`'s job is to ask each view what it knows and translate that into
Java.

The work is split across two renderers and a small set of supporting types:

- [`JavaValidationRenderer`][java-validation-renderer] — the main renderer. It walks
  every compilation message, asks each registered [`OptionGenerator`][option-generator]
  for the code that implements its option on that message, and hands the result to
  [`ValidationCodeInjector`][validation-code-injector] for placement inside the message
  and its builder.
- [`SetOnceRenderer`][set-once-renderer] — a separate renderer for `(set_once)`. The
  option does not contribute a check to the `validate()` method; instead it modifies the
  builder so that setters refuse to overwrite an already-assigned value. The render
  pipeline is therefore different enough that it lives apart from
  `JavaValidationRenderer`.

The two renderers and the SPI that lets custom options plug into them are described on
this page.

## The plugin entry point

`JavaValidationPlugin` extends the language-agnostic `ValidationPlugin` from `:context`,
adds the two Java renderers, and folds in any custom options discovered through the
`ValidationOption` SPI:

<embed-code
  file="$java/src/main/kotlin/io/spine/tools/validation/java/JavaValidationPlugin.kt"
  start="public open class JavaValidationPlugin"
  end="^\)">
</embed-code>
```kotlin
public open class JavaValidationPlugin : ValidationPlugin(
    renderers = listOf(
        JavaValidationRenderer(customGenerators = customOptions.map { it.generator }),
        SetOnceRenderer()
    ),
    views = customOptions.flatMap { it.view }.toSet(),
    reactions = customOptions.flatMap { it.reactions }.toSet(),
)
```

Custom options contribute three kinds of artefacts: views and reactions for `:context`
(covered in “[The validation model](validation-model.md)”) and a `generator` for `:java`.
The plugin is the single point where all three are registered.

## The render lifecycle

`JavaValidationRenderer` runs once per `SourceFileSet`. The Spine Compiler invokes it
with the Java sources that `protoc` has already produced. The renderer iterates over
every message in the set, asks every generator what it has to contribute for that
message, and emits a single [`MessageValidationCode`][message-validation-code] bundle
per message:

```kotlin
override fun render(sources: SourceFileSet) {
    // We receive `grpc` and `kotlin` output sources roots here as well.
    // As for now, we modify only `java` sources.
    if (!sources.hasJavaRoot) {
        return
    }

    findMessageTypes()
        .forEach { message ->
            val code = generateCode(message)
            val file = sources.javaFileOf(message)
            file.render(code)
        }
}
```

Three properties of this loop are worth highlighting:

- The renderer visits **every** message, not just messages with declared constraints.
  A message with no options still becomes a `ValidatableMessage` whose generated
  `validate()` consults `ValidatorRegistry` (see
  “[Runtime library](runtime-library.md)”).
- The list of generators is fixed during one renderer run. Built-ins come from
  `builtInGenerators()`; custom generators arrive through the constructor. They are
  composed once and `inject(querying, typeSystem)` is called on each before the first
  `codeFor()` invocation.
- All collaboration with the model happens through `Querying`. The renderer never
  reads `.proto` files; it only reads the projections that `:context` populated.

## The `OptionGenerator` SPI

`OptionGenerator` is the abstraction that decouples the renderer from the specifics of
any single option. Every built-in option handled by `JavaValidationRenderer`, and every
custom Java validation option, is implemented as a subclass:

<embed-code
  file="$java/src/main/kotlin/io/spine/tools/validation/java/generate/OptionGenerator.kt"
  start="public abstract class OptionGenerator"
  end="^\}">
</embed-code>
```kotlin
public abstract class OptionGenerator {

    protected lateinit var querying: Querying
    protected lateinit var typeSystem: TypeSystem

    public abstract fun codeFor(type: TypeName): List<SingleOptionCode>

    public fun inject(querying: Querying, typeSystem: TypeSystem) {
        check(!::querying.isInitialized) {
            "`inject()` must be called exactly once on `${this::class.simpleName}`."
        }
        this.querying = querying
        this.typeSystem = typeSystem
    }
}
```

A typical generator queries its own projection, filters by the message currently being
processed, and emits one `SingleOptionCode` per option application. `RequiredGenerator`
is representative:

<embed-code
  file="$java/src/main/kotlin/io/spine/tools/validation/java/generate/option/RequiredGenerator.kt"
  start="internal class RequiredGenerator"
  end="^\}">
</embed-code>
```kotlin
internal class RequiredGenerator : OptionGeneratorWithConverter() {

    private val allRequiredFields by lazy {
        querying.select<RequiredField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allRequiredFields
            .filter { it.id.type == type }
            .map { GenerateRequired(it, converter).code() }
}
```

One convenience subclass of `OptionGenerator` exists today:

- [`OptionGeneratorWithConverter`][option-generator-with-converter] — adds a lazily
  constructed `JavaValueConverter` for translating Protobuf default values into Java
  literals. Generators that need to compare a field against its type-specific default
  (`(required)`, `(distinct)`, the bound options) extend this class.

Generators may also keep their own per-run state, as long as nothing that depends on
`Querying` or `TypeSystem` is touched before `inject()` returns.

## What the generator produces

A generator returns a list of [`SingleOptionCode`][single-option-code] objects, one per
option application in the message. The shape is intentionally minimal:

<embed-code
  file="$java/src/main/kotlin/io/spine/tools/validation/java/generate/SingleOptionCode.kt"
  start="public class SingleOptionCode"
  end="^\)">
</embed-code>
```kotlin
public class SingleOptionCode(
    public val constraint: CodeBlock,
    public val fields: List<FieldDeclaration<*>> = emptyList(),
    public val methods: List<MethodDeclaration> = emptyList(),
)
```

- `constraint` is the body that goes into the generated `validate()` method. It runs in
  a known scope that exposes a few well-defined variables (see “[The validate scope](#the-validate-scope)”
  below).
- `fields` and `methods` are class-level declarations. They are how an option can carry
  precomputed state — for example, `PatternGenerator` declares one
  `private static final java.util.regex.Pattern` field per `(pattern)` application so the
  pattern is compiled once and reused across calls.

The generated `constraint` block is plain Java text built from typed expressions. The
following snippet from `RequiredGenerator` is typical:

```kotlin
val constraint = CodeBlock(
    """
    if (${field.hasDefaultValue()}) {
        var fieldPath = ${parentPath.resolve(field.name)};
        var typeName =  ${parentName.orElse(declaringType)};
        var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"))};
        $violations.add(violation);
    }
    """.trimIndent()
)
```

There is no template engine. Generators interpolate Kotlin values for class names, field
references, and helper expressions into a Java code string. The expression types under
[`expression/`][expression-pkg] (`FieldPaths`, `TemplateStrings`, `ConstraintViolations`,
`ClassNames`, `Strings`, `UnsetValue`, …) are the building blocks; they expose typed
methods like `parentPath.resolve(field.name)` so the interpolation reads as code rather
than string concatenation.

### The validate scope

Every constraint block is injected into the same enclosing method, so the generated
code can rely on a fixed set of in-scope variables. They are declared in
[`ValidateScope`][validate-scope]:

| Variable      | Java type                              | Role                                                                            |
|---------------|----------------------------------------|---------------------------------------------------------------------------------|
| `violations`  | `ArrayList<ConstraintViolation>`       | Accumulator. A constraint adds a violation by `violations.add(violation)`.      |
| `parentPath`  | `io.spine.base.FieldPath`              | Path from the validation root to the current message. Empty for top-level use.  |
| `parentName`  | `io.spine.type.TypeName?` (nullable)   | Name of the type that triggered validation. Non-null only for nested messages.  |

The companion [`MessageScope`][message-scope] exposes an implicit `this` reference for
generators that need to read the message's own fields. Together, the two scopes are the
only state a constraint block can assume; everything else must be derived from view
state at generation time or carried by class-level fields the generator declares.

## Injecting the code into the PSI

After `JavaValidationRenderer` has assembled a `MessageValidationCode` for a message, it
hands the bundle to [`ValidationCodeInjector`][validation-code-injector]. The injector
operates on the [IntelliJ PSI][intellij-psi] representation of the already-generated Java
file. In the
main validation renderer, it is the component that mutates the message and builder PSI:

```kotlin
fun inject(code: MessageValidationCode, messageClass: PsiClass) {
    val builderClass = messageClass.nested("Builder")
    execute {
        messageClass.apply {
            implementValidatableMessage()
            declareValidateMethod(code.constraints)
            declareSupportingFields(code.fields)
            declareSupportingMethods(code.methods)
        }
        builderClass.apply {
            implementValidatingBuilder(messageClass)
            injectValidationIntoBuildMethod()
            annotateBuildReturnType()
            annotateBuildPartialReturnType()
        }
    }
}
```

The injector encodes the conventions for the shape of every generated validator:

- The message class is made to implement `ValidatableMessage`, gaining a
  `validate(parentPath, parentName)` method whose body concatenates every constraint
  block produced by the generators and finishes with a call into `ValidatorRegistry`
  (see “[Runtime library](runtime-library.md)”). The method returns
  `Optional<ValidationError>` rather than throwing, so a built message can be
  re-validated without paying for an exception.
- The builder is made to implement `ValidatingBuilder`. Its `build()` method is wrapped:
  the existing return is preceded by a call to `validate()`, and any violation is thrown
  as `ValidationException`. For constraints produced by `OptionGenerator`s, this is where
  validation errors become exceptions; `(set_once)` is handled separately and throws from
  builder mutators.
- The `build()` return type is annotated `@Validated` and `buildPartial()` is annotated
  `@NonValidated`. These markers are how downstream code (and IDE tooling) tell the two
  results apart at a glance.

Because the injector controls placement, generators are not allowed to write methods,
fields, or interface declarations directly into the file. They contribute snippets and
declarations through `SingleOptionCode`; the injector decides where each lands.

## The `(set_once)` renderer

`SetOnceRenderer` is a `JavaRenderer` in its own right, registered alongside
`JavaValidationRenderer` by `JavaValidationPlugin`. It exists because `(set_once)`
semantics modify builder behavior rather than add a check to `validate()`: the option
must reject any setter call that would change an already-assigned value, and that
rejection has to fire from inside the setter itself.

The renderer queries `SetOnceField` projections, dispatches to a field-type-specific
implementation of [`SetOnceJavaConstraints`][set-once-constraints]
(`SetOnceMessageField`, `SetOnceEnumField`, `SetOnceStringField`,
`SetOnceBooleanField`, `SetOnceBytesField`, `SetOnceNumberField`), and lets that
implementation modify every relevant setter, merge method, and the
`mergeFrom(CodedInputStream, …)` switch in the builder. The mechanics differ enough
between primitive, string, bytes, enum, and message fields that the per-type split is
worthwhile.

From the renderer's point of view, the result is a builder whose mutating entry points
all call `throwIfNotDefaultAndNotSame` before assigning. The `(set_once)` constraint
therefore never appears in the generated `validate()` method; it is a property of the
builder, not of the message. This is also why `(set_once)` does not participate in the
`OptionGenerator` SPI — a custom option that needed similar semantics would need its
own renderer and its own per-type logic, not a generator slot.

## Plugging in custom options

Custom options participate in code generation through the third member of the
`ValidationOption` SPI:

<embed-code
  file="$java/src/main/kotlin/io/spine/tools/validation/java/ValidationOption.kt"
  start="public interface ValidationOption"
  end="^\}">
</embed-code>
```kotlin
public interface ValidationOption {

    public val reactions: Set<Reaction<*>>
    public val view: Set<Class<out View<*, *, *>>>
    public val generator: OptionGenerator
}
```

`reactions` and `view` contribute model-side artefacts (see
“[The validation model](validation-model.md)”). `generator` is the Java-side
contribution. `JavaValidationPlugin` discovers `ValidationOption` implementations
through `ServiceLoader` and passes each `generator` to `JavaValidationRenderer`, which
appends them to the built-in list. Custom generators are therefore indistinguishable from
built-ins at run time: they receive the same `Querying` and `TypeSystem`, query their
own projections, and return `SingleOptionCode` instances that are merged into the same
`validate()` method with the rest.

The end-to-end walkthrough — declaring the option, modeling it in `:context`, writing a
generator, and wiring it through `META-INF/services` — lives in
“[Adding a new built-in validation option](adding-a-built-in-option.md)”. The
consumer-facing variant of the same SPI is covered by
“[Custom validation](../user/05-custom-validation/)” in the User's Guide.

## What's next

- [Runtime library](runtime-library.md) — the types in `:jvm-runtime` that the
  generated code calls into at execution time.
- [Extension points](extension-points.md) — the public extension surface built around
  `ValidationOption` and `MessageValidator`.
- [Adding a new built-in validation option](adding-a-built-in-option.md) — the
  contributor walkthrough that ties this page to the rest of the guide.

[java-validation-renderer]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/JavaValidationRenderer.kt
[set-once-renderer]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/setonce/SetOnceRenderer.kt
[option-generator]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/OptionGenerator.kt
[option-generator-with-converter]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/OptionGeneratorWithConverter.kt
[single-option-code]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/SingleOptionCode.kt
[message-validation-code]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/MessageValidationCode.kt
[validation-code-injector]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/ValidationCodeInjector.kt
[validate-scope]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/ValidateScope.kt
[message-scope]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/MessageScope.kt
[set-once-constraints]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/setonce/SetOnceJavaConstraints.kt
[expression-pkg]: https://github.com/SpineEventEngine/validation/tree/master/java/src/main/kotlin/io/spine/tools/validation/java/expression
[intellij-psi]: https://plugins.jetbrains.com/docs/intellij/psi.html
