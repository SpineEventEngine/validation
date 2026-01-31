# Structuring repositories for Hugo documentation using Go modules

This document describes the **recommended repository layout and Hugo
Module mount strategy** for building modular documentation where:

-   The **main site** assembles documentation from multiple repositories
-   Each framework/library keeps **all documentation isolated under
    `docs/`**
-   Hugo Modules (Go modules) are used for versioning and composition

------------------------------------------------------------------------

## High-level architecture

-   **Main site repository**
    -   Hugo site that assembles all documentation
    -   Imports documentation modules only
-   **Documentation provider repositories** (framework, libraries)
    -   Each repo exposes its documentation as a **Hugo Module**
    -   All doc-related files live under `docs/`

Each `docs/` directory is:
 - a **Go module root** (`go.mod`)
 - a **Hugo project root** (`hugo.yaml`)

------------------------------------------------------------------------

## Main site repository

    main-site/
      go.mod
      hugo.yaml
      content/
      layouts/
      assets/

### hugo.yaml

``` yaml
module:
  imports:
    - path: github.com/your-org/framework/docs
    - path: github.com/your-org/lib-foo/docs
    - path: github.com/your-org/lib-bar/docs
```

------------------------------------------------------------------------

## Framework documentation repository

    framework/
      docs/
        go.mod
        go.sum
        hugo.yaml
        content/
          framework/
            _index.md
        layouts/
        assets/

### docs/go.mod

``` go
module github.com/your-org/framework/docs
go 1.22
```

### docs/hugo.yaml

``` yaml
module:
  mounts:
    - source: content
      target: content/framework
    - source: layouts
      target: layouts
    - source: assets
      target: assets
```

------------------------------------------------------------------------

## Library documentation repository (example: lib-foo)

    lib-foo/
      docs/
        go.mod
        hugo.yaml
        content/
          libraries/
            foo/
              _index.md

### docs/go.mod

``` go
module github.com/your-org/lib-foo/docs
go 1.22
```

### docs/hugo.yaml

``` yaml
module:
  mounts:
    - source: content
      target: content/libraries/foo
    - source: layouts
      target: layouts
    - source: assets
      target: assets
```

------------------------------------------------------------------------

## Content conventions
 *	Always mount into namespaced sections:
 *	`content/framework/...`
 *	`content/libraries/<libname>/...`
 *	Use `_index.md` at section roots to create proper landing pages
 *	Avoid mounting directly to `content/` without a subdirectory

------------------------------------------------------------------------

## Optional: shared documentation UI module

For shared shortcodes, partials, and styling:

    docs-ui/
    go.mod
    hugo.yaml
    layouts/
    shortcodes/
    partials/
    assets/

**docs-ui/hugo.yaml:**

```yaml
module:
  mounts:
  - source: layouts
    target: layouts
  - source: assets
    target: assets
```
Import this module before content modules in the main site.

------------------------------------------------------------------------

## Local development

Run documentation standalone from a repo:

    cd framework
    hugo server --source docs

For multi-repo development, consider a go.work file to use local checkouts instead of remote module versions.

------------------------------------------------------------------------

## Key rules to follow
 * docs/ is the only Hugo + Go module root in doc-providing repos
 * Each docs module defines its own mounts
 * The main site only imports modules
 * Mount targets must be unique and namespaced
 * Commit both go.mod and go.sum for reproducible builds

------------------------------------------------------------------------

## References
 * Hugo Modules overview: https://gohugo.io/hugo-modules/
 * Hugo module configuration: https://gohugo.io/configuration/module/
 * Go modules reference: https://go.dev/ref/mod
