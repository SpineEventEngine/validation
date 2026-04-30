# Task: Extend "Custom validation" documentation

## Goal

Extend `docs/content/docs/validation/05-custom-validation/_index.md` with step-by-step
guidance on implementing a custom validation option and its code generator. The existing
content provides a correct high-level overview; the task is to add concrete detail and
working code examples derived from the `(when)` option implementation in
`docs/_time/validation/`.

**Target audience**: a developer integrating a new custom option into an existing
Spine-based build plugin.

**Acceptance criteria**: after reading the extended section, a developer can implement all
required artefacts (proto, Kotlin classes, `@AutoService` registrations) for a new
field-level option without consulting the source code.

---

## Source references

All code examples should be verified against and derived from these files:

| Artefact | Path |
|---|---|
| Proto option definition | `docs/_time/time/src/main/proto/spine/time_options.proto` |
| Event proto message | `docs/_time/validation/src/main/proto/spine/tools/time/validation/events.proto` |
| View state proto message | `docs/_time/validation/src/main/proto/spine/tools/time/validation/views.proto` |
| `ValidationOption` + `Reaction` + `View` | `docs/_time/validation/src/main/kotlin/io/spine/tools/time/validation/java/WhenOption.kt` |
| `OptionGenerator` | `docs/_time/validation/src/main/kotlin/io/spine/tools/time/validation/java/WhenGenerator.kt` |
| `OptionsProvider` registration | `tests/extensions/src/main/kotlin/io/spine/tools/validation/test/CustomOptionsProvider.kt` |
| `ValidationOption` SPI entry-point | `tests/extensions/src/main/kotlin/io/spine/tools/validation/test/CurrencyOption.kt` |

---

## Gaps in current documentation

The current `_index.md` is missing:

1. **Proto artefacts** — no explanation of the three distinct proto definitions required:
   - the option message itself (e.g. `TimeOption` + `Time` enum, extension field),
   - the domain event emitted by the reaction (e.g. `WhenFieldDiscovered`),
   - the view state accumulator (e.g. `WhenField` with `(entity).kind = PROJECTION`).
2. **`OptionsProvider` registration** — mentioned in step 2 but no code shown.
3. **`ValidationOption` entry-point** — no code showing the `@AutoService` annotation,
   the `NAME` constant convention, or how reactions/view/generator are wired together.
4. **Reaction** — does not explain the `@Where` filter, the `EitherOf2<Event, NoReaction>`
   return type, the "disabled option" short-circuit (e.g. `TIME_UNDEFINED`), or the
   `checkPlaceholders` / `Compilation.check` error-reporting API.
5. **View** — does not show the proto backing message, the `FieldRef` identity pattern, or
   the `alter { }` DSL.
6. **Generator** — does not explain `OptionGeneratorWithConverter`, lazy view querying, or
   `SingleOptionCode`.

---

## File structure

Mirror the convention used in `docs/content/docs/validation/04-validators/`:
the `_index.md` provides the overview and "What's next" navigation; each detailed
topic lives in its own page.

```
docs/content/docs/validation/05-custom-validation/
    _index.md                     ← existing file, update only
    declare-the-option.md         ← new
    register-the-option.md        ← new
    implement-the-reaction.md     ← new
    implement-the-view.md         ← new
    implement-the-generator.md    ← new
```

---

## Changes to `_index.md`

- Keep the existing high-level 4-step overview and the workflow diagram unchanged.
- Replace the detailed prose sub-sections ("Reaction", "View", "Generator") with a
  short one-liner each that links to the corresponding new page.
- Update the "What's next" block to include links to all five new pages.

---

## Page specifications

### `declare-the-option.md`

**Front-matter title**: `Declare the option in Protobuf`

Three sub-sections, each with a prose introduction and a code snippet.

#### Declare the option message

- Explain that the option is a Protobuf `extend` block targeting one of the standard
  descriptor option types (`FieldOptions`, `MessageOptions`, etc.).
- Show a trimmed version of `time_options.proto`:
  - `extend google.protobuf.FieldOptions { TimeOption when = 73819; }`
  - the `TimeOption` message with `Time in = 1` and `string error_msg = 3`
  - the `Time` enum (`TIME_UNDEFINED`, `PAST`, `FUTURE`)
  - the `(default_message)` annotation on `TimeOption`
- Note: omit `package` from the proto file if shorter option usage in `.proto` files is
  desired (cite the comment in `time_options.proto` that explains this trade-off).

#### Declare the event

- Explain that the Reaction emits a domain event that carries all data the View and
  Generator need.
- Show a trimmed version of `events.proto`:
  - `message WhenFieldDiscovered` with `compiler.FieldRef id = 1`, `compiler.Field subject`,
    `string error_message`, `Time bound`, `TimeFieldType type`.
- Note: `id` must be the first field and must be the same type that the View uses as its
  identity (`FieldRef` in this case).

#### Declare the view state

- Explain that the view state is the persistent accumulator queried by the Generator.
- Show a trimmed version of `views.proto`:
  - `message WhenField` with `option (entity).kind = PROJECTION` and the same fields as
    the event.
