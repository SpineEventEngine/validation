---
title: Build tasks
description: The Gradle tasks defined for the documentation module, what they do, and how they are wired.
headline: Documentation
---

# Build tasks

The `:docs` subproject is configured in [`docs/build.gradle.kts`][docs-build].
The tasks declared there are thin Gradle wrappers around shell scripts in
`docs/_script/` and the `embed-code-go` binaries in `docs/_bin/`. This page
documents every task, what it does, where it lands its output, and how the
tasks depend on each other.

## Working directory and paths

All shell scripts under `docs/_script/` assume that `docs/` is the current
working directory. The Gradle tasks take care of this automatically — the
`Exec` tasks invoke each script with an absolute path under `$projectDir`, so
running them as `./gradlew :docs:<task>` from the repository root works.

When invoking the scripts directly (for example, during debugging), `cd` into
`docs/` first:

```bash
cd docs
./_script/hugo-serve
```

The scripts navigate further on their own — for example, `_script/hugo-serve`
enters `_preview/` before launching `hugo server`, and `_script/embed-code`
enters `_bin/` before invoking the embedding tool.

## Hugo tasks

These tasks drive Hugo via the scripts in `docs/_script/`.

### `installDependencies`

Runs `_script/install-dependencies`, which executes `npm install` inside
`_preview/`. The script installs the Node devDependencies declared in
`_preview/package.json` (`postcss`, `autoprefixer`,
`@fullhuman/postcss-purgecss`, and so on). All Hugo-related tasks depend on
this task, so it is rarely invoked manually.

### `runSite`

Runs `_script/hugo-serve` → `hugo server` from `_preview/`. The Hugo dev
server hot-reloads pages on edits to `docs/content/`. This is the standard
way to preview documentation changes locally; see the "Previewing the
documentation locally" recipe in "[Procedures](procedures.md)".

### `buildSite`

Runs `_script/hugo-build` → `hugo` from `_preview/`. The Hugo build emits the
static site under `docs/_preview/public/`. The output is intended for local
inspection only — it is *not* the artifact published to `spine.io`. See the
"Building the documentation locally" recipe in
"[Procedures](procedures.md)".

## Plugin-version tasks

These tasks keep the plugin coordinates in the example projects under
`docs/_examples/` aligned with the versions used elsewhere in the
repository.

The task type is [`UpdatePluginVersion`][update-plugin-version], defined in
`buildSrc/`. It scans recursively for `build.gradle.kts` files under the
configured directory and rewrites declarations of the form
`id("<plugin-id>") version "<version>"`. When `kotlinVersion` is also set,
it additionally rewrites `kotlin("<plugin>") version "<version>"` lines.

### `updateValidationPluginVersion`

Updates `id("io.spine.validation") version "…"` in every `build.gradle.kts`
under `docs/_examples/` to `validationVersion` from
[`version.gradle.kts`][version-gradle], and updates `kotlin("…") version "…"`
to `Kotlin.version`.

### `updateCoreJvmPluginVersion`

Updates `id("io.spine.core-jvm") version "…"` to `CoreJvmCompiler.version`
from the [`CoreJvmCompiler`][core-jvm-compiler-dep] dependency object in
`buildSrc/`.

### `updatePluginVersions`

Aggregator that depends on both tasks above. This is the task contributors
run after bumping the Validation version or the CoreJvm Compiler version;
see the recipes in "[Procedures](procedures.md)".

## Code-embedding tasks

These tasks invoke the `embed-code-go` binaries under `docs/_bin/`. Both
depend on `updatePluginVersions`, so the example sources always reflect the
current plugin versions before snippets are embedded or checked.

### `embedCode`

Runs `_script/embed-code`. The script first runs

```bash
git submodule update --remote --merge --recursive
```

so that `_examples/` and `_time/` track their default remote branches. It
then invokes `embed-code-macos` with `-mode="embed"` and the configuration at
`_settings/embed-code.yml`, which rewrites the fenced regions in the Markdown
pages under `content/` from their source snippets. Run this task manually
after editing an embedded snippet or after pointing a source root at a new
commit. See "[Embedded examples](embedded-examples.md)" for the
configuration format and "[Procedures](procedures.md)" for the day-to-day
recipe.

### `checkSamples`

Runs `_script/check-samples`. The script selects `embed-code-linux` when the
`GITHUB_ACTIONS` environment variable is `true`, and `embed-code-macos`
otherwise, then invokes it with `-mode="check"`. The tool exits non-zero if
any embedded snippet drifts from its source. This task is executed by the
[Code Embedding][check-code-embedding] GitHub workflow on every pull
request.

## Aggregate tasks

These tasks tie the example projects under `docs/_examples/` into the main
build so that the embedded code is actually compilable.

### `publishAllToMavenLocal`

Aggregator that depends on every `PublishToMavenLocal` task across the root
project's subprojects. Running it stages the current Validation artifacts to
the local Maven repository, where the example projects can resolve them
through `mavenLocal()`.

### `buildExamples`

Custom `RunGradle` task that delegates to the `buildAll` task inside
`docs/_examples/`. It depends on `publishAllToMavenLocal` (so the examples
build against the just-published artifacts) and on `updatePluginVersions`
(so the example build files reference the current plugin versions).

### `buildAll`

The conventional "build everything for the documentation" entry point.
Depends on `publishAllToMavenLocal` and `buildExamples`. This is the task
invoked by the [Code Embedding][check-code-embedding] GitHub workflow to
validate that every embedded snippet still compiles inside a real Gradle
build.

## Task dependency map

| Task                            | Depends on                                                  |
|---------------------------------|-------------------------------------------------------------|
| `installDependencies`           | —                                                           |
| `runSite`                       | `installDependencies`                                       |
| `buildSite`                     | `installDependencies`                                       |
| `updateValidationPluginVersion` | —                                                           |
| `updateCoreJvmPluginVersion`    | —                                                           |
| `updatePluginVersions`          | `updateValidationPluginVersion`, `updateCoreJvmPluginVersion` |
| `embedCode`                     | `updatePluginVersions`                                      |
| `checkSamples`                  | `updatePluginVersions`                                      |
| `publishAllToMavenLocal`        | all `PublishToMavenLocal` tasks across the root project     |
| `buildExamples`                 | `publishAllToMavenLocal`, `updatePluginVersions`            |
| `buildAll`                      | `publishAllToMavenLocal`, `buildExamples`                   |

## What's next

- "[Embedded examples](embedded-examples.md)" — how `embedCode` and
  `checkSamples` decide what source goes into which page.
- "[External tooling](tooling.md)" — the `site-commons` Hugo theme that the
  Hugo tasks render against, and how the `embed-code-go` binaries get into
  `_bin/`.
- "[Procedures](procedures.md)" — the recipes that combine these tasks into
  end-to-end contributor workflows.

[docs-build]: https://github.com/SpineEventEngine/validation/blob/master/docs/build.gradle.kts
[update-plugin-version]: https://github.com/SpineEventEngine/validation/blob/master/buildSrc/src/main/kotlin/io/spine/gradle/docs/UpdatePluginVersion.kt
[version-gradle]: https://github.com/SpineEventEngine/validation/blob/master/version.gradle.kts
[core-jvm-compiler-dep]: https://github.com/SpineEventEngine/validation/blob/master/buildSrc/src/main/kotlin/io/spine/dependency/local/CoreJvmCompiler.kt
[check-code-embedding]: https://github.com/SpineEventEngine/validation/blob/master/.github/workflows/check-code-embedding.yml
