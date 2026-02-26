# Spine Validation documentation plan (repo-local)

This repository contains the **staging** content for Spine Validation docs.
The goal of this plan is to turn the existing drafts + references into a coherent,
buildable documentation set, without expanding scope unnecessarily.

## Key locations (source of truth)

- Docs content (Hugo): `docs/content/docs/validation/`
- Protobuf options reference (for built-ins):
  - `docs/_options/options.proto`
- Example projects for embedded snippets: `docs/_examples/`
- Docs build notes: `docs/GRADLE.md`

## Definition of done (for a “first complete docs cut”)

- Navigation is consistent (no dead links between pages).
- Embedded code snippets are up-to-date (`checkSamples` passes).
- Preview site builds locally (`buildSite` succeeds).
- “Getting started” can be followed end-to-end without guessing.

## Minimal task list (in order)

1) Information architecture (IA): make Hugo navigation coherent
- Status: DONE (2026-02-23).
- Added/updated section landing pages:
  - `docs/content/docs/validation/_index.md`
  - `docs/content/docs/validation/09-developers-guide/_index.md`
- Replaced broken `.../index.md` links with directory links where appropriate:
  - `docs/content/docs/validation/00-intro/_index.md`
  - `docs/content/docs/validation/01-getting-started/_index.md`
- Added “What’s next” navigation to keep a clear reading path:
  - `docs/content/docs/validation/00-intro/target-audience.md`
  - `docs/content/docs/validation/00-intro/philosophy.md`
  - `docs/content/docs/validation/09-developers-guide/architecture.md`
  - `docs/content/docs/validation/09-developers-guide/key-modules.md`
  - `docs/content/docs/validation/01-getting-started/first-model.md`
- Fixed an obvious broken image reference:
  `docs/content/docs/validation/08-custom-validation/_index.md`

2) Complete “Getting started” flow
- Status: DONE (2026-02-24). 
- Validate that the "Getting started" section covers:
  importing options, build-time validation, `build()` vs `buildPartial()`, and `validate()`.

3) [Concepts](archive/concepts-plan.md)
- Status: DONE (2026-02-26).

4) Working with error messages
- Describe how `TemplateString` works (placeholders + values) and how to convert it to a
  human-readable message (formatting).
- Clarify the recommended ways to work with Validation errors in:
  - Kotlin: `TemplateString.format()` / `TemplateString.formatUnsafe()`.
  - Java: `io.spine.validation.TemplateStrings.format(TemplateString)`.
- Explain the structure of `ValidationError` / `ConstraintViolation`, and what fields developers
  should use when:
  - displaying messages to end users;
  - logging diagnostics (e.g. include `type_name`, `field_path`, and the unformatted template).
- Add troubleshooting notes for common runtime formatting problems (e.g. missing placeholder
  values; choosing `formatUnsafe()` when partial substitution is acceptable).
- Source references to anchor the docs:
  - `jvm-runtime/src/main/proto/spine/validation/error_message.proto`
  - `jvm-runtime/src/main/proto/spine/validation/validation_error.proto`
  - `jvm-runtime/src/main/kotlin/io/spine/validation/TemplateStringExts.kt`
  - `jvm-runtime/src/main/kotlin/io/spine/validation/RuntimeErrorPlaceholder.kt`

5) Built-in options: publish a minimal reference set
- From `docs/_options/options.proto`,
  enumerate the built-in options and group them (fields, strings, numbers, collections, message-level).
- For each documented option: purpose, supported field types, common pitfalls, and a short `.proto` example.
- Start with the options already used in docs/examples: `(required)`, `(pattern)`, `(min)/(max)`,
  `(distinct)`, `(validate)`.

6) Runtime API usage (Java + Kotlin)
- Document the two primary usage patterns:
  - fail-fast on `build()` (throws `ValidationException`);
  - non-throwing `validate()` (returns `Optional<ValidationError>`).
- Link to the runtime entry points used by generated code:
  `jvm-runtime/src/main/java/io/spine/validation/ValidatableMessage.java`,
  `jvm-runtime/src/main/java/io/spine/validation/ValidatingBuilder.java`,
  `jvm-runtime/src/main/java/io/spine/validation/Validate.java`,
  `jvm-runtime/src/main/java/io/spine/validation/ValidationException.java`,
  `jvm-runtime/src/main/kotlin/io/spine/validation/MessageExtensions.kt`.

7) Verification pass (keep it tight; fix only doc-related issues)
- From `docs/`, run:
  - `./gradlew embedCode`
  - `./gradlew checkSamples`
  - `./gradlew buildSite`
- If something fails, fix the docs, embedded snippet markers, or example sources until green.
