# Spine Validation documentation plan (repo-local)

This repository contains the **staging** content for Spine Validation docs.
The goal of this plan is to turn the existing drafts + references into a coherent,
buildable documentation set, without expanding scope unnecessarily.

## Key locations (source of truth)

- Docs content (Hugo): `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/`
- Protobuf options reference (for built-ins):
  - `/Users/sanders/Projects/Spine/validation/docs/_options/options.proto`
  - `/Users/sanders/Projects/Spine/validation/docs/_options/time_options.proto`
- Example projects for embedded snippets: `/Users/sanders/Projects/Spine/validation/docs/_examples/`
- Docs build notes: `/Users/sanders/Projects/Spine/validation/docs/GRADLE.md`

## Definition of done (for a “first complete docs cut”)

- Navigation is consistent (no dead links between pages).
- Embedded code snippets are up-to-date (`checkSamples` passes).
- Preview site builds locally (`buildSite` succeeds).
- “Getting started” can be followed end-to-end without guessing.

## Minimal task list (in order)

1) Information architecture (IA): make Hugo navigation coherent
- Status: DONE (2026-02-23).
- Added/updated section landing pages:
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/_index.md`
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/09-developers-guide/_index.md`
- Replaced broken `.../index.md` links with directory links where appropriate:
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/00-intro/_index.md`
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/01-getting-started/_index.md`
- Added “What’s next” navigation to keep a clear reading path:
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/00-intro/target-audience.md`
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/00-intro/philosophy.md`
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/09-developers-guide/architecture.md`
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/09-developers-guide/key-modules.md`
  - `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/01-getting-started/first-model.md`
- Fixed an obvious broken image reference:
  `/Users/sanders/Projects/Spine/validation/docs/content/docs/validation/08-custom-validation/_index.md`

2) Complete “Getting started” flow
- Validate that “Adding to build” + “First model” cover:
  importing options, build-time validation, `build()` vs `buildPartial()`, and `validate()`.

3) Concepts: explain how Validation works (one layer deeper than “getting started”)
- Add a Concepts landing page and an “options overview” page that explains:
  where options come from, how they’re applied, and what code gets generated.
- Keep this conceptual (no option-by-option details yet).

4) Built-in options: publish a minimal reference set
- From `/Users/sanders/Projects/Spine/validation/docs/_options/options.proto` and
  `/Users/sanders/Projects/Spine/validation/docs/_options/time_options.proto`,
  enumerate the built-in options and group them (fields, strings, numbers, collections, message-level, time).
- For each documented option: purpose, supported field types, common pitfalls, and a short `.proto` example.
- Start with the options already used in docs/examples: `(required)`, `(pattern)`, `(min)/(max)`,
  `(distinct)`, `(validate)`, `(when)`.

5) Runtime API usage (Java + Kotlin)
- Document the two primary usage patterns:
  - fail-fast on `build()` (throws `ValidationException`);
  - non-throwing `validate()` (returns `Optional<ValidationError>`).
- Link to the runtime entry points used by generated code:
  `/Users/sanders/Projects/Spine/validation/jvm-runtime/src/main/java/io/spine/validation/ValidatableMessage.java`,
  `/Users/sanders/Projects/Spine/validation/jvm-runtime/src/main/java/io/spine/validation/ValidatingBuilder.java`,
  `/Users/sanders/Projects/Spine/validation/jvm-runtime/src/main/java/io/spine/validation/Validate.java`,
  `/Users/sanders/Projects/Spine/validation/jvm-runtime/src/main/java/io/spine/validation/ValidationException.java`,
  `/Users/sanders/Projects/Spine/validation/jvm-runtime/src/main/kotlin/io/spine/validation/MessageExtensions.kt`.

6) Verification pass (keep it tight; fix only doc-related issues)
- From `/Users/sanders/Projects/Spine/validation/docs/`, run:
  - `./gradlew embedCode`
  - `./gradlew checkSamples`
  - `./gradlew buildSite`
- If something fails, fix the docs, embedded snippet markers, or example sources until green.

## Suggested “multi-agent” split (still one owner)

If you want to use multiple agents, treat them as focused roles that each pick up one task
above and produce a concrete artifact (a page draft, an option table, or a green build).
You remain the single integrator/releaser.
