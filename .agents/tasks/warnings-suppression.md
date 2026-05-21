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
           java {
               warnings {
                   unsignedFields = false
               }
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
- **Settings home:** the new top-level module `:java-settings` hosts the proto
  schema (`ValidationWarnings`). Both `:java` (renderer side) and
  `:gradle-plugin` (writer side) depend on it. Proto package
  `spine.validation.java`; Java package `io.spine.tools.validation.settings`.

## Design

### 1. User-facing DSL

Nested DSL on `ValidationExtension`, positive polarity (each property is "is this
warning enabled", default `true`). The Java-target settings live under a
dedicated `java { ... }` block so that other targets (e.g. Kotlin, JS) can be
added later without breaking the namespace:

```kotlin
spine {
    validation {
        java {
            warnings {
                unsignedFields = false   // suppress
            }
        }
    }
}
```

Implementation:

- The nested `java { ... }` block lives **inside `ValidationExtension`** —
  not as a sibling top-level class. Concretely: a nested
  `abstract class Java @Inject constructor(project: Project)` declared inside
  `ValidationExtension`, with a `warnings: Warnings` property and a
  `warnings(Action<Warnings>)` method for the block syntax.
- The `Warnings` class is nested inside `Java` (also abstract, `@Inject`-
  constructed), with `unsignedFields: Property<Boolean>` (default `true`).
- `ValidationExtension` exposes a `java: Java` property (created via the host
  project's `ObjectFactory`) plus a `java(Action<Java>)` method so the block
  syntax works.

### 2. Settings proto

Lives in the new top-level `:java-settings` module — a proto-only module
that hosts settings consumed by `JavaValidationRenderer`. Both `:java` and
`:gradle-plugin` depend on this module.

- **Location:** `java-settings/src/main/proto/spine/validation/java/validation_warnings.proto`.
  File name mirrors the top-level type name `ValidationWarnings`; directory
  layout matches the proto package (`spine/validation/java`).
- **Schema:**
  ```proto
  syntax = "proto3";
  package spine.validation.java;
  option java_package = "io.spine.tools.validation.settings";
  option java_outer_classname = "ValidationWarningsProto";
  option java_multiple_files = true;

  // Per-kind toggles for warnings emitted by the Java target of the
  // Validation Compiler. Each field defaults to `true` (warning enabled)
  // when the proto is absent or the field is unset; setting `false`
  // suppresses the warning.
  message ValidationWarnings {
      // Emit the "unsigned integer types are not supported in Java" warning
      // for `uint32`/`uint64` fields with `(range)`, `(min)`, or `(max)`.
      bool unsigned_fields = 1;  // default true
  }
  ```

### 3. Write side (Gradle plugin)

In `ValidationGradlePlugin.configureValidation()`:

- The plugin depends on `:java-settings`, so it builds a strongly-typed
  `ValidationWarnings` message — no JSON-by-hand.
- After collecting values from the new `validation.java.warnings { ... }`
  block, the plugin writes the message in `Format.ProtoBinary` to the Spine
  Compiler settings directory for the consumer
  `io.spine.tools.validation.java.JavaValidationRenderer`.
- The consumer ID is kept as a string literal — the plugin must not depend
  on `:java` (renderer module), only on `:java-settings`.
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

- [x] **New module:** add `:java-settings` (top-level, proto-only) with its
      own `build.gradle.kts` (mimics `:jvm-runtime` minus the runtime
      dependencies) and a `src/main/proto/spine/validation/java/` source
      tree. Registered in root `settings.gradle.kts`.
- [x] **Proto move:** `git mv` the `validation_warnings.proto` from
      `:context` to `:java-settings`; switch proto package to
      `spine.validation.java`; keep Java package
      `io.spine.tools.validation.settings`.
- [x] **Dependencies:** `:java` and `:gradle-plugin` declare
      `project(":java-settings")` so both compile against the typed
      `ValidationWarnings` message.
- [x] **Extension:** restructure `ValidationExtension` DSL so the warnings
      block is nested under a `java { ... }` block:
      `validation.java.warnings.unsignedFields`. Both `Java` and `Warnings`
      are nested `abstract @Inject` classes inside `ValidationExtension`,
      per the team-memory rule on nested Gradle DSLs.
- [x] **Gradle plugin:** in `ValidationGradlePlugin.configureValidation()`,
      build a typed `ValidationWarnings` message from
      `validation.java.warnings` and write it in `Format.ProtoBinary` to the
      Spine Compiler settings directory (the renderer never has to
      distinguish "field absent" from "field set to `false`").
- [ ] **Read side (already in place):** `JavaValidationRenderer` loads
      `ValidationWarnings` via `LoadsSettings` and calls
      `UnsignedIntegerWarnings.setEnabled(...)` on every render. Verify it
      still compiles after the proto move (Java package unchanged).
- [x] **Refactor:** extract `UnsignedIntegerWarnings` from
      `BoundedFieldGenerator.kt` into its own file
      `UnsignedIntegerWarnings.kt` in the same package. *(Done in commit
      `817b6b5823` on this branch.)*
- [ ] **Tests:**
  - Unit test on `BoundedFieldGenerator`: with `unsigned_fields=false`,
    `unsignedIntegerWarning()` is not invoked (use `UnsignedIntegerWarningsSpec`
    as the existing reference).
  - Integration test in `gradle-plugin/`: a Gradle build that sets
    `java { warnings { unsignedFields = false } }` produces no warning line
    in the compiler output for a proto fixture with `uint32 + (range)`.
  - Regression test: default behavior unchanged when no `java` block is
    present.
- [x] **Docs:** update KDoc on `ValidationExtension` and its nested
      `Java`/`Warnings` classes plus the user-facing
      `06-gradle-plugin/_index.md` to reflect the new
      `validation.java.warnings.unsignedFields` path.
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
- 2026-05-21 — corrected: no sibling `WarningsExtension`. The nested
  `warnings { ... }` block lives inside `ValidationExtension` (private
  nested `abstract class Warnings`), per the team-memory rule on nested
  Gradle DSLs.
- 2026-05-21 — renamed proto file `warnings_settings.proto` →
  `validation_warnings.proto` to match the top-level type name
  `ValidationWarnings`. `java_outer_classname` updated to
  `ValidationWarningsProto` accordingly.
- 2026-05-21 — introduced top-level `:java-settings` module as the home of
  Java-renderer settings. `git mv`d `validation_warnings.proto` from
  `:context` to `:java-settings`; switched proto package to
  `spine.validation.java`. `:java` and `:gradle-plugin` both depend on
  `:java-settings`. Plugin now writes the typed `ValidationWarnings`
  message in `Format.ProtoBinary` instead of a hand-built JSON literal.
- 2026-05-21 — DSL restructured: warnings block now lives under a nested
  `java { ... }` block — `validation.java.warnings.unsignedFields` — so
  future non-Java targets can have their own sub-blocks without crowding
  the top-level `validation { ... }` namespace.
