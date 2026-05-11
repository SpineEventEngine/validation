---
title: Embedded examples
description: How example source code is embedded into the documentation pages.
headline: Documentation
---

# Embedded examples

<!-- TODO: Explain the role of code embedding: keeping snippets in the
documentation in lock-step with real, compilable sources. -->

## Example sources

<!-- TODO: Describe `_examples/` as the Git submodule pointing to
`spine-examples/hello-validation`, what example projects it provides
(`first-model`, `first-model-with-framework`, `external`), and how those
projects participate in the build via `:docs:buildExamples`. -->

## Spine Time as an implementation example

<!-- TODO: Explain why the Spine Time library is wired in as `_time/`
(submodule pointing to `SpineEventEngine/time`): it provides a real-world
implementation example for custom validation. -->

## The `settings/embed-code.yml` file

<!-- TODO: Walk through the configuration:
- `code-path`: the five source roots (`root`, `examples`, `runtime`,
  `java`, `context`) and what each exposes.
- `docs-path`: where pages that can host embedded snippets live.
- `code-includes`: the file globs the tool considers as embeddable sources.
-->

## Embedding syntax on the page side

<!-- TODO: Show the Markdown syntax used to reference a snippet from a
source file, with a small worked example. -->

## How embedding runs

<!-- TODO: Explain the `:docs:embedCode` and `:docs:checkSamples` flow and
what the prebuilt `embed-code-*` binaries under `_bin/` do at runtime. -->
