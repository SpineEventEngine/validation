---
title: Build tasks
description: The Gradle tasks defined for the documentation module, what they do, and how they are wired.
headline: Documentation
---

# Build tasks

<!-- TODO: Introduce `docs/build.gradle.kts`. Explain that the tasks are thin
wrappers around shell scripts under `_script/`, and that all scripts assume
`docs/` as the current working directory. Migrate the contents of
`docs/GRADLE.md` into this page. -->

## Working directory and paths

<!-- TODO: Document the convention that scripts and Gradle tasks run from
`docs/`. Describe the role of `_script/`, `_bin/`, `_preview/`,
`_settings/`. -->

## Hugo tasks

<!-- TODO: `installDependencies`, `runSite`, `buildSite`. Where the output
lands (`_preview/public/`) and what is regenerated on each run. -->

## Plugin-version tasks

<!-- TODO: `updateValidationPluginVersion`, `updateCoreJvmPluginVersion`,
`updatePluginVersions`. Describe what the `UpdatePluginVersion` task type
does and where it is defined (`buildSrc`). -->

## Code-embedding tasks

<!-- TODO: `embedCode` and `checkSamples`. Mention the binary-selection logic
in the shell scripts and the `git submodule update` step inside
`_script/embed-code`. Note that `checkSamples` is run by
`.github/workflows/check-code-embedding.yml`. -->

## Aggregate tasks

<!-- TODO: `publishAllToMavenLocal`, `buildExamples`, `buildAll`. Explain how
they tie the example projects under `_examples/` into the main build, and that
`buildAll` is what CI runs to validate the embedded snippets. -->

## Task dependency map

<!-- TODO: Small diagram or table showing which tasks depend on which. -->
