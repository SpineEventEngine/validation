---
title: External tooling
description: The external tools the documentation build depends on and how they are consumed.
headline: Documentation
---

# External tooling

<!-- TODO: Brief overview of the third-party tools that the docs build pulls
in and how each is wired. -->

## `site-commons`

<!-- TODO: What `site-commons` is (shared Hugo theme/layout for Spine sites),
where it lives (`https://github.com/SpineEventEngine/site-commons`), and how
it is consumed:
- imported as a Hugo module in `_preview/hugo.toml`
- pinned in `_preview/go.mod`
Reference the procedure on the Procedures page for updating it. -->

## `embed-code-go`

<!-- TODO: What the tool does (parses fenced regions in Markdown and replaces
them with current source content), where it lives
(`https://github.com/SpineEventEngine/embed-code-go`), and how it is consumed
in this repo:
- prebuilt binaries checked in under `docs/_bin/`
- selected per-OS by `_script/embed-code` and `_script/check-samples`
Reference the procedure on the Procedures page for updating the binaries.
-->

## Hugo and the Node toolchain

<!-- TODO: Note the Hugo version expectation, the `npm install` step driven
by `:docs:installDependencies`, and the `_preview/package.json` devDependency
set (`postcss`, `autoprefixer`, `@fullhuman/postcss-purgecss`, etc.). -->
