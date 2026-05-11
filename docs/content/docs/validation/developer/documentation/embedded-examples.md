---
title: Embedded examples
description: How example source code is embedded into the documentation pages.
headline: Documentation
---

# Embedded examples

The documentation embeds snippets from real source files instead of inlining
them as plain Markdown. This keeps the snippets in lock-step with the code
they document: when a method signature, a Protobuf option, or an example
project file changes, the documentation either updates automatically (when
`:docs:embedCode` runs) or fails CI (when `:docs:checkSamples` runs against
a stale page). There are four source pools the documentation pulls from:

1. The Validation library code itself — the repository root and the
   production modules under it, exposed through the `root`, `runtime`,
   `java`, and `context` source roots.
2. The Validation examples — the `docs/_examples/` Git submodule, exposed
   through the `examples` source root.
3. The Spine Time library — the `docs/_time/` Git submodule, exposed
   through the `time` source root.
4. The documentation tree — the `docs/` directory, exposed through the
   `docs` source root.

## Example sources

The `docs/_examples/` directory is a Git submodule pointing to the
[`spine-examples/hello-validation`][hello-validation] repository. It hosts
the small, runnable example projects that the User's Guide walks through:

| Project                       | Purpose                                                                                  |
|-------------------------------|------------------------------------------------------------------------------------------|
| `first-model`                 | The minimal Protobuf model with field-level constraints used in "[Getting started](../../user/01-getting-started/)". |
| `first-model-with-framework`  | The same model extended with `MessageValidator` and registry wiring.                     |
| `external`                    | A consumer project that depends on validated messages from another module.               |

These projects compile and run against the just-built Validation artifacts.
The `:docs:buildExamples` task delegates a `buildAll` invocation into
`docs/_examples/`, and `:docs:publishAllToMavenLocal` stages the Validation
artifacts the example projects resolve through `mavenLocal()`. See
"[Build tasks](build-tasks.md)" for the wiring.

## Spine Time as the implementation example

The custom-validation chapter needs a real, non-trivial implementation of
the extension points described in "[Extension points](../extension-points.md)"
and "[Adding a new built-in validation option](../adding-a-built-in-option.md)".
[Spine Time][spine-time] is that example: it defines its own validation
options (`(when)`, `(time)`) and implements them through the same
extension points the documentation describes.

To embed snippets from Spine Time's actual source — not a copy that would
inevitably drift — the library is included as the `docs/_time/` Git
submodule pointing at [`SpineEventEngine/time`][spine-time]. The
code-embedding tool then reads snippets from that working tree through
the `$time` source root.

## The `_settings/embed-code.yml` file

The configuration file [`docs/_settings/embed-code.yml`][embed-code-yml]
declares the source roots the embedding tool understands, where the
documentation pages live, and which files are considered embeddable.

<embed-code
  file="$docs/_settings/embed-code.yml">
</embed-code>
```yaml
code-path:
  - name: "root"
    path: "../.."
  - name: "examples"
    path: "../_examples"
  - name: "runtime"
    path: "../../jvm-runtime"
  - name: "java"
    path: "../../java"
  - name: "context"
    path: "../../context"
  - name: "time"
    path: "../_time"
  - name: "docs"
    path: ".."
docs-path: "../content/docs/"
code-includes:
  - ".gitignore"
  - "**/*.kts"
  - "**/*.md"
  - "**/*.proto"
  - "**/*.java"
  - "**/*.kt"
  - "**/*.yml"
```

### Source roots (`code-path`)

Each entry maps a logical *name* to a directory *path*, resolved relative
to `docs/_bin/` (because the scripts `cd _bin` before invoking the
binary). A page references a source root by its name, prefixed with `$`.

| Name       | Path                | What it exposes                                                          |
|------------|---------------------|--------------------------------------------------------------------------|
| `root`     | `../..`             | The Validation repository root — top-level files (`version.gradle.kts`, `build.gradle.kts`, …) and any module not separately mapped. |
| `examples` | `../_examples`      | The Hello Validation example projects (the submodule above).             |
| `runtime`  | `../../jvm-runtime` | The `:jvm-runtime` module — runtime library sources used in "[Runtime library](../runtime-library.md)". |
| `java`     | `../../java`        | The `:java` module — Java code-generation sources used in "[Java code generation](../java-code-generation.md)". |
| `context`  | `../../context`     | The `:context` module — the custom-validation context sources.           |
| `time`     | `../_time`          | The Spine Time submodule used as the implementation example for custom validation. |
| `docs`     | `..`                | The `docs/` directory — documentation settings, scripts, and other documentation-local files. |

