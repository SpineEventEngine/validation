---
slug: docs-validation-gradle-plugin-page
branch: address-issues
owner: claude
status: draft
started: 2026-05-21
related-tasks:
  - warnings-suppression
---

## Goal

Add a dedicated documentation page for the **Validation Gradle plugin** and
its `validation { ... }` extension. The page explains what the plugin does
and how to configure it through its DSL. It sits **after** the
*Custom validation* section in the user-guide navigation.

Success criteria:

1. New section appears in the rendered docs between
   *05 Custom validation* and any subsequent section, accessible from the
   left navigation as **Validation Gradle plugin** (or chosen title).
2. The page re-iterates the plugin's purpose, cross-linking the existing
   partial coverage in *01 Getting started → Adding to build*.
3. The page documents every property of `ValidationExtension`, with code
   snippets showing both the default and a customized configuration; and
   explains *why* the DSL is shaped the way it is (positive polarity,
   nested blocks, lazy `Property<T>`).
4. The first published version covers what exists today (`enabled`) plus
   `warnings { unsignedFields }` once the
   [`warnings-suppression`](warnings-suppression.md) task lands.

## Context

- Docs use Hugo with numeric directory prefixes for ordering (e.g.,
  `01-getting-started`, `05-custom-validation`).
- Existing user-doc sections under
  `docs/content/docs/validation/user/`:
  - `00-intro`
  - `01-getting-started` — contains
    [`adding-to-build.md`](http://localhost:1313/docs/validation/user/01-getting-started/adding-to-build/)
    with a partial mention of the plugin
    (`#adding-gradle-plugins-to-the-build`).
  - `02-concepts`
  - `03-built-in-options`
  - `04-validators`
  - `05-custom-validation`
- Pages use a `---` YAML frontmatter with `title`, `description`,
  `headline: Documentation`. Internal links use relative `.md` paths.
  Inline code samples are shown in fenced ```kotlin``` blocks; live
  build-file excerpts use `<embed-code .../>` shortcodes — out of scope
  for this first revision (we will hand-write the snippets).
- The plugin extension as of this writing
  (`gradle-plugin/src/main/kotlin/io/spine/tools/validation/gradle/ValidationExtension.kt`):
  - `enabled: Property<Boolean>` (default `true`)
  - **after `warnings-suppression`:** nested `warnings { unsignedFields }`
- The Gradle plugin ID is `io.spine.validation`; the extension is registered
  under the name `validation` and nests under the project-wide `spine { }`
  block.

## Design

### 1. Location and navigation

- New directory: `docs/content/docs/validation/user/06-gradle-plugin/`
- Index file: `_index.md` (Hugo convention used by every other section).
- Numeric prefix `06-` places it right after `05-custom-validation` and
  before any future section.

If a single page proves enough, only `_index.md` is needed. If the page
grows beyond a comfortable read (e.g., adds a configuration-recipes
sub-page), additional `*.md` files can be added later — the section
directory leaves room for that without re-numbering.

### 2. Title and frontmatter

```yaml
---
title: Validation Gradle plugin
description: What the plugin does, and how to configure it via the `validation { ... }` extension.
headline: Documentation
---
```

### 3. Page outline

The page is structured as follows. Each H2 maps to one user need.

1. **`# Validation Gradle plugin`** — top-level heading.
2. **`## What the plugin does`**
   - One paragraph: integrates the Validation Compiler into the build,
     adds the JVM runtime dependency, applies the Protobuf Gradle plugin
     and the Spine Compiler Gradle plugin transitively.
   - Cross-link the install instructions in
     [Adding to build → Adding Gradle plugins to the build](../01-getting-started/adding-to-build.md#adding-gradle-plugins-to-the-build)
     so readers who land here first can install the plugin without
     duplicating those steps on this page.
   - Brief note: the plugin can be applied in two modes (standalone vs.
     CoreJvm); detail stays in *Adding to build*.
3. **`## Configuration via the extension`**
   - Show the canonical zero-config block (everything default):
     ```kotlin
     spine {
         validation {
             // All defaults: validation is enabled; all warnings emitted.
         }
     }
     ```
   - Explain the nesting: `spine { ... }` is the project-wide Spine
     extension; `validation { ... }` is the per-plugin nested extension
     registered under the name `validation`.
4. **`## Properties`** — one H3 per property.
   - **`### enabled`** — type `Property<Boolean>`, default `true`. What
     turning it off does (skips registering the Validation Compiler
     plugin; the runtime dependency is still added — explain *why*, per
     the comment in `ValidationGradlePlugin.kt:76-84`).
   - **`### warnings { ... }`** — nested block. (Added by
     `warnings-suppression`.)
     - **`#### unsignedFields`** — type `Property<Boolean>`, default
       `true`. When `false`, suppresses the *"unsigned integer types are
       not supported in Java"* warning emitted by the bound checks for
       `uint32` / `uint64` fields with `(range)`, `(min)`, or `(max)`.
       Show the suppression snippet:
       ```kotlin
       spine {
           validation {
               warnings {
                   unsignedFields = false
               }
           }
       }
       ```
       Note: the warning still represents a real Java limitation — turn
       it off only when you have read it once and accepted the
       trade-off.
5. **`## Next steps`** — link forward.
   - Back to [Concepts](../02-concepts/_index.md) (if reader is just
     browsing), or
   - [Custom validation](../05-custom-validation/_index.md) for users
     who want to extend the plugin.

### 4. Cross-links to add elsewhere

- In `01-getting-started/adding-to-build.md`, after the existing
  "Adding Gradle plugins to the build" section, add a one-line pointer:
  "For configuration options, see
  [Validation Gradle plugin](../06-gradle-plugin/_index.md)."
- This is a tiny edit and ships with the new page.

## Plan

- [ ] Confirm:
  - section title (`Validation Gradle plugin` vs. another wording?),
  - whether `06-gradle-plugin/_index.md` (section directory) is preferred
    over a single `06-gradle-plugin.md` file at the parent level. Default:
    section directory, matching the existing layout.
- [ ] Create `docs/content/docs/validation/user/06-gradle-plugin/` with
      `_index.md` carrying the outline above. Content tied to the
      *current* `ValidationExtension` shape:
  - includes `enabled` (covered today),
  - includes `warnings { unsignedFields }` (covered once
    `warnings-suppression` merges).
- [ ] Add the forward link from
      `01-getting-started/adding-to-build.md` to the new page.
- [ ] Run the doc preview (Hugo) locally and verify:
  - the new section appears after *Custom validation*;
  - all relative links resolve;
  - the page renders without shortcode errors.
- [ ] Apply the `writer` / `review-docs` skills before opening the PR.
- [ ] Version bump (per `pre-pr`).

## Sequencing

This task is **paired with** `warnings-suppression`:

- The `enabled` documentation can land at any time (covers today's
  surface) and would be useful on its own.
- The `warnings { unsignedFields }` section depends on the
  extension/proto/renderer wiring in `warnings-suppression`.

Preferred: land both in the same PR so the doc page is published
already covering the new property, avoiding a stale snapshot. If that
PR grows too large, split — but ship the docs page with at least
`enabled` so the navigation entry exists.

## Open questions

1. **Single-page vs. section-with-sub-pages.** Stick to one `_index.md`
   for now, or pre-create a section that anticipates future sub-pages
   (e.g., "Recipes", "Migration notes")? My lean: one `_index.md`; split
   later if it gets long.
2. **Embed-code shortcodes for snippets.** The other pages use
   `<embed-code file="$examples/..."/>` to pull live Kotlin from the
   examples repo. For this page, do we want to mirror that pattern
   (requires a matching example in `spine-examples/hello-validation`)
   or hand-write the snippets for the first revision? My lean:
   hand-write for v1 — the snippets are small and self-explanatory —
   and consider example-driven snippets in a follow-up.
3. **Title wording.** "Validation Gradle plugin" vs.
   "Gradle plugin" (since we're already in the Validation docs) vs.
   "Plugin configuration". My lean: "Validation Gradle plugin" to match
   the way *Adding to build* refers to it.

## Log

- 2026-05-21 — drafted, awaiting review
