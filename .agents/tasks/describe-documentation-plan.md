# Plan: Describe the technical details of the documentation process

This plan elaborates the brief in
[`describe-documentation-plan-draft.md`](describe-documentation-plan-draft.md).
The goal is to deliver a dedicated documentation sub-tree under the existing
Developer guide that explains, end to end, how the Spine Validation
documentation is authored, built, previewed, and released.

## 1. Outcome

A new sub-tree appears as the last item of the "Validation developer guide"
section of the documentation and answers the following questions:

- What is the `docs` module and how does it fit into the `spine.io` website
  build?
- How is the documentation built locally and previewed?
- Which Gradle tasks are involved, what do they do, and where do their outputs
  land?
- How are source code examples embedded into the documentation, and why is
  Spine Time wired in as a Git submodule?
- How is `settings/embed-code.yml` structured, and what are the source roots?
- How do contributors perform the recurring documentation-related procedures
  (version bumps, tool updates, submodule refresh, adding pages, adding
  embedded examples)?

In addition, `docs/README.md` is introduced as the in-repo entry point, and
`docs/GRADLE.md` is retired because its content is folded into the new pages.

## 2. Deliverables

### 2.1 New documentation sub-tree

Location:
`docs/content/docs/validation/developer/documentation/`

Pages:

- `_index.md` — Overview. Purpose of the `docs` module; relationship to Hugo
  and the `_preview` directory; how the module participates in the `spine.io`
  website build; reading map for the child pages.
- `build-tasks.md` — Gradle build tasks defined in `docs/build.gradle.kts`,
  what each task does, dependencies between them, and the paths the scripts
  rely on. Folds in the content of `docs/GRADLE.md`. Covers `:docs:runSite`
  (local Hugo preview) and the task that produces the publishable output.
- `embedded-examples.md` — How example sources are included in the
  documentation; the role of Spine Time as the implementation-example library
  and the rationale for wiring it as a Git submodule under `_time`; structure
  of `settings/embed-code.yml` and its source roots.
- `tooling.md` — External tooling the docs build depends on:
  - `site-commons` (Go module) — what it provides, why it is a Go module, how
    it is consumed.
  - `embed-code-go` — what it does, how it is consumed by the docs build.
- `procedures.md` — Step-by-step recipes for recurring tasks (see section 3).

### 2.2 In-repo entry point and cleanup

- Add `docs/README.md` with a brief description of the `docs` module and a
  link to the new overview page (`developer/documentation/_index.md`).
- Remove `docs/GRADLE.md` once its content has been migrated into
  `developer/documentation/build-tasks.md`. Verify there are no remaining
  references to `GRADLE.md` before deletion.

### 2.3 Navigation update

- Update `docs/data/docs/validation/2-0-0-snapshot/sidenav.yml`: add a nested
  entry as the last item under the `developer` key. Use `file_path` values
  relative to the version `content_path` (`docs/validation`), without the
  `.md` suffix, and treating `_index.md` as the directory path. Preserve the
  file's existing ordering, indentation, quoting style, and comments.

Proposed entry (final wording to be confirmed when drafting):

```yaml
- page: Documentation process
  key: documentation
  children:
    - page: Overview
      file_path: developer/documentation
    - page: Build tasks
      file_path: developer/documentation/build-tasks
    - page: Embedded examples
      file_path: developer/documentation/embedded-examples
    - page: External tooling
      file_path: developer/documentation/tooling
    - page: Procedures
      file_path: developer/documentation/procedures
```

## 3. Procedures to document on `procedures.md`

Each procedure is presented as numbered steps with the exact commands, the
files touched, and the expected outcome.

1. **Incrementing the version of Validation** — as drafted in the brief
   (update `version.gradle.kts`, run `:docs:updatePluginVersions`, commit
   sequence, run `clean build`, commit `pom.xml` and `dependencies.md` as
   `Update dependency reports`).
2. **Updating the version of the CoreJvm Compiler** — update the
   `CoreJvmCompiler` dependency object under `buildSrc`, run
   `:docs:updatePluginVersions`.
3. **Refreshing the embedded example sources from Spine Time** — initial
   submodule init/update for `_time`, switching to a specific commit, and how
   the change propagates through the embed-code pipeline.
