---
title: Build, packaging, and release
description: How the multi-project build is wired and how Spine Validation artifacts are produced.
headline: Documentation
---

# Build, packaging, and release

> This page is a placeholder. Content is in progress.

This section will describe how the multi-project build is wired and how artifacts are
produced: the Gradle multi-project layout and the relationship between `:java`,
`:java-bundle`, and `:gradle-plugin`; why `:java-bundle` exists (a fat JAR for compiler
plugin distribution); and the version flow from this repository to downstream
consumers of the Gradle plugin.
