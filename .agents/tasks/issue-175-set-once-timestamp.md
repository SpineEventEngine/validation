---
slug: issue-175-set-once-timestamp
branch: air/please-work-on-this-issue-https-github.com-spineeventengine-vali-8bd73481-e
owner: claude
status: in-progress
started: 2026-05-22
---

## Goal

Close [issue #175](https://github.com/SpineEventEngine/validation/issues/175) by adding
a positive integration test that applies `(set_once)` to a field of type
`google.protobuf.Timestamp` — an external message imported from a Protobuf dependency.

## Context

The issue has two items:

1. Negative tests: fail when the option is applied to unsupported field types.
2. Positive test for `(set_once)` on an external message type like `Timestamp`,
   guarding the regression that motivated the original report.

Item 1 is already covered at the reaction level:

- `context-tests/.../SetOnceOptionSpec.kt` exercises both unsupported categories
  (`repeated`, `map`) for `(set_once)`, which is exhaustive because the supported-type
  check is purely structural (`!isList && !isMap`).
- `context-tests/.../GoesReactionSpec.kt` + `GoesReactionTestEnv.kt` enumerate **all 13**
  unsupported primitive types (`bool` + 12 numeric variants) as both target and companion,
  matching `SUPPORTED_PRIMITIVES = {STRING, BYTES}`.

Item 2 has no coverage today — the `(set_once)` proto fixtures only use the locally
defined `Name` message. `(goes)` already uses `Timestamp` in several of its fixtures, so
the pattern is well-established.

## Plan

- [x] Draft this task file and the system plan.
- [x] Add `import "google/protobuf/timestamp.proto";` and a new `Measurement`
      message with one `(set_once) = true` `Timestamp` field to
      `tests/validating/src/testFixtures/proto/spine/test/tools/validate/set_once_fields.proto`.
- [x] Add two distinct `Timestamp` constants (`BIRTHDAY1`, `BIRTHDAY2`) to
      `tests/validating/src/testFixtures/kotlin/io/spine/test/options/setonce/SetOnceTestEnv.kt`.
- [x] Add two nested test classes to
      `tests/validating/src/test/kotlin/io/spine/test/options/setonce/SetOnceFieldsITest.kt`
      mirroring the existing `…non-default message` / `…default and same-value message`
      pair, but for `Measurement`.
- [x] `./gradlew :tests:validating:test --tests "*SetOnceFieldsITest*"` passes (82/82).
- [x] `./gradlew :tests:validating:test` passes (460/460, no regressions).
- [x] Bump `version.gradle.kts` via the `bump-version` skill (SNAPSHOT.441 → .442).
- [ ] Run the `pre-pr` skill (version gate, build/check, reviewers).
- [ ] Delete this file on merge to master.

## Log

- 2026-05-22 — drafted, approved by user (turn 4), executing.
- 2026-05-22 — implementation + tests complete; 460/460 pass. Proceeding to version bump.
- 2026-05-22 — version bumped to `2.0.0-SNAPSHOT.442`; dependency reports regenerated and committed.