4. **Adding a new embedded code example** — declare or extend a source root
   in `settings/embed-code.yml`, reference the snippet from a Markdown page,
   verify it renders in preview.
5. **Adding or updating a documentation page** — create the Markdown file
   under `content/docs/validation/...`, update the corresponding
   `sidenav.yml` entry, preview locally, validate links.
6. **Updating `site-commons`** — bump the Go module version in `go.mod`,
   tidy/vendor as required, verify the preview still renders correctly.
7. **Updating `embed-code-go`** — bump the tool version, re-run the embed
   pipeline, verify embedded snippets.
8. **Building the documentation locally** — which Gradle task produces the
   publishable output, where the output lands, and how it is consumed by the
   `spine.io` website build.
9. **Previewing the documentation locally** — `:docs:runSite`, prerequisites,
   how to reach the preview, troubleshooting tips.

## 4. Cross-cutting content notes

- Use fenced code blocks for every shell command and file path. Format file
  and directory names as inline code.
- Follow `.agents/documentation-guidelines.md` for quotation style, footnote
  links for external `https://` URLs, and widow/runt/orphan/river handling.
- Match identifiers (Gradle task names, Kotlin/Go module names, YAML keys) to
  what is in the repository at the time of writing.
- Prefer concrete, executable steps and short paragraphs over abstract
  description.

## 5. Research items to verify before writing

These must be confirmed against the current repo state during drafting:

- Exact set and names of Gradle tasks defined in `docs/build.gradle.kts`
  (including `:docs:runSite`, `:docs:updatePluginVersions`, and the build
  task that produces the publishable output) and their inter-task
  dependencies.
- Output directory of the documentation build and how/where the `spine.io`
  website pipeline consumes it.
- Current contents and source-root structure of
  `docs/_settings/embed-code.yml`.
- The exact role of the `_preview` directory in the local Hugo preview flow
  (input/output, generated vs. checked-in).
- How `_time` is wired as a Git submodule (path, default branch/commit
  policy) and which Gradle tasks consume it.
- Whether `site-commons` is consumed via `go.mod` directly under `docs/`,
  via a Hugo module config, or both.
- Current means of pinning the `embed-code-go` version used by the build.
- The version selector for the documentation site (the `2-0-0-snapshot`
  versus `2-0-x` entries in `docs/data/versions.yml`) to confirm the
  sub-tree is added to the main version only.

## 6. Work breakdown

Ordered for incremental review:

1. **Discovery pass.** Walk the repo and answer every research item in
   section 5. Capture findings as notes (not committed) to feed the drafts.
2. **Skeleton commit.** Create empty `_index.md`, `build-tasks.md`,
   `embedded-examples.md`, `tooling.md`, `procedures.md` with frontmatter
   and section headings only. Update `sidenav.yml` with the new entry.
   Verify the navigation renders via `:docs:runSite`.
3. **Draft `_index.md`** (overview) and `build-tasks.md` (migrate
   `GRADLE.md` content; expand with task descriptions, paths, and outputs).
4. **Draft `embedded-examples.md`** (Spine Time submodule rationale,
   `embed-code.yml` structure, page-side syntax for embedding snippets).
5. **Draft `tooling.md`** (`site-commons`, `embed-code-go`: purpose,
   consumption, update mechanics — pure description here; the update steps
   live in `procedures.md`).
6. **Draft `procedures.md`** covering all nine procedures with verified
   commands and expected outcomes.
7. **Add `docs/README.md`** pointing to `_index.md`; remove `docs/GRADLE.md`
   after confirming no remaining references.
8. **Validation pass.**
   - `./gradlew :docs:runSite` and click through every new page; verify
     navigation, code blocks, embedded snippets, and internal links.
   - Run `./gradlew dokka` if any Kotlin/Java source comments were touched.
   - Re-read each page for adherence to the documentation guidelines
     (quotation rules, reference-style external links, no widows/runts).

## 7. Out of scope

- Changes to historical doc versions (`2-0-x` and earlier) or their
  `sidenav.yml` files.
- Documenting unrelated developer procedures not part of the documentation
  pipeline.
- Restructuring the existing Developer guide pages beyond appending the new
  sub-tree.
