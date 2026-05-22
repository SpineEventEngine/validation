---
slug: reject-required-on-empty
branch: improve-console-output
owner: claude
status: in-progress
started: 2026-05-22
related-issues:
  - https://github.com/SpineEventEngine/validation/issues/146
---

## Goal

Make the Validation Compiler reject `(required)` set on any field whose type is
`google.protobuf.Empty` — singular, `repeated`, or `map<_, Empty>` — at
**compile time**, producing a clear error pointing at the offending field.
Today, such fields produce a runtime "always invalid" message because the
generated check compares against the default instance of `Empty`, which always
matches.

Scope is limited to the **explicit** `(required)` option in this repo. The
parallel rule for *implicitly* required IDs (command messages, entity states)
lives in `SpineEventEngine/core-jvm-compiler` and will be filed as a
follow-up issue linked from #146.

## Context

Issue: https://github.com/SpineEventEngine/validation/issues/146

Today's behavior (runtime-only) is captured by `RequiredMessageITest.not be
applied to the type 'Empty'` and the `AlwaysInvalid` fixture at
`tests/validating/src/testFixtures/proto/spine/test/tools/validate/required.proto:63`.
The test comment explicitly references this issue as "we should do more and
raise compile-time error".

Decisions made with the user before drafting (one question at a time):
- (Q1) Implicit-required case (command IDs / entity-state IDs): only this
  repo; open a follow-up issue under `core-jvm-compiler` and cross-link it
  with #146.
- (Q2) Existing `AlwaysInvalid` runtime fixture: keep the proto definition,
  but move it to a "should-fail-compilation" fixture set in `context-tests`
  (analogous to `RequiredBoolField`).
- (Q3) Structural placement: inline a new private helper next to
  `checkFieldType()` in `RequiredOption.kt`; call it from the existing
  `RequiredReaction.whenever()`.
- (Q4) Only reject `Empty` with `(required)` set. Pointless `repeated Empty`
  / `map<_, Empty>` *without* `(required)` are out of scope — not the job
  of Validation.

### Key code locations

- `context/src/main/kotlin/io/spine/tools/validation/option/required/RequiredOption.kt`
  — `RequiredReaction.whenever()` (line ~91) and the private
  `checkFieldType()` helper (line ~149) live here. The new helper goes next
  to `checkFieldType()` and is invoked from `whenever()`.
- `context/src/main/kotlin/io/spine/tools/validation/option/required/RequiredFieldSupport.kt`
  — type-support rules; not touched by this change.
- `context-tests/src/testFixtures/proto/spine/validation/required_option_spec.proto`
  — where the new "bad" fixtures land (three new messages).
- `context-tests/src/test/kotlin/io/spine/tools/validation/RequiredOptionSpec.kt`
  — where the new compile-time tests land.
- `tests/validating/src/testFixtures/proto/spine/test/tools/validate/required.proto:63`
  — `AlwaysInvalid` to be removed (replaced by the context-tests fixtures).
- `tests/validating/src/test/kotlin/io/spine/test/options/required/RequiredMessageITest.kt:60-77`
  — the `not be applied to the type 'Empty'` test method to be removed
  (along with its `AlwaysInvalid` and `Empty` imports).

### API used to detect `Empty`

Following the existing `refersToAny()` pattern in
`compiler-api/.../FieldTypeExts.kt`:
- `TypeNameOrBuilder.isAny` checks `packageName == "google.protobuf" &&
  simpleName == "Any"`.
- `Type.isAny = isMessage && message.isAny`.
- `FieldType.refersToAny()` handles singular / list / map.

We mirror that locally in `RequiredOption.kt` with a `refersToEmpty()`
private helper. No change to `compiler-api` is needed for this issue.

## Plan

- [ ] **Add compile-time check** — in `RequiredOption.kt`:
  - Add private helper `checkFieldIsNotEmpty(field, file)` next to
    `checkFieldType()`, using `Compilation.check(...)` with a message like:
    `"The field `${field.qualifiedName}` of type `${field.type.name}` cannot
    be marked as `($REQUIRED)` because `google.protobuf.Empty` has no
    fields and its instances are always equal to the default value."`
  - Add a private `FieldType.refersToEmpty()` helper that checks singular
    message / `list.message` / `map.valueType.message` for
    `packageName == "google.protobuf" && simpleName == "Empty"`.
  - Call `checkFieldIsNotEmpty(field, file)` from
    `RequiredReaction.whenever()` immediately after `checkFieldType(...)`.
- [ ] **Move the runtime fixture to a compile-time fixture set**:
  - Delete `AlwaysInvalid` from
    `tests/validating/src/testFixtures/proto/spine/test/tools/validate/required.proto`.
  - Add three new fixture messages to
    `context-tests/src/testFixtures/proto/spine/validation/required_option_spec.proto`:
    `RequiredEmptyField` (singular), `RequiredRepeatedEmpty` (repeated),
    `RequiredMapWithEmptyValue` (`map<string, Empty>`).
- [ ] **Add compile-time tests** in `RequiredOptionSpec.kt`:
  - Three tests asserting `assertCompilationFails(...)` on each new
    fixture with a substring matching the new error message.
  - Reuse the existing `unsupportedFieldType(field)` only if the message
    is exactly the same wording; otherwise add a sibling helper
    `rejectsEmpty(field)` in the same file.
- [ ] **Remove the runtime test**:
  - Delete the `not be applied to the type 'Empty'` test method in
    `RequiredMessageITest.kt`.
  - Drop the now-unused `import com.google.protobuf.Empty` and
    `import io.spine.test.tools.validate.AlwaysInvalid`.
- [ ] **Search for any other live references** to `AlwaysInvalid` in
      `tests/validating` proto/code (the `spine.test.validate.AlwaysInvalid`
      in `messages.proto` is a DIFFERENT message — string fields — and
      must remain untouched).
- [ ] **Pre-PR**:
  - Run `bump-version` per repo policy.
  - Run the build per `.agents/running-builds.md`.
  - `kotlin-review` on the diff.
  - `review-docs` on KDoc changes (any new public docs?).
- [ ] **Follow-up issue** under `core-jvm-compiler`:
  - File a new issue: "Reject `(required)`-implicit ID fields of type
    `google.protobuf.Empty` at compile-time".
  - Link it back to #146 in both directions (mention from each issue).
  - Body: short rationale, link to this change in `validation`, and the
    affected reactions (`RequiredIdReaction`, `EntityStateIdReaction`).

## Log

- 2026-05-22 — drafted from user clarifications Q1–Q4 above; awaiting
  approval via `ExitPlanMode`.
