# Add `ValidatingBuilder.validate()` probe method

**Status:** implementation complete; awaiting build verification and user review.

## Goal

Add a non-throwing validation probe to `io.spine.validation.ValidatingBuilder`
per ADR 0001 (core-jvm, D9 amendment of 2026-07-05):

```java
default List<ConstraintViolation> validate()
```

Requested by @armiol in
[core-jvm PR #1642](https://github.com/SpineEventEngine/core-jvm/pull/1642#issuecomment-4885985767)
as a complement to the `tryAlter {}` entity extension (de-event-sourcing
Phase A).

## Semantics

- `buildPartial()` the current content; if the result is a
  `ValidatableMessage`, unwrap `validate()` → `ValidationError` →
  constraint violations; otherwise return an empty list.
- Empty list == valid. Never throws on invalid content. Never mutates
  the builder.
- Contrast: `build()` throws `ValidationException`; `buildPartial()` skips
  validation entirely.

## Changes

- `jvm-runtime/src/main/java/io/spine/validation/ValidatingBuilder.java` —
  the default method + Javadoc; interface Javadoc mentions the probe.
- `jvm-runtime/src/test/kotlin/io/spine/validation/ValidatingBuilderSpec.kt` —
  stub-based unit tests (valid, invalid-no-throw, no-mutation,
  non-`ValidatableMessage` fallback).
- `tests/validating/src/test/kotlin/io/spine/test/JavaMessageSmokeTest.kt` —
  integration tests against generated `CardNumber` code.
- `version.gradle.kts` — `2.0.0-SNAPSHOT.446` → `.447` (bump-version skill;
  commit step withheld — no commit authorization from the user).

## Verification

- `./gradlew build` — full build with tests.
- `./gradlew dokkaGenerate` — Javadoc/Dokka for the doc change.
- `kotlin-review` and `review-docs` agents on the branch diff.

Delete this file on merge to master.
