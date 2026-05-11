---
title: Procedures
description: Step-by-step recipes for recurring documentation-related tasks.
headline: Documentation
---

# Procedures

Each section below is an executable recipe. Unless stated otherwise, run
commands from the repository root and prefix Gradle tasks with `./gradlew`.
The conceptual background for each procedure lives on the sibling pages:
"[Build tasks](build-tasks.md)", "[Embedded examples](embedded-examples.md)",
and "[External tooling](tooling.md)".

## Incrementing the version of Validation

1. Update the version literal in [`version.gradle.kts`][version-gradle]:

   ```kotlin
   val validationVersion by extra("<new-version>")
   ```

2. Propagate the new version into the example projects' plugin coordinates:

   ```bash
   ./gradlew :docs:updatePluginVersions
   ```

   This rewrites the `io.spine.validation` plugin version (and the
   `io.spine.core-jvm` plugin version) in every `build.gradle.kts` under
   `docs/_examples/`.

3. Commit `version.gradle.kts` on its own with:

   ```text
   Bump version -> `<new-version>`
   ```

4. Commit the remaining files touched by step 2 with:

   ```text
   Bump Validation -> `<new-version>`
   ```

5. Run a clean build and refresh the dependency reports:

   ```bash
   ./gradlew clean build
   ```

   Then commit the regenerated `pom.xml` and `dependencies.md` with:

   ```text
   Update dependency reports
   ```

   This step follows the same convention used elsewhere in the Spine SDK
   projects.

## Updating the version of the CoreJvm Compiler

1. Update the version in the `CoreJvmCompiler` dependency object under
   [`buildSrc`][core-jvm-compiler-dep].

2. Propagate the new version into the example projects' plugin coordinates:

   ```bash
   ./gradlew :docs:updatePluginVersions
   ```

   This rewrites the `io.spine.core-jvm` plugin version (and the
   `io.spine.validation` plugin version) in every `build.gradle.kts` under
   `docs/_examples/`.

## Refreshing embedded example sources from Spine Time

The Spine Time library at `docs/_time/` is a Git submodule. Use these
steps when first cloning the repository, when refreshing the submodule to
its default remote branch, or when intentionally pinning it to a specific
commit.

1. Initialize and fetch the submodule on a fresh clone:

   ```bash
   git submodule update --init --recursive docs/_time
   ```

2. To refresh `_time/` to its default remote branch and re-embed snippets
   from that revision, run:

   ```bash
   ./gradlew :docs:embedCode
   ./gradlew :docs:checkSamples
   ```

   The `:docs:embedCode` task runs
   `git submodule update --remote --merge --recursive` before invoking the
   embedding tool, so this path intentionally accepts the latest remote
   revision selected by the submodule configuration.

3. To pin `_time/` to a specific commit instead, fetch and check it out
   inside the submodule:

   ```bash
   cd docs/_time
   git fetch origin
   git checkout <commit-or-ref>
   cd -
   ```

4. Re-embed snippets without running `:docs:embedCode`, because that task
   would move the submodule back to the default remote branch before
   embedding:

   ```bash
   ./gradlew :docs:updatePluginVersions
   cd docs/_bin
   ./embed-code-macos -config-path="../_settings/embed-code.yml" -mode="embed"
   cd -
   ./gradlew :docs:checkSamples
   ```

5. Commit the updated submodule pointer and any snippets changed by the
   embedding step:

   ```bash
   git add docs/_time docs/content/docs
   git commit -m "Update Spine Time submodule -> <commit-or-ref>"
   ```

## Adding a new embedded code example

1. If the source file lives outside the existing source roots in
   [`_settings/embed-code.yml`][embed-code-yml], add a new entry under
   `code-path` (a `name` plus a path relative to `docs/_bin/`). If the
   file extension is not yet listed, add a glob under `code-includes`.

2. In the target Markdown page, write an `<embed-code>` element followed
   by an empty fenced code block. For example:

   ```markdown
   <embed—code
     file="$root/version.gradle.kts"
     start="val validationVersion"
     end="val validationVersion">
   </embed—code>
   ```

   The `start` and `end` attributes are regular expressions matched
   against single lines; the matching lines are included.

3. Embed and verify:

   ```bash
   ./gradlew :docs:embedCode
   ./gradlew :docs:checkSamples
   ```

   The fenced block under the element is now populated with the snippet.

4. Preview the page locally (see "Previewing the documentation locally"
   below) and confirm the snippet renders with the right syntax
   highlighting.

5. Commit the page, the snippet contents, and any changes to
   `_settings/embed-code.yml`.

For the underlying mechanics, see "[Embedded examples](embedded-examples.md)".

## Adding or updating a documentation page

