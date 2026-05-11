---
title: Documentation process
description: How the Spine Validation documentation is authored, built, previewed, and released.
headline: Documentation
---

# Documentation process

This section describes the technical side of the Spine Validation
documentation: how the `docs/` module is organized, which Gradle tasks drive
the build, how source code is embedded into pages, which external tools the
build depends on, and the recurring procedures contributors need to perform.

The `docs/` module is a *staging area*, not a published site. The pages and
data under it are merged into the main documentation project
([SpineEventEngine/documentation][main-documentation]) and published from
there to the [`spine.io`][spine-io] website. The build wiring on this page
exists to let contributors preview, validate, and iterate on the content of
this repository before that merge happens.

## What lives under `docs/`

The module is laid out as a Hugo project supplemented by Gradle wiring and a
Go-based code-embedding tool. The top-level layout is:

| Path             | Role                                                                       |
|------------------|----------------------------------------------------------------------------|
| `content/`       | Markdown pages of the documentation, organized by section and version.     |
| `data/`          | Site data files, including the per-version `sidenav.yml` navigation.       |
| `layouts/`       | Hugo layout overrides specific to the Validation site.                     |
| `build.gradle.kts` | Gradle build script for the `:docs` subproject. See "[Build tasks](build-tasks.md)". |
| `_preview/`      | Hugo project root used to render and serve the site locally.               |
| `_examples/`     | Git submodule with the Hello Validation example projects.                  |
| `_time/`         | Git submodule with the Spine Time library, used as the implementation example for custom validation. |
| `_settings/`     | Configuration for the code-embedding tool — see "[Embedded examples](embedded-examples.md)". |
| `_script/`       | Shell scripts that the Gradle tasks shell out to.                          |
| `_bin/`          | Prebuilt `embed-code-go` binaries for Linux, macOS, and Windows.           |
| `_options/`      | A copy of the Validation Protobuf options surface kept alongside the docs. |

The underscore prefix marks directories that are *not* part of the Hugo site
tree — Hugo ignores top-level paths starting with `_`. Everything Hugo
actually renders lives under `content/`, `data/`, and `layouts/`.

## Relationship to the `spine.io` website

There is no automated deployment from this repository. The CI workflows
under `.github/workflows/` only build the example sources
(`:docs:buildAll`) and verify that the embedded snippets are in sync with
their sources (`:docs:checkSamples`). Publication happens through the main
documentation project, which pulls the Markdown content of `docs/content/`
together with content from other Spine projects and renders the unified
[spine.io](https://spine.io) site.

This split has two practical consequences for contributors:

- The Hugo site rendered under `_preview/public/` is for local inspection
  only; treat it as a staging artifact, not as the published site.
- Anything that the main documentation project does not pick up — for
  example, files outside `content/` and `data/` — is *not* shipped, no
  matter how it renders locally.

## How to read the rest of this section

- "[Build tasks](build-tasks.md)" — every Gradle task defined in
  `docs/build.gradle.kts`, what each does, and how they depend on each
  other.
- "[Embedded examples](embedded-examples.md)" — how source code from the
  Validation modules and the `_examples`/`_time` submodules ends up inside
  the documentation pages.
- "[External tooling](tooling.md)" — the `site-commons` Hugo theme and the
  `embed-code-go` tool, including how each is wired into the build.
- "[Procedures](procedures.md)" — step-by-step recipes for the recurring
  documentation tasks (version bumps, submodule refresh, adding pages and
  embedded examples, updating external tooling, building and previewing
  locally).

[main-documentation]: https://github.com/SpineEventEngine/documentation
[spine-io]: https://spine.io/