- Explain that the `id` field must match the event `id` type so the framework routes the
  event to the correct view instance.

---

### `register-the-option.md`

**Front-matter title**: `Register the option`

Two sub-sections.

#### Register the proto extension

- Show the `@AutoService(OptionsProvider::class)` pattern from `CustomOptionsProvider.kt`
  (call `registerAllExtensions` on the generated outer class of the proto file containing
  the `extend` block).
- Explain that this makes the option visible to the Protobuf descriptor machinery at
  runtime.

#### Wire up ValidationOption

- Show the full `WhenOption` class (public API only, omit private helpers):
  - `@AutoService(ValidationOption::class)` annotation
  - `companion object { const val NAME = "when" }` — explain this string is the value
    matched by `@Where` in the Reaction.
  - `override val reactions`, `override val view`, `override val generator`
- Note: one `ValidationOption` per custom option; only one generator is allowed per
  `ValidationOption`.

---

### `implement-the-reaction.md`

**Front-matter title**: `Implement the Reaction`

- Show the class declaration: `internal class WhenReaction : Reaction<FieldOptionDiscovered>()`.
- Show the reaction method signature with its annotations:
  ```kotlin
  @React
  override fun whenever(
      @External @Where(field = OPTION_NAME, equals = WhenOption.NAME)
      event: FieldOptionDiscovered
  ): EitherOf2<WhenFieldDiscovered, NoReaction>
  ```
- Explain each annotation:
  - `@External` — the event originates from the compiler's bounded context.
  - `@Where` — narrows the subscription to events for this option only; `OPTION_NAME` is
    a constant from `io.spine.tools.validation`.
- Describe the three possible outcomes:
  1. **Unsupported field type** — call `Compilation.check(...)` to emit a compile error
     and stop processing.
  2. **Disabled option** — short-circuit with `return ignore()` (emits `NoReaction`) when
     the option value is the sentinel `TIME_UNDEFINED`.
  3. **Valid, enabled option** — validate the error message template via
     `checkPlaceholders(SUPPORTED_PLACEHOLDERS, field, file, WhenOption.NAME)`, then build
     and return the domain event using the Kotlin DSL
     (`whenFieldDiscovered { ... }.asA()`).
- Include a concise end-to-end code snippet of the `WhenReaction.whenever()` body.

---

### `implement-the-view.md`

**Front-matter title**: `Implement the View`

- Show the class declaration:
  `internal class WhenFieldView : View<FieldRef, WhenField, WhenField.Builder>()`
- Explain the three type parameters: ID type, state proto message, state builder.
- Show the subscriber method:
  ```kotlin
  @Subscribe
  fun on(e: WhenFieldDiscovered) = alter {
      subject = e.subject
      errorMessage = e.errorMessage
      bound = e.bound
      type = e.type
  }
  ```
- Note: views only accumulate data; validation and business logic belong in the Reaction.

---

### `implement-the-generator.md`

**Front-matter title**: `Implement the Generator`

- Show the class declaration:
  `internal class WhenGenerator : OptionGeneratorWithConverter()`
- Explain when to use `OptionGeneratorWithConverter` vs the plain `OptionGenerator` base
  (the former injects a `JavaValueConverter` for converting proto field values to Java
  code expressions).
- Show the lazy view query:
  ```kotlin
  private val allWhenFields by lazy {
      querying.select<WhenField>().all()
  }
  ```
  Explain that `querying` is injected by the framework and must not be accessed before
  the generator is invoked.
- Show the `codeFor` override:
  ```kotlin
  override fun codeFor(type: TypeName): List<SingleOptionCode> =
      allWhenFields
          .filter { it.id.type == type }
          .map { GenerateWhen(it, converter).code() }
  ```
  Explain that the framework calls `codeFor` once per processed message type and that
  each returned `SingleOptionCode` wraps a `CodeBlock` inlined into the generated
  `validate()` method.
- Recommend the inner-class pattern (`GenerateWhen`) to separate "gather view data"
  (generator) from "compose code string" (inner class), but note that internal helpers
  can be omitted from documentation for brevity.

---

## Style and formatting rules

- Each new page must include a YAML front-matter block with `title`, `description`, and
  `headline: Documentation` (follow existing pages in `04-validators/`).
- Use `##` for top-level sections and `###` for sub-sections within a page.
- Use fenced code blocks with language tags (`kotlin`, `protobuf`).
- Keep prose paragraphs short (≤4 lines); use bullet lists for multi-item enumerations.
- Avoid widows, runts, orphans, and rivers (see `.agents/documentation-guidelines.md`).
- All class/method/field names must match the actual source identifiers exactly.
- End each page with a "What's next" section linking forward to the next page and back
  to the section index (`../`).

---

## Out of scope

- End-to-end code for a hypothetical new option.
- Documenting the `currency` option separately (it is already referenced in `_index.md`).
- Changes to any file outside `docs/content/docs/validation/05-custom-validation/`.
