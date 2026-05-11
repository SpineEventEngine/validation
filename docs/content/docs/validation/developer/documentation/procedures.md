---
title: Procedures
description: Step-by-step recipes for recurring documentation-related tasks.
headline: Documentation
---

# Procedures

<!-- TODO: One-line preamble: each section below is an executable recipe with
the exact commands, files touched, and expected outcome. -->

## Incrementing the version of Validation

<!-- TODO: Steps from the brief:
1. Update `version.gradle.kts`.
2. Run `./gradlew :docs:updatePluginVersions`.
3. Commit `version.gradle.kts` with message `Bump version -> \`<new_version>\``.
4. Commit the remaining touched files with `Bump Validation -> \`<new_version>\``.
5. Run `./gradlew clean build` and commit `pom.xml` and `dependencies.md`
   with `Update dependency reports`.
-->

## Updating the version of the CoreJvm Compiler

<!-- TODO:
1. Update the version in `CoreJvmCompiler` under `buildSrc`.
2. Run `./gradlew :docs:updatePluginVersions`.
-->

## Refreshing embedded example sources from Spine Time

<!-- TODO: Initial `git submodule init && git submodule update` for `_time`;
switching to a specific commit; note that `_script/embed-code` runs
`git submodule update --remote --merge --recursive` automatically. -->

## Adding a new embedded code example

<!-- TODO:
1. Declare or extend a source root in `docs/_settings/embed-code.yml`.
2. Reference the snippet from a Markdown page using the embed syntax.
3. Run `./gradlew :docs:embedCode` to embed.
4. Run `./gradlew :docs:checkSamples` to verify consistency.
5. Preview locally with `./gradlew :docs:runSite`.
-->

## Adding or updating a documentation page

<!-- TODO:
1. Create or edit the Markdown file under `docs/content/docs/validation/...`.
2. Update `docs/data/docs/validation/2-0-0-snapshot/sidenav.yml` to match.
3. Preview locally with `./gradlew :docs:runSite`.
4. Validate internal links.
Reference the documentation guidelines in `.agents/documentation-guidelines.md`.
-->

## Updating `site-commons`

<!-- TODO:
1. From `docs/_preview/`, run `go get github.com/SpineEventEngine/site-commons@<commit-or-tag>`.
2. Run `go mod tidy`.
3. Preview with `./gradlew :docs:runSite` and verify rendering.
4. Commit `docs/_preview/go.mod` and `docs/_preview/go.sum`.
-->

## Updating `embed-code-go`

<!-- TODO:
1. Build the binaries from the `SpineEventEngine/embed-code-go` repository
   for Linux, macOS, and Windows.
2. Replace `docs/_bin/embed-code-linux`, `embed-code-macos`, and
   `embed-code-windows.exe`.
3. Run `./gradlew :docs:checkSamples` to verify the new binary still passes.
4. Commit the updated binaries.
-->

## Building the documentation locally

<!-- TODO:
1. `./gradlew :docs:buildSite`.
2. Output appears under `docs/_preview/public/`.
3. Note that this output is for inspection only; publication happens via
   merging content into `SpineEventEngine/documentation`.
-->

## Previewing the documentation locally

<!-- TODO:
1. `./gradlew :docs:runSite`.
2. Open the URL printed by `hugo server` (typically `http://localhost:1313`).
3. Edits to Markdown under `docs/content/` hot-reload automatically.
-->
