---
title: Adding a new built-in validation option
description: How contributors add a new standard validation option to the Spine Validation library.
headline: Documentation
---

# Adding a new built-in validation option

This page is the contributor-side counterpart to the User's Guide
“[Custom validation](../05-custom-validation/)” section. Where that section explains how a
*consumer* wires a custom option into their own project, this page explains how a
*contributor* adds a new **standard** option to the Validation library — one that ships in
[`spine/options.proto`][options-proto] and is recognised by every consumer of the library
without further configuration.

The mechanics are similar but the locations differ:

| Aspect             | Custom option                                 | Built-in option                                                         |
|--------------------|-----------------------------------------------|-------------------------------------------------------------------------|
| Option declaration | A `.proto` file in the consumer's repository. | [`spine/options.proto`][options-proto] in the Base Libraries repo.      |
| Reaction and view  | Modules in the consumer's repository.         | [`:context`][context-pkg] in this repository.                           |
| Generator          | Module in the consumer's repository.          | [`:java`][java-pkg] in this repository.                                 |
| Discovery          | `ValidationOption` SPI via `ServiceLoader`.   | Direct registration in `ValidationPlugin` and `JavaValidationRenderer`. |
| Distribution       | Consumer project, optionally a Gradle plugin. | Ships with the Validation library and `:java-bundle`.                   |

The walkthrough below uses `(required)` as the recurring concrete reference. It is the
built-in whose model and codegen are most thoroughly described elsewhere in the guide
(see “[The validation model](validation-model.md)” and
“[Java code generation](java-code-generation.md)”), so each step links to the section that
explains that step in depth.

## Before you start

Adding a built-in option is a coordinated change across two repositories — Validation and
[Base Libraries][base-libraries] — and across at least three modules in this repository. Before the
implementation, decide:

