---
title: Testing strategy
description: Map of the test modules in Spine Validation and guidance on choosing the right one.
headline: Documentation
---

# Testing strategy

The repository has a deliberately wide test layout: ten modules dedicated to tests
(or to fixtures consumed by tests), plus per-module unit tests where they pay off.
This is not over-engineering — each module isolates one concern that the others
cannot exercise without polluting their own classpath. The compile-time/runtime
split described in “[Architecture](architecture.md)” is reflected in how the test
modules are organised, and so is the built-in/custom split documented in
“[Extension points](extension-points.md)”.

This page maps the modules to those concerns and tells you which one to extend
when you add a test. The full inventory of modules — including non-test modules —
lives in “[Key modules](key-modules.md)”; this page focuses on *what to do* with
the test modules rather than restating their one-line descriptions.

## The shape of the test layout

Tests in this repository fall into four bands. Picking the right module is mostly
a question of which band a new test belongs to.

| Band | Concern | Modules |
|------|---------|---------|
| Compile-time diagnostics | The Compiler must reject invalid uses of a built-in option with a clear message. | `:context-tests` |
| Runtime library | `:jvm-runtime` types behave correctly without involving the Compiler at all. | `:jvm-runtime` (in-module tests) |
| End-to-end on built-ins | A `.proto` with built-in options compiles and the generated `validate()` produces the expected report. | `:tests:vanilla`, `:tests:validating`, `:tests:runtime` |
| Custom extensions | The `ValidationOption` and `MessageValidator` SPIs work end-to-end against a realistic consumer setup. | `:tests:extensions`, `:tests:consumer`, `:tests:consumer-dependency`, `:tests:validator`, `:tests:validator-dependency` |

The two extension-test pairs (`:tests:consumer` + `:tests:consumer-dependency`,
`:tests:validator` + `:tests:validator-dependency`) each consist of a *consumer*
project that runs the assertions and a *dependency* project that contributes
`.proto` types or service implementations the consumer pulls in. The dependency
half is not a test suite; it exists so that the consumer half can exercise the
realistic case where `.proto` types or validators come from a different module.

## Compile-time diagnostics: `:context-tests`

`:context-tests` is the home for tests that assert the Compiler *fails* on a
malformed use of a built-in option, with a specific diagnostic. The module is
built around [Prototap][prototap], a Spine test harness that runs `protoc` plus
the Spine Compiler against intentionally-broken `.proto` fixtures and exposes the
captured diagnostics back to the test code.

The shape of a typical spec — see
[`RangeReactionSpec.kt`][range-reaction-spec] for a full example:

- A spec class extends [`CompilationErrorTest`][compilation-error-test], which
  invokes `tapConsole` from `Logging.testLib` and the Prototap entry points.
- The spec asserts both that compilation failed and that the diagnostic carries
  the expected message — for example, that pointing `(range)` at an unsupported
  field type names the field and the rejected type.
- The matching `.proto` fixture lives under
  `context-tests/src/testFixtures/proto/spine/validation/`. The naming
  convention pairs the spec with its fixture: `range_bad_field_type.proto`,
  `range_bad_overflow.proto`, and so on.