1. Create or edit the Markdown file under
   `docs/content/docs/validation/<section>/`. Match the frontmatter
   convention used by the surrounding pages (`title`, `description`,
   `headline: Documentation`).

2. Keep the navigation in sync. Update
   `docs/data/docs/validation/2-0-0-snapshot/sidenav.yml`:

   - For a new page, add a `page`/`file_path` entry under the
     appropriate parent. The `file_path` is relative to
     `docs/content/docs/validation/` and omits the `.md` extension; an
     `_index.md` maps to its directory path (for example,
     `developer/documentation`).
   - For a renamed or moved page, update the existing `file_path`.
   - For a deleted page, remove the entry.

   Do not edit `sidenav.yml` files under other version directories
   unless you are intentionally amending a historical version.

3. Preview locally (see "Previewing the documentation locally") and
   click through every internal link.

4. Follow the project documentation guidelines in
   [`documentation-guidelines.md`][doc-guidelines] — formatting of file
   and directory names as code, reference-style external links, no
   widows/runts/orphans/rivers.

## Updating `site-commons`

This is the procedure documented under "[Theme updates][theme-updates]"
in the `site-commons` README, wrapped with the verification steps used
in this repository.

1. From `docs/_preview/`, pull the latest theme version:

   ```bash
   cd docs/_preview
   hugo mod get -u github.com/SpineEventEngine/site-commons
   cd -
   ```

2. Preview the site and click through the navigation, code blocks, and
   embedded snippets:

   ```bash
   ./gradlew :docs:runSite
   ```

3. Commit and push the resulting changes to
   `docs/_preview/go.mod` and `docs/_preview/go.sum`:

   ```bash
   git add docs/_preview/go.mod docs/_preview/go.sum
   git commit -m "Update site-commons"
   ```

## Updating `embed-code-go`

The tool is consumed as prebuilt binaries under `docs/_bin/`. The
authoritative copies are published by the upstream project under
[`embed-code-go/bin`][embed-code-bin]. We do *not* build the tool as
part of the Validation documentation build — see the upstream repository
for any build, test, or release questions.

1. From [`embed-code-go/bin`][embed-code-bin], download the three
   binaries on the branch or tag you want to ship:
   `embed-code-linux`, `embed-code-macos`, `embed-code-windows.exe`.

2. Replace the files of the same names under `docs/_bin/`.

3. Make sure the macOS and Linux binaries are executable:

   ```bash
   chmod +x docs/_bin/embed-code-macos docs/_bin/embed-code-linux
   ```

4. Verify that the new binaries still produce the same embedded output
   the repository expects:

   ```bash
   ./gradlew :docs:checkSamples
   ```

   If `checkSamples` reports drift caused by the tool itself (formatting,
   whitespace, …), run `./gradlew :docs:embedCode` and inspect the diff
   before committing.

5. Commit the updated binaries under `docs/_bin/` with a message that
   names the upstream commit they were pulled from.

## Building the documentation locally

1. Build the site:

   ```bash
   ./gradlew :docs:buildSite
   ```

2. The generated static site appears under `docs/_preview/public/`. Open
   `docs/_preview/public/index.html` in a browser to inspect the output,
   or serve the directory through any static file server.

3. Treat this output as a *staging artifact only*. Publication to
   spine.io happens through the main documentation project, not from
   this repository — see "[Documentation process](_index.md)" for the
   relationship.

## Previewing the documentation locally

1. Start the Hugo dev server:

   ```bash
   ./gradlew :docs:runSite
   ```

2. Open the URL printed by `hugo server`, typically
   `http://localhost:1313/`.

3. Edits to Markdown files under `docs/content/` hot-reload
   automatically. Edits to `docs/data/` files (including `sidenav.yml`)
   and to Hugo layouts require a restart of the task.

4. To stop the server, press `Ctrl+C` in the terminal running the task.

## What's next

- "[Documentation process](_index.md)" — overview and how this section
  fits into the wider documentation pipeline.
- "[Build tasks](build-tasks.md)" — reference for every Gradle task
  these recipes invoke.

[version-gradle]: https://github.com/SpineEventEngine/validation/blob/master/version.gradle.kts
[core-jvm-compiler-dep]: https://github.com/SpineEventEngine/validation/blob/master/buildSrc/src/main/kotlin/io/spine/dependency/local/CoreJvmCompiler.kt
[embed-code-yml]: https://github.com/SpineEventEngine/validation/blob/master/docs/_settings/embed-code.yml
[doc-guidelines]: https://github.com/SpineEventEngine/validation/blob/master/.agents/documentation-guidelines.md
[theme-updates]: https://github.com/SpineEventEngine/site-commons#theme-updates
[embed-code-go]: https://github.com/SpineEventEngine/embed-code-go
[embed-code-bin]: https://github.com/SpineEventEngine/embed-code-go/tree/master/bin
