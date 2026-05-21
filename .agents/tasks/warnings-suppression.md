---
slug: warnings-suppression
branch: address-issues
owner: claude
status: in-progress
started: 2026-05-21
related-tasks:
  - docs-validation-gradle-plugin-page
---

## Goal

Let users silence Validation compiler warnings on a *per-kind* basis through the
`validation { ... }` Gradle extension. First (and currently only) kind covered:
the **unsigned-integer** warning emitted by `BoundedFieldGenerator` when `uint32`
or `uint64` fields carry `(range)`, `(min)`, or `(max)` options.

Success criteria:

1. A user adding
   ```kotlin
   spine {
       validation {
           warnings {
               unsignedFields = false
           }
       }
   }
   ```
   to their `build.gradle.kts` no longer sees the unsigned-integer warning in
   the build output, while validation code generation continues to work.
2. With no configuration, behavior is unchanged from today (warning still
   emitted — default `true`).
3. The mechanism is extensible: adding a second warning kind later requires
   only a new property and a new proto field — no architecture change.

## Context

- Three warnings exist today, but only `unsignedFields` is in scope now:
  - `java/src/main/kotlin/io/spine/tools/validation/java/generate/option/bound/BoundedFieldGenerator.kt:220-263`
  - (out of scope for this task) `(if_invalid)` deprecation — `context/.../ValidateOption.kt:105`
  - (out of scope for this task) `(is_required)` deprecation — `context/.../ChoiceOption.kt:110`
- `ValidationExtension` currently exposes only `enabled: Property<Boolean>`
  (`gradle-plugin/src/main/kotlin/io/spine/tools/validation/gradle/ValidationExtension.kt:42`).
  No channel exists between the Gradle extension and the Spine Compiler renderers
  that emit warnings — we must build one.
- Chosen transport: **Spine Compiler `Settings` proto** (the idiomatic mechanism;
  see `spine/compiler/settings.proto` → `Settings.consumer`, `Settings.file`,
  and the `LoadsSettings` interface used by Spine Compiler renderers).
- **Settings consumer:** the renderer
  `io.spine.tools.validation.java.JavaValidationRenderer`. Its canonical class
  name is the `Settings.consumer` value the Gradle plugin writes and the
  renderer reads. The renderer is the natural reader: it already owns the
  per-build lifecycle (`UnsignedIntegerWarnings.clear()` on each `render()`)
  and composes the generators that emit the warning.

## Design

### 1. User-facing DSL