Add a spec here for every diagnostic a reaction can raise. The reaction-side
conventions in “[The validation model](validation-model.md#error-reporting-conventions)” list
the diagnostic categories that built-ins are expected to cover: unsupported field
type, unsupported placeholder, companion-without-primary, malformed range,
overflow, and so forth.

`:context-tests` is *not* the place for tests that compile successfully and
inspect generated code or runtime behaviour. Those belong in `:tests:validating`
or `:tests:runtime`.

## Runtime library: `:jvm-runtime` in-module tests

`:jvm-runtime` ships with its own `src/test/` source set covering the runtime
types in isolation: `ValidatorRegistry`, `ExceptionFactory`,
`ValidationException`, `TemplateString` rendering, `TimestampValidator`, and the
violation diagnostics under `io.spine.validation.diags`. No Compiler or codegen
is involved — these are unit tests on the public runtime surface described in
“[Runtime library](runtime-library.md)”.

Add a test here when you change a type that is part of the runtime API and the
behaviour can be exercised by constructing a `Message`, a `ConstraintViolation`,
or a `ValidationException` directly. If your test needs a generated `validate()`
method to fire, you are in the wrong module — go to `:tests:runtime` or
`:tests:validating` instead.

`:context`, `:java`, and `:gradle-plugin` do not have unit-test source sets:
their behaviour is a function of the model and the renderer, both of which are
covered by the integration modules below. A unit test that needed to instantiate
a renderer or a reaction in isolation would have to rebuild most of the Spine
Compiler harness around it; the integration modules already do that.

## End-to-end on built-ins

Three modules cover the case that matters most for built-in options: a `.proto`
file is compiled by the Spine Compiler, generated Java is produced, and the test
exercises the generated `validate()`. They differ in scope and in what they make
easy.

### `:tests:validating` — the primary integration suite

`:tests:validating` is the module to reach for first. It uses
`java-test-fixtures` to keep `.proto` fixtures and Kotlin helpers in one place,
shared across many specs.

What sits where:

- **`testFixtures/proto/spine/test/tools/validate/`** — the shared `.proto`
  surface: `required.proto`, `numbers.proto`, `goes_*`, `set_once_*`,
  `external_constraint.proto`, plus rejection and command types used by
  cross-cutting specs. New built-ins typically add one fixture file here that
  exercises every supported field type.
- **`testFixtures/kotlin/`** — assertion helpers, placeholder builders, and
  test environments used across specs.
- **`src/test/kotlin/io/spine/test/options/`** — the specs themselves, organised
  by option (`required/`, `goes/`, `setonce/`) or by cross-cutting concern
  (`ChoiceITest.kt`, `ExternalConstraintITest.kt`,
  `CustomOptionsLoadingITest.kt`).

The framework stack is JUnit 5 plus Kotest assertions plus Truth for the
classes that already used it. Specs typically build a message, catch
`ValidationException`, and assert on the shape of the resulting `ValidationError`
— including placeholder resolution and the `field_path` of each violation.

This is the module used by the “[Adding a new built-in validation option](adding-a-built-in-option.md#6-test-the-option)”
walkthrough, and it should be your default for any test that asks “does this
option do what its consumer-facing documentation says it does?”

### `:tests:runtime` — runtime behaviour and constraint matrices

`:tests:runtime` covers runtime *behaviour* that is independent of any specific
option — `ValidationOfConstraintTest`, `OneofSpec`, `EnclosedMessageValidationSpec`,
`AnyValidationSpec`, `EntityIdSpec`, `MessageExtensionsSpec`, `FieldAwareMessageSpec`
— alongside per-option constraint matrices under
`src/test/kotlin/io/spine/validation/option/`. The build applies
`CoreJvmCompiler.pluginId` (the McJava Compiler) so that test messages benefit
from the Spine Java extras (entity columns, message extensions, field-aware
generated code) the runtime types are designed to interoperate with.

Use this module when:

- The behaviour you need to exercise depends on Spine's Java extras and not just
  on validation, or
- You are adding cross-cutting runtime behaviour (`Validate.check` semantics,
  enclosed-message dispatch, oneof handling) rather than the behaviour of one
  specific option.

`:tests:runtime` does not use `testFixtures` — `.proto` fixtures live alongside
the specs.

### `:tests:vanilla` — baseline integration without extensions

`:tests:vanilla` is the smallest end-to-end suite: a stock build with no custom
extensions, exercising a handful of constraints (`JavaValidationSpec`,
`IsRequiredSpec`, `GoesConstraintSpec`, `DistinctConstraintSpec`). Its purpose
is to catch breakage that would otherwise be masked by the richer setup in
`:tests:validating` or by the McJava plugin in `:tests:runtime`.

Add a test here only when your change interacts with the Java codegen pipeline
in a way that the more focused suites would not catch — typically a baseline
smoke test for a new built-in, or a regression test for a defect that involved
the plain Spine Compiler classpath.

## Custom extensions

Two pairs of modules cover the extension SPIs end-to-end. Both use the
*consumer + dependency* split: the consumer module hosts the assertions, and the
dependency module supplies the `.proto` types or service registrations that the
consumer needs to pull in from somewhere other than its own source tree.

### `:tests:extensions` and `:tests:consumer*` — `ValidationOption`

`:tests:extensions` is a tiny Kotlin module (no `src/test`) that implements the
running `(currency)` example referenced throughout the documentation. It
contributes a reaction, a view, a generator, and a `ValidationOption`
implementation registered through `@AutoService`. Its purpose is to be a
realistic, third-party-shaped `ValidationOption` that the consumer modules can
depend on.

`:tests:consumer` consumes that extension: it applies the Compiler, lists
`:tests:extensions` on the Compiler user classpath, and asserts that the
custom option is discovered, runs in the model, and produces the expected
generated code and runtime violations. `:tests:consumer-dependency` provides
`.proto` types that `:tests:consumer` imports, so the test setup matches the
realistic case where a consumer's protos live across several Gradle modules.

Add to these modules when you change something that affects how a custom
`ValidationOption` is discovered, instantiated, or wired into the plugin — see
“[Extension points: discovery](extension-points.md#discovery)”. Do *not* add
generic option-behaviour tests here; if a built-in change happens to break custom
options, the diagnostic will show up in the `:tests:consumer*` suite, but the
authoritative test for the built-in still lives in `:tests:validating`.

### `:tests:validator` and `:tests:validator-dependency` — `MessageValidator`

The validator pair mirrors the consumer pair, for the runtime SPI. The
dependency module declares `.proto` types and `MessageValidator` implementations
discovered through `ServiceLoader`; the consumer module asserts that the
registry picks them up, that they run alongside compiled checks for local
messages, and that they are the only entry point for external messages — the
contract documented in
“[Extension points: ordering and composition](extension-points.md#ordering-and-composition)”.

Add to these modules when you change `ValidatorRegistry`, `MessageValidator`
discovery, the `DetectedViolation` → `ConstraintViolation` translation, or the
generated wiring that calls into the registry from local messages.

## Choosing the right module

When in doubt, work down this list:

1. Is the test asserting that the **Compiler should fail** on a malformed `.proto`? → **`:context-tests`**.
2. Is the test exercising a runtime type **without involving the Compiler**? → **`:jvm-runtime` in-module tests**.
3. Is the test exercising a **built-in option's behaviour** through generated code? → **`:tests:validating`**.
4. Is the test about **runtime semantics that span options** — `validate()` mechanics, oneof dispatch, enclosed messages, McJava interop? → **`:tests:runtime`**.
5. Is the test about a **custom `ValidationOption`** being discovered and applied? → **`:tests:consumer`** (with fixtures from `:tests:extensions` and `:tests:consumer-dependency`).
6. Is the test about a **custom `MessageValidator`** being discovered and applied? → **`:tests:validator`** (with fixtures from `:tests:validator-dependency`).
7. Is the change a **broad pipeline regression** that does not fit any of the above? → **`:tests:vanilla`**.

If two modules look plausible, prefer the one with narrower scope. A test that
ends up in `:tests:vanilla` because it wanted to be near the Java-compilation
pipeline but really tests `(required)` behaviour will rot — the vanilla suite
intentionally stays small, and the test is more discoverable next to its
peers in `:tests:validating`.

## Conventions

A handful of conventions keep specs across the suites consistent:

- **Kotest matchers**, JUnit 5 lifecycle. Specs use the `*Spec` or `*ITest`
  naming convention; pick the suffix the surrounding directory already uses.
- **Parameterised tests** with `@MethodSource` for per-field-type matrices. The
  built-in option specs in `:tests:validating` and `:tests:runtime` use this
  pattern liberally; copy it rather than handwriting per-type test methods.
- **Fixtures are shared, specs are not.** Move `.proto` files into
  `testFixtures` when more than one spec needs them; keep specs in
  `src/test/kotlin/`. Inverting this — sharing specs across modules — is not a
  pattern this repository uses, and it tends to mask which module owns the
  behaviour.
- **Inline comments are welcome in tests.** The general rule against inline
  comments in production code does not apply to specs — see
  `.agents/documentation-guidelines.md`. Use them to explain non-obvious setup
  in fixtures or to flag the violation shape a spec is asserting.
- **One concern per spec class.** A spec named after an option covers that
  option; cross-cutting behaviour goes in its own spec. The
  `ValidationOfConstraintTest`/`OneofSpec` split in `:tests:runtime` is the
  reference shape.

## What's next

- [Key modules](key-modules.md) — one-line descriptions of every module in the
  repository, including the ones not shown above.
- [Adding a new built-in validation option](adding-a-built-in-option.md#6-test-the-option)
  — the contributor walkthrough that shows how the test modules above are
  combined for a concrete change.
- [Extension points](extension-points.md) — the SPIs that the
  `:tests:consumer*` and `:tests:validator*` modules exercise.
- [Build, packaging, and release](build-and-release.md) — how the test modules
  fit into the multi-project build.

[prototap]: https://github.com/SpineEventEngine/ProtoTap
[range-reaction-spec]: https://github.com/SpineEventEngine/validation/blob/master/context-tests/src/test/kotlin/io/spine/tools/validation/RangeReactionSpec.kt
[compilation-error-test]: https://github.com/SpineEventEngine/validation/blob/master/context-tests/src/test/kotlin/io/spine/tools/validation/CompilationErrorTest.kt