- **Whether the option is general enough to belong in the base library.** Built-in
  options are part of the Spine vocabulary every consumer inherits. An option that is
  meaningful only inside one domain belongs in that domain's library as a custom option,
  exactly the way Spine Time ships `(when)` (see
  “[Custom validation](../05-custom-validation/)”).
  The “[Extension points](extension-points.md#the-two-surfaces-at-a-glance)” page
  describes when to choose `ValidationOption` over `MessageValidator`; the same discipline
  applies to built-ins.
- **The option's declaration site.** A field-level option becomes a `FieldOptions`
  extension; an option on a `oneof` group becomes a `OneofOptions` extension; a
  message-level option becomes a `MessageOptions` extension. The reaction's input event
  and the projection's identity follow from this choice — see
  “[The validation model](validation-model.md#the-bounded-context-shape)”.
- **Whether the option is primary or companion.** A companion option overrides one aspect
  of a primary — `(if_missing)` overrides `(required)`'s error message, `(if_invalid)`
  overrides `(validate)`'s. A&nbsp;companion has its own reaction and event but contributes to
  the *primary's* projection. See
  “[Companion options](validation-model.md#companion-options)”.
- **Whether the option needs runtime helpers.** Most options compile down to inline Java
  that uses only types already in `:jvm-runtime`. A few — for example, `(pattern)` —
  introduce new placeholders or share a runtime helper class. Plan this up front; it
  affects which modules you change.

## 1. Declare the option in Base Libraries

The Protobuf extension that defines the option's name, target descriptor type, and
extension number lives in [`spine/options.proto`][options-proto] in the
[Base Libraries][base-libraries] repository, not in this repository. Every built-in extension number is
allocated from the same range as the options that already ship there, and the file is the
single point at which `protoc` learns about the option.

A field-level option declaration follows the same shape as the existing built-ins —
illustrated below with an `EXT_NUMBER` placeholder for the field number that the
Base Libraries maintainers allocate:

```protobuf
extend google.protobuf.FieldOptions {
    // A boolean option that requires a field to be set.
    bool required = EXT_NUMBER [(default_message) = "The field must be set."];
}
```

Three points worth highlighting:

- The **extension number** must be unique within `FieldOptions`/`MessageOptions`/
  `OneofOptions`. Allocate it in coordination with the maintainers of Base Libraries
  rather than picking a number locally.
- The **`(default_message)` annotation** is the fallback error template. It is read by
  [`defaultErrorMessage`][default-message] in `:context` and recorded on the discovery
  event, so the projection picks it up only when no companion has overridden it. See
  “[Error message templates and placeholders](validation-model.md#error-message-templates-and-placeholders)”.
- For options that carry structured data (rather than a bare `bool` or `string`), declare
  a separate Protobuf message type in `options.proto` and use it as the extension's type —
  the way `IfMissingOption`, `PatternOption`, and `RequireOption` are declared.

The change to `options.proto` ships in the next Base Libraries release. Until that
release is available, the matching changes in this repository will not compile against
the published artefact: coordinate the version bump with the Base Libraries maintainers
and merge the two changes in lock-step.

## 2. Add the option name constant

`:context` matches incoming `FieldOptionDiscovered` / `OneofOptionDiscovered` /
`MessageOptionDiscovered` events by the option's textual name. The constants live in
[`OptionNames.kt`][option-names]:

```kotlin
/**
 * The name of the `(required)` option.
 */
public const val REQUIRED: String = "required"
```

Add a new constant for every option you introduce, primary or companion. The reaction
uses it in its `@Where(field = OPTION_NAME, equals = …)` filter, and so does the
generator if it needs to refer to the option name in error messages or compilation
diagnostics.

## 3. Model the option in `:context`

This step is the heart of the work. The first four substeps mirror the artefacts a
Bounded Context combines (see
“[The Bounded Context shape](validation-model.md#the-bounded-context-shape)”); the fifth
wires those artefacts into the plugin:

### 3.1. Declare the discovery event

Add a `*Discovered` message to
[`context/src/main/proto/spine/validation/events.proto`][events-proto]. The event carries
the data the projection will record:

<embed-code
  file="$context/src/main/proto/spine/validation/events.proto"
  start="message RequiredFieldDiscovered \{"
  end="^\}">
</embed-code>
```protobuf
message RequiredFieldDiscovered {

    compiler.FieldRef id = 1;

    // The field in which the option was discovered.
    compiler.Field subject = 2;

    // The default error message template.
    string default_error_message = 3;
}
```

The `id` field must be the first declared field and must match the projection's identity
type — `compiler.FieldRef` for field-level options, the corresponding declaration type
for `oneof` and message options. Companion events typically carry only the override they
contribute (for example, `IfMissingOptionDiscovered` carries just the custom message);
see “[The discovered event](validation-model.md#the-discovered-event)”.

### 3.2. Declare the projection

Add a view to [`context/src/main/proto/spine/validation/views.proto`][views-proto]. The
shape mirrors the event, with `(entity).kind = PROJECTION` to mark it as a Bounded Context
projection:

<embed-code
  file="$context/src/main/proto/spine/validation/views.proto"
  start="message RequiredField \{"
  end="^\}">
</embed-code>
```protobuf
message RequiredField {
    option (entity).kind = PROJECTION;

    compiler.FieldRef id = 1;

    // The field in which the option was discovered.
    compiler.Field subject = 2;

    // The error message template.
    string error_message = 3;
}
```

A primary option owns its projection. A companion folds into the *primary's*
projection: `IfMissingOption` does not declare its own view, it only contributes to
`RequiredField`. See “[The projection](validation-model.md#the-projection)”.

### 3.3. Implement the reaction

The reaction subscribes to the upstream `*OptionDiscovered` event, filters by the option
name constant, validates applicability, and emits the `*Discovered` domain event:

<embed-code
  file="$context/src/main/kotlin/io/spine/tools/validation/option/required/RequiredOption.kt"
  start="internal class RequiredReaction"
  end="^\}">
</embed-code>
```kotlin
internal class RequiredReaction : Reaction<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = REQUIRED)
        event: FieldOptionDiscovered,
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        if (!event.option.boolValue) {
            return ignore()
        }

        val defaultMessage = defaultErrorMessage<IfMissingOption>()
        return requiredFieldDiscovered {
            id = field.ref
            subject = field
            defaultErrorMessage = defaultMessage
        }.asA()
    }
}
```

The reaction is the only place where applicability is checked. By the time the
discovery event is emitted, the option has been confirmed valid for this declaration site.

A few conventions all built-in reactions follow:

- **Use `EitherOf2<…, NoReaction>` when the option can be disabled at the declaration
  site** (`(required) = false` is a correctly applied but disabled option, so the
  reaction returns `NoReaction` and no projection is created). Use `Just<…>` when every
  application of the option must produce a discovery event.
- **Report misapplication through `Compilation.check` / `Compilation.error`**, never
  through exceptions. The lambda is evaluated only on failure, so detailed diagnostics
  are cheap. See
  “[Error reporting conventions](validation-model.md#error-reporting-conventions)”.
- **For companion options, call `checkPrimaryApplied` first.** It fails compilation if
  the companion is used without the primary it modifies (see
  “[Companion options](validation-model.md#companion-options)”).
- **Validate the error template's placeholders against a fixed set.** This applies to
  reactions whose option carries a custom message template — typically a companion such
  as `(if_missing)` — not to a primary like `(required)` whose template is fixed by
  `(default_message)`. Such a reaction declares a `SUPPORTED_PLACEHOLDERS` set and calls
  `checkPlaceholders` on the supplied message; this is what lets the generator assume
  every placeholder it later reads is known. See `IfMissingReaction` for the running
  reference.

### 3.4. Implement the projection

The projection is a Kotlin `View` parameterised by its identity, state, and builder
types. It subscribes to the discovery event and folds it into state:

<embed-code
  file="$context/src/main/kotlin/io/spine/tools/validation/option/required/RequiredOption.kt"
  start="internal class RequiredFieldView"
  end="^\}">
</embed-code>
```kotlin
internal class RequiredFieldView : View<FieldRef, RequiredField, RequiredField.Builder>() {

    @Subscribe
    fun on(e: RequiredFieldDiscovered) {
        val currentMessage = state().errorMessage
        val message = currentMessage.ifEmpty { e.defaultErrorMessage }
        alter {
            subject = e.subject
            errorMessage = message
        }
    }

    @Subscribe
    fun on(e: IfMissingOptionDiscovered) = alter {
        errorMessage = e.customErrorMessage
    }
}
```

A projection may subscribe to multiple events: `RequiredFieldView` folds both
`RequiredFieldDiscovered` (the primary) and `IfMissingOptionDiscovered` (the companion).
Either order works — the projection picks the default only if no custom message has been
recorded yet.

### 3.5. Register the reaction and view

[`ValidationPlugin`][validation-plugin] is the language-agnostic entry point that lists
every built-in. Add the new reaction and view to its `views` and `reactions` sets:

```kotlin
public abstract class ValidationPlugin(
    // ...
) : Plugin(
    // ...
    views = views + setOf(
        RequiredFieldView::class.java,
        // ... other existing views
        YourNewView::class.java,  // the view you are adding
    ),
    reactions = reactions + setOf<Reaction<*>>(
        RequiredReaction(),
        IfMissingReaction(),
        // ... other existing reactions
        YourNewReaction(),  // the reaction(s) you are adding
    )
)
```

For a primary plus its companion, both reactions go in the `reactions` set; only the
primary's view goes in `views`.

## 4. Implement code generation

The Java side reads from the populated projection and emits inline Java. Typically this
is one generator class (often supported by a small helper that builds the per-application
`CodeBlock`) plus one line of registration; see [4.3](#43-builder-mutating-options) for
the exception that needs a separate renderer instead.

### 4.1. Write the `OptionGenerator`

Place the generator under
[`java/src/main/kotlin/io/spine/tools/validation/java/generate/option/`][option-generator-pkg].
Built-ins extend `OptionGenerator` directly, or `OptionGeneratorWithConverter` when the
emitted code needs `JavaValueConverter` for default-value comparison:

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

The pattern is uniform across the built-ins:

- Query the projection lazily — `querying` is not available until `inject()` returns,
  see “[The render lifecycle](java-code-generation.md#the-render-lifecycle)”.
- Filter views by the message type currently being processed.
- Delegate per-application code construction to a small helper class
  (`GenerateRequired` here). The helper produces a `CodeBlock` that runs inside the
  validate scope described in “[The validate scope](java-code-generation.md#the-validate-scope)” —
  `violations`,
  `parentPath`, `parentName` are in scope and the helper appends a
  `ConstraintViolation` to `violations` when the constraint fails.

The constraint block follows the same shape every built-in uses: derive the field path
from `parentPath`, derive the type name from `parentName.orElse(declaringType)`, build a
`ConstraintViolation` through the `constraintViolation` expression helper, and add it to
`violations`. See “[What the generator produces](java-code-generation.md#what-the-generator-produces)”
for the full anatomy of a `SingleOptionCode`.

### 4.2. Register the generator

[`JavaValidationRenderer`][java-validation-renderer] keeps the list of built-in
generators in `builtInGenerators()`. Append the new generator there:

```kotlin
private fun builtInGenerators(): List<OptionGenerator> = listOf(
    RequiredGenerator(),
    PatternGenerator(),
    GoesGenerator(),
    DistinctGenerator(),
    ValidateGenerator(),
    RangeGenerator(),
    MaxGenerator(),
    MinGenerator(),
    ChoiceGenerator(),
    RequireOptionGenerator(),
    // ... add the new generator here
)
```

Because the option ships as a built-in, no `ValidationOption` SPI implementation is
involved: the generator is registered directly. The
“[Extension points](extension-points.md#the-validationoption-spi-end-to-end)” page
describes how a custom option reaches the same `JavaValidationRenderer` through
`ServiceLoader` instead.

### 4.3. Builder-mutating options

`(set_once)` is the one built-in whose semantics modify builder behaviour rather than
adding a check to `validate()`. It is rendered by a separate
[`SetOnceRenderer`][set-once-renderer] and does not implement `OptionGenerator` at all.
A new built-in with similar semantics — for example, an option that should reject a
setter call rather than report a violation at `build()` time — needs its own renderer
following the `SetOnceRenderer` pattern, not a generator slot. See
“[The `(set_once)` renderer](java-code-generation.md#the-set_once-renderer)”.

## 5. Add runtime support if needed

Most options compile to inline Java that uses only types already exported by
`:jvm-runtime`: `ConstraintViolation`, `TemplateString`, `FieldPath`, the
`Validate` entry points, and the placeholders enumerated in
[`RuntimeErrorPlaceholder`][runtime-error-placeholder]. No runtime change is required for
the average new option.

A new option needs runtime work in three cases:

- **It introduces a new error placeholder.** Both
  [`ErrorPlaceholder`][error-placeholder] in `:context` and
  [`RuntimeErrorPlaceholder`][runtime-error-placeholder] in `:jvm-runtime` enumerate the
  placeholder names; the two enums must stay in sync. The duplication is documented in
  KDoc on `ErrorPlaceholder` and is expected to disappear once the Compiler and the
  runtime share a base.
- **It needs a shared runtime helper.** If the option's generated code would otherwise
  inline a non-trivial routine into every `validate()` body, factor it out as a `static`
  helper in `:jvm-runtime` and call it from the generated code instead. This is rare;
  the generated `if (…) { violations.add(…) }` blocks are deliberately self-contained.
- **It changes the violation schema.** New fields on `ConstraintViolation` or
  `ValidationError` are wire-visible — these Protobuf types cross process boundaries,
  see “[Constraints on the runtime surface](runtime-library.md#constraints-on-the-runtime-surface)”.
  Coordinate any change to the schema with the maintainers and respect Protobuf
  field-number stability.

The runtime must not parse `.proto` descriptors to recover what the model already knew.
If the option needs more runtime support than a placeholder or a helper, recheck whether
the work belongs at build time instead.

## 6. Test the option

The repository ships several test modules, each with a different scope. New built-ins
typically touch three of them. The test modules are catalogued in
“[Key modules](key-modules.md#test-modules)”; choosing the right one is covered in
“[Testing strategy](testing-strategy.md)”.

- **`:context-tests`** — Prototap-based compilation tests for `:context`. Add a spec
  here for every diagnostic the reaction can raise (unsupported field type, unsupported
  placeholder, companion-without-primary, invalid syntax, …). Specs sit alongside
  existing ones such as
  [`IfMissingReactionSpec.kt`][if-missing-reaction-spec], with `.proto` fixtures under
  `context-tests/src/testFixtures/proto/spine/validation/`.
- **`:tests:validating`** — end-to-end behaviour for the option in generated code.
  Existing `(required)` integration tests live under
  [`tests/validating/src/test/kotlin/io/spine/test/options/required/`][required-itest-pkg];
  shared `.proto` fixtures live under
  `tests/validating/src/testFixtures/proto/spine/test/tools/validate/`. Add a fixture
  message that exercises every supported field type and a Kotest spec that builds the
  message, asserts the violation report shape, and verifies the placeholders resolve.
- **`:tests:vanilla`** — baseline integration without custom extensions. Add a smoke
  test here only if the option introduces an interaction with the broader Java codegen
  pipeline that the more focused `:tests:validating` cases would not catch.

`:tests:extensions` and `:tests:consumer*` exist for *custom* options and consumer-side
scenarios; a new built-in should not need additions there. `:tests:validator*` covers
`MessageValidator` discovery and is unrelated. `:tests:runtime` is the right home for
behaviour that is purely about runtime types — `Validate.check`, `ValidatorRegistry`,
exception formatting — independent of any specific option.

## 7. Document the option in the User guide

A built-in option is part of the public Validation vocabulary, so its consumer-facing
documentation lives in the User's Guide
“[Built-in options](../03-built-in-options/)” section, not in the Developer's Guide.
Pick the page that matches the option's declaration site and add an entry consistent with
the surrounding conventions:

- Field-level options — [`field-level-options.md`](../03-built-in-options/field-level-options.md).
- `oneof` options — [`oneof-fields.md`](../03-built-in-options/oneof-fields.md).
- Message-level options — [`message-level-options.md`](../03-built-in-options/message-level-options.md).

The option's primary entry goes on exactly one of the three pages above, keyed to the
declaration site. [`repeated-and-map-fields.md`](../03-built-in-options/repeated-and-map-fields.md)
is a cross-cutting reference, not a fourth declaration-site target: if your option has
notable behaviour on `repeated` or `map` fields (non-empty checks, per-element
validation, distinctness, …), additionally cross-reference the new entry from that page.

Each entry follows the same shape: a one-sentence purpose, the **Applies to** list of
supported field types, a **Minimal example** snippet, and a **Custom message** snippet
when the option supports `(if_…).error_msg`. Update the `_index.md` summary line if the
new option falls outside the existing categories.

If the option ships with an associated companion (`(if_missing)`-style), document them as
a pair on the same page; do not give a companion its own section.

## How this differs from a custom option

The contributor-facing flow above and the consumer-facing flow in
“[Custom validation](../05-custom-validation/)” share most of their substance: declare an
option, model it as a reaction plus a view, generate Java, register the pieces. The
differences are concentrated at the boundaries:

- **No `OptionsProvider`.** Built-in options live in `spine/options.proto` in the base
  library, which is registered with the global `ExtensionRegistry` by the base library
  itself. A custom option registers its own provider; a built-in does not.
- **No `ValidationOption` SPI implementation.** Built-ins are listed directly in
  `ValidationPlugin` and `JavaValidationRenderer.builtInGenerators()`. The 
  `ValidationOption` SPI is the discovery mechanism for custom options only.
- **No `META-INF/services` entry, no `@AutoService`.** The plugin loads built-ins
  through ordinary class references; `ServiceLoader` is involved only for custom
  contributions.
- **No separate Gradle-plugin step.** Built-ins ship in `:java-bundle` and are placed on
  the Compiler's classpath by `:gradle-plugin` together with the rest of the library
  (see “[Build, packaging, and release](build-and-release.md)”). A custom option's module
  must explicitly register itself with the Compiler — see
  “[Pass the option to the Compiler](../05-custom-validation/pass-to-compiler.md)”.
- **The option declaration crosses repositories.** The `.proto` change lives in
  [Base Libraries][base-libraries] and ships in that library's release; the model and codegen changes
  live here. The two changes must be coordinated.

The User's Guide
“[Custom validation](../05-custom-validation/)” section is still worth reading end-to-end
before contributing a built-in: it describes the same architectural ideas from the
consumer's perspective, and the running `(when)` example illustrates patterns — disabled
sentinel values, message-typed options, repeated and map handling — that built-ins use
too.

## What's next

- [The validation model](validation-model.md) — the full anatomy of events, projections,
  and reactions in `:context`.
- [Java code generation](java-code-generation.md) — `OptionGenerator`, the validate
  scope, and how `SingleOptionCode` is injected into generated classes.
- [Runtime library](runtime-library.md) — what is on the runtime classpath when a
  generated `validate()` runs, and where to put runtime helpers.
- [Extension points](extension-points.md) — the public extension surfaces and the
  constraints that govern them.
- [Testing strategy](testing-strategy.md) — choosing the right test module for each
  layer of a new option.

[options-proto]: https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto
[base-libraries]: https://github.com/SpineEventEngine/base-libraries
[context-pkg]: https://github.com/SpineEventEngine/validation/tree/master/context
[java-pkg]: https://github.com/SpineEventEngine/validation/tree/master/java
[option-names]: https://github.com/SpineEventEngine/validation/blob/master/context/src/main/kotlin/io/spine/tools/validation/option/OptionNames.kt
[events-proto]: https://github.com/SpineEventEngine/validation/blob/master/context/src/main/proto/spine/validation/events.proto
[views-proto]: https://github.com/SpineEventEngine/validation/blob/master/context/src/main/proto/spine/validation/views.proto
[validation-plugin]: https://github.com/SpineEventEngine/validation/blob/master/context/src/main/kotlin/io/spine/tools/validation/ValidationPlugin.kt
[default-message]: https://github.com/SpineEventEngine/validation/blob/master/context/src/main/kotlin/io/spine/tools/validation/DefaultErrorMessage.kt
[error-placeholder]: https://github.com/SpineEventEngine/validation/blob/master/context/src/main/kotlin/io/spine/tools/validation/ErrorPlaceholder.kt
[option-generator-pkg]: https://github.com/SpineEventEngine/validation/tree/master/java/src/main/kotlin/io/spine/tools/validation/java/generate/option
[java-validation-renderer]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/JavaValidationRenderer.kt
[set-once-renderer]: https://github.com/SpineEventEngine/validation/blob/master/java/src/main/kotlin/io/spine/tools/validation/java/setonce/SetOnceRenderer.kt
[runtime-error-placeholder]: https://github.com/SpineEventEngine/validation/blob/master/jvm-runtime/src/main/kotlin/io/spine/validation/RuntimeErrorPlaceholder.kt
[if-missing-reaction-spec]: https://github.com/SpineEventEngine/validation/blob/master/context-tests/src/test/kotlin/io/spine/tools/validation/IfMissingReactionSpec.kt
[required-itest-pkg]: https://github.com/SpineEventEngine/validation/tree/master/tests/validating/src/test/kotlin/io/spine/test/options/required