Nested DSL on `ValidationExtension`, positive polarity (each property is "is this
warning enabled", default `true`):

```kotlin
spine {
    validation {
        warnings {
            unsignedFields = false   // suppress
        }
    }
}
```

Implementation:

- New `WarningsExtension` class with `unsignedFields: Property<Boolean>` (default
  `true`).
- `ValidationExtension` gets a nested `warnings: WarningsExtension` plus a
  `warnings(Action<WarningsExtension>)` method so the block syntax works.

### 2. Settings proto

New proto under the shared validation proto tree, alongside the other
validation `.proto` files:

- **Location:** `context/src/main/proto/spine/validation/warnings_settings.proto`
  (same directory as `events.proto`, `views.proto`, `field_group.proto`, etc.).
- **Schema:**
  ```proto
  syntax = "proto3";
  package spine.validation;
  option java_package = "io.spine.validation.settings";
  option java_outer_classname = "WarningsSettingsProto";
  option java_multiple_files = true;

  // Per-kind toggles for warnings emitted by the Validation Compiler.
  // Each field defaults to `true` (warning enabled) when the proto is absent
  // or the field is unset; setting `false` suppresses the warning.
  message ValidationWarnings {
      // Emit the "unsigned integer types are not supported in Java" warning
      // for `uint32`/`uint64` fields with `(range)`, `(min)`, or `(max)`.
      bool unsigned_fields = 1;  // default true
  }
  ```

### 3. Write side (Gradle plugin)

In `ValidationGradlePlugin.configureValidation()`:

- After collecting values from the new `warnings` block, build a
  `ValidationWarnings` message.
- Register it via the Spine Compiler `Settings` API for the consumer
  `JavaValidationRenderer::class.java.canonicalName`
  (= `io.spine.tools.validation.java.JavaValidationRenderer`).
- Always write the settings (every flag is explicit on the read side; see §6).

### 4. Read side (Spine Compiler renderer)

- `JavaValidationRenderer` implements `LoadsSettings<ValidationWarnings>` (or
  the equivalent contract) and resolves the settings instance lazily in
  `render(sources)`, alongside the existing
  `UnsignedIntegerWarnings.clear()` call. If the settings file is absent
  (legacy / non-Gradle invocation), it falls back to a default instance with
  `unsigned_fields = true`.
- The resolved `ValidationWarnings` instance is exposed to the option
  generators via the same wiring already used to inject `typeSystem` /
  `querying` (see `OptionGenerator.inject(...)` at
  `JavaValidationRenderer.kt:69`). Concretely: extend the inject step so the
  bound generators (`RangeGenerator`, `MinGenerator`, `MaxGenerator`) receive
  the settings.
- In `BoundedFieldGenerator.checkWithinBounds()` (line ~122), gate the call to
  `unsignedIntegerWarning(view.file, field.span, boundPrimitive)` on
  `warnings.unsignedFields` (default `true` when settings absent).

### 5. Refactor: extract `UnsignedIntegerWarnings` to its own file

Currently `UnsignedIntegerWarnings` lives as a top-level `internal object` at
the bottom of `BoundedFieldGenerator.kt` (lines 265–291). The in-flight changes
on this branch already import it from `JavaValidationRenderer` (a different
package), so it has graduated from a private helper of `BoundedFieldGenerator`
into part of the module's internal API.

By Kotlin / Spine convention, a top-level declaration used outside its defining
file should live in its own file. Move it to:

`java/src/main/kotlin/io/spine/tools/validation/java/generate/option/bound/UnsignedIntegerWarnings.kt`

Scope of the move:

- Relocate the `internal object UnsignedIntegerWarnings` and its KDoc verbatim.
- Leave the `private fun unsignedIntegerWarning(...)` helper in
  `BoundedFieldGenerator.kt` (it is genuinely private to that file).
- Update imports in `BoundedFieldGenerator.kt` and `JavaValidationRenderer.kt`
  (and anywhere else that references it) — no public API change.
- The existing `UnsignedIntegerWarningsSpec` test continues to cover the moved
  object; no test relocation strictly required, but the test's package may be
  aligned with the new file location if guidelines call for it.

Done as part of this task because the same gating logic in §4 (read side)
will route through `UnsignedIntegerWarnings`, so the file is touched anyway.

### 6. Polarity convention

- **Gradle DSL:** positive — `warnings { unsignedFields = false }` means
  "do not emit the warning". User-facing language matches `-Xlint:-foo` style.
- **Proto:** also positive (`bool unsigned_fields = 1`, `true` means "emit").
  No inversion at the proto boundary.
- **Resolving the proto3 default-value ambiguity:** the Gradle plugin
  **always writes** the `ValidationWarnings` settings, populating each field
  from the corresponding `Property<Boolean>` (whose convention is `true`).
  Therefore the renderer never has to distinguish "field absent" from
  "field set to `false`": the file is always present and every flag is
  always explicit.
- The renderer reads `ValidationWarnings.getUnsignedFields()` directly and
  treats `false` as "suppress". If the settings file is somehow missing
  (e.g., legacy build, manual invocation outside the Gradle plugin), the
  read side defaults to `true` (preserves today's behavior).

## Plan

- [ ] Confirm open questions with reviewer (see below).
- [ ] **Proto:** add `context/src/main/proto/spine/validation/warnings_settings.proto`
      next to the other validation protos; no new sourceset needed.
- [ ] **Extension:** add `WarningsExtension` and nested `warnings` block on
      `ValidationExtension`; default `unsignedFields.convention(true)`.
- [ ] **Gradle plugin:** in `ValidationGradlePlugin.configureValidation()`,
      compose `ValidationWarnings` from the extension and always register it
      with the Compiler settings registry (so every flag is explicit on the
      read side).
- [ ] **Read side:** make `JavaValidationRenderer` load `ValidationWarnings`
      via `LoadsSettings` (consumer ID =
      `io.spine.tools.validation.java.JavaValidationRenderer`); thread the
      resolved settings down to the bound generators (`RangeGenerator`,
      `MinGenerator`, `MaxGenerator`) so `BoundedFieldGenerator` can gate
      `unsignedIntegerWarning()` on `warnings.unsignedFields`.
- [ ] **Refactor:** extract `UnsignedIntegerWarnings` from
      `BoundedFieldGenerator.kt` into its own file
      `UnsignedIntegerWarnings.kt` in the same package; update imports in
      `BoundedFieldGenerator.kt`, `JavaValidationRenderer.kt`, and any other
      callers. Leave the `private fun unsignedIntegerWarning(...)` helper
      where it is.
- [ ] **Tests:**
  - Unit test on `BoundedFieldGenerator`: with `unsigned_fields=false`,
    `unsignedIntegerWarning()` is not invoked (use `UnsignedIntegerWarningsSpec`
    as the existing reference).
  - Integration test in `gradle-plugin/`: a Gradle build that sets
    `warnings { unsignedFields = false }` produces no warning line in
    the compiler output for a proto fixture with `uint32 + (range)`.
  - Regression test: default behavior unchanged when no `warnings` block is
    present.
- [ ] **Docs:** update KDoc on `ValidationExtension` (and the new
      `WarningsExtension`) to show the `warnings { unsignedFields }` block.
      User-facing site documentation is owned by the sibling task
      [`docs-validation-gradle-plugin-page`](docs-validation-gradle-plugin-page.md);
      that plan covers the new
      `06-gradle-plugin/_index.md` page (placed after *Custom validation* in
      the navigation) and includes a section for `warnings { unsignedFields }`.
- [ ] **Version bump:** required for the PR per `pre-pr` skill.
- [ ] **Verification:** run the build/check command per
      `.agents/running-builds.md`.

## Open questions (please confirm during plan review)

_All open questions resolved. See sibling task
[`docs-validation-gradle-plugin-page`](docs-validation-gradle-plugin-page.md)
for documentation work._

## Log

- 2026-05-21 — drafted, awaiting review
- 2026-05-21 — proto location resolved: `context/src/main/proto/spine/validation/`
  alongside the existing validation protos.
- 2026-05-21 — proto polarity resolved: keep `bool unsigned_fields` positive;
  Gradle plugin always writes the settings so every flag is explicit on the
  read side. No inversion in the proto layer.
- 2026-05-21 — added refactor step: extract `UnsignedIntegerWarnings` (now
  imported by `JavaValidationRenderer`) into its own
  `UnsignedIntegerWarnings.kt`; Spine convention says top-level declarations
  used outside their defining file get their own file.
- 2026-05-21 — settings consumer ID resolved:
  `io.spine.tools.validation.java.JavaValidationRenderer`. Renderer (not the
  plugin) reads `ValidationWarnings` and injects it into the bound generators.
- 2026-05-21 — documentation question resolved: site docs are spun out to
  sibling task `docs-validation-gradle-plugin-page` (new `06-gradle-plugin/`
  section after *Custom validation*). This task now owns only KDoc updates.
