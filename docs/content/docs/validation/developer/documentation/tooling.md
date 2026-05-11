---
title: External tooling
description: The external tools the documentation build depends on and how they are consumed.
headline: Documentation
---

# External tooling

The documentation build leans on three external pieces of tooling: the
[site-commons][site-commons] Hugo theme, the [embed-code-go][embed-code-go]
tool, and the Hugo + Node toolchain that drives the static-site build. This
page documents what each is, how it is wired into the build, and where it is
pinned. The step-by-step recipes for updating each live in
"[Procedures](procedures.md)".

## `site-commons`

[site-commons][site-commons] is the shared Hugo theme and layout library
used by the documentation sites of Spine projects. It provides the
navigation chrome, the sidenav rendering that consumes the
[`sidenav.yml`](../../../../data/docs/validation/2-0-0-snapshot/sidenav.yml)
files, code-highlighting partials, and other Hugo components that are
common across Spine sites.

It is consumed as a Hugo module. Two files wire it in:

- `docs/_preview/hugo.toml` imports `site-commons` and the local `../..`
  module (the `docs/` directory itself) under `[module] [[module.imports]]`:

  ```toml
  [module]
    [[module.imports]]
      path = '../..'
    [[module.imports]]
      path = 'github.com/SpineEventEngine/site-commons'
  ```

  Hugo modules sit on top of a Go module, so this import is resolved through
  `_preview/go.mod`.

- `docs/_preview/go.mod` pins the exact `site-commons` version, expressed as
  a pseudo-version timestamp:

  ```text
  require (
      github.com/SpineEventEngine/site-commons v0.0.0-20260507130158-84db050dfe11 // indirect
      github.com/gohugoio/hugo-mod-bootstrap-scss/v5 v5.20300.20800 // indirect
  )
  ```

The `hugo-mod-bootstrap-scss` dependency is a transitive Hugo module pulled
in by `site-commons`; it is listed here because Hugo resolves the full
module graph through this `go.mod`.

The repository-root `docs/go.mod` is a separate file for tooling that
inspects the `docs/` directory as a Go module on its own. The
*authoritative* pin for what gets rendered locally is the one in
`_preview/go.mod`.

### Updating `site-commons`

The procedure is the one documented under "[Theme updates][theme-updates]"
in the `site-commons` README. From `docs/_preview/`, run:

```bash
hugo mod get -u github.com/SpineEventEngine/site-commons
```

Then commit and push the resulting changes to `go.mod` and `go.sum`. The
companion "Updating `site-commons`" recipe in
"[Procedures](procedures.md)" wraps this command with the surrounding
verification steps (preview, sanity check).

## `embed-code-go`

[embed-code-go][embed-code-go] is the command-line tool that powers
`:docs:embedCode` and `:docs:checkSamples`. It reads the
`<embed-code>` elements described in "[Embedded examples](embedded-examples.md)",
resolves each `file` reference against the source roots in
[`_settings/embed-code.yml`](../../../../_settings/embed-code.yml), and
either rewrites the fenced code block beneath the element (`-mode="embed"`)
or fails non-zero on drift (`-mode="check"`).

The tool is consumed as **prebuilt binaries checked into the repository**,
not as a Go module or downloaded artifact. The binaries live under
`docs/_bin/`:

| File                       | When it is used                                              |
|----------------------------|--------------------------------------------------------------|
| `embed-code-linux`         | `_script/check-samples` when `GITHUB_ACTIONS=true` (CI).     |
| `embed-code-macos`         | `_script/check-samples` locally; `_script/embed-code` always. |
| `embed-code-windows.exe`   | Currently not selected by either script.                     |

There is no version string recorded in this repository. Updating to a new
release of `embed-code-go` means rebuilding the binaries from its source
repository for each target platform and replacing the artifacts under
`docs/_bin/`. The step-by-step recipe is "Updating `embed-code-go`" in
"[Procedures](procedures.md)".

## Hugo and the Node toolchain

The `:docs:installDependencies` task runs `npm install` inside `_preview/`
to install the Node packages that Hugo's asset pipeline consumes. The
relevant declarations are in `docs/_preview/package.json`:

```json
"devDependencies": {
  "@fullhuman/postcss-purgecss": "^7.0.2",
  "autoprefixer": "^10.4.22",
  "postcss": "^8.5.10",
  "postcss-cli": "^11.0.1",
  "postcss-discard-comments": "^7.0.5"
}
```

These power the PostCSS processing chain that Hugo invokes for CSS assets
(autoprefixing, purging unused rules from the Bootstrap-based theme).
`package-lock.json` is committed and is the authoritative pin.

Hugo itself is *not* installed by the build — the `_script/hugo-serve` and
`_script/hugo-build` scripts assume that `hugo` is on the `PATH`. Install it
from the [Hugo project][hugo-install] before running `:docs:runSite` or
`:docs:buildSite` for the first time. The version is not pinned in this
repository; use a recent extended release that matches what `site-commons`
expects.

## What's next

- "[Procedures](procedures.md)" — the recipes for updating `site-commons`
  and `embed-code-go`, and for installing or upgrading the local Hugo and
  Node toolchain.

[site-commons]: https://github.com/SpineEventEngine/site-commons
[theme-updates]: https://github.com/SpineEventEngine/site-commons#theme-updates
[embed-code-go]: https://github.com/SpineEventEngine/embed-code-go
[hugo-install]: https://gohugo.io/installation/