To embed from Spine Time, use the `$time` source root with a path relative
to `docs/_time/`.

### Pages directory (`docs-path`)

`docs-path: "../content/docs/"` points the tool at the Markdown tree it
should scan when running `embedCode` or `checkSamples`. Only files under
this directory are considered for embedding.

### Embeddable file types (`code-includes`)

The globs under `code-includes` filter which source files the tool will
read when resolving an `<embed-code>` block. Adding a new file extension
(for example, `**/*.json` for embedded JSON snippets) means appending it
to this list.

## Embedding syntax on the page side

An `<embed-code>` element marks a region in a Markdown page that the tool
manages. The tool replaces the *fenced code block* immediately following
the element with the content extracted from the referenced source file.
When the element has `start` and `end` attributes, the tool embeds only
the lines between the matching patterns. When it has only `file`, the
tool embeds the whole file.

This page describes the syntax used in the Validation documentation.
For the complete `embed-code-go` syntax — including named fragments,
multi-piece fragments, omitted boundaries, and the exact line-pattern
rules — see the upstream [embedding guide][embed-code-go-embedding].

````markdown
<embed—code
  file="$root/version.gradle.kts"
  start="val validationVersion"
  end="val validationVersion">
</embed—code>
```kotlin
val validationVersion by extra("2.0.0-SNAPSHOT.419")
```
````

The attributes used most often in Validation pages are:

- `file` — `$source-root/relative/path/to/file.ext` where `source-root`
  is a name from `code-path`.
- `start` — an extended glob-style pattern matched against a single line.
  The matching line is the *first* line included.
- `end` — an extended glob-style pattern matched against a single line.
  The matching line is the *last* line included. To include a single line,
  use the same pattern for both.
- The language tag on the fenced block (`kotlin`, `java`, `proto`, …)
  controls syntax highlighting; the tool does not derive it from the
  source extension.

Patterns match anywhere in a line by default. Use `^` at the beginning
or `$` at the end when the match must be anchored to the start or end of
the line.

Other working examples in the repository to copy from:

- A Java method body — see [`runtime-library.md`][runtime-library] for
  patterns like `start="public static <M extends Message> M check"` with
  `end="^    \}"`.
- A Kotlin class declaration — same page, the `DetectedViolation` snippet.
- A Gradle DSL block — see [`build-and-release.md`][build-and-release]
  for the `spinePublishing { … }` example, anchored with `end="^}"`.

## How embedding runs

The two Gradle tasks that drive the embedding flow are documented on
"[Build tasks](build-tasks.md)":

- `:docs:embedCode` rewrites the fenced regions in place. Use it after
  editing a `start`/`end` pattern, adding a new `<embed-code>` element,
  or pointing a source root at a new commit.
- `:docs:checkSamples` verifies that every fenced region matches what
  the tool would produce now, without writing. The
  [Code Embedding][check-code-embedding] workflow runs this on every
  pull request.

Both tasks shell out to the `embed-code-go` binaries checked in under
`docs/_bin/`. See "[External tooling](tooling.md)" for how those
binaries are kept up to date.

## What's next

- "[External tooling](tooling.md)" — the `site-commons` Hugo theme and
  the `embed-code-go` tool, including how each is updated.
- "[Procedures](procedures.md)" — the day-to-day recipes for adding a
  new embedded example and for refreshing the Spine Time submodule.

[hello-validation]: https://github.com/spine-examples/hello-validation
[spine-time]: https://github.com/SpineEventEngine/time
[embed-code-yml]: https://github.com/SpineEventEngine/validation/blob/master/docs/_settings/embed-code.yml
[embed-code-go-embedding]: https://github.com/SpineEventEngine/embed-code-go/blob/master/EMBEDDING.md
[runtime-library]: https://github.com/SpineEventEngine/validation/blob/master/docs/content/docs/validation/developer/runtime-library.md
[build-and-release]: https://github.com/SpineEventEngine/validation/blob/master/docs/content/docs/validation/developer/build-and-release.md
[check-code-embedding]: https://github.com/SpineEventEngine/validation/blob/master/.github/workflows/check-code-embedding.yml
