---
title: Getting started
description: Step-by-step guide to setting up Spine Validation in your project.
headline: Documentation
---

# Getting started

This section helps you set up Spine Validation, define your first validated Protobuf model,
and see validation in action in Java and Kotlin.

If you are new to the library, read the short overview first:
- Introduction → [Overview](../00-intro/)
- Who this is for → [Target audience](../00-intro/target-audience.md)
- Design principles → [Philosophy](../00-intro/philosophy.md)


## What you’ll learn

- How to add Spine Validation to a JVM project using Gradle plugins.
- How to declare validation rules in `.proto` files.
- How validation is enforced at runtime.

## Prerequisites

- Java 17+
- Gradle (Kotlin DSL or Groovy)
- Optional: Kotlin 2.2.21+ for the Kotlin Protobuf DSL

If your project already generates Java/Kotlin sources from `.proto` files, you’re 90% there.
Spine Validation integrates into the build to generate and inject validation logic into
the code produced by `protoc`.

Code samples in this tutorial use [Google Truth](https://truth.dev/) and
[Kotest Assertions](https://kotest.io/docs/assertions/assertions.html).

## Quick path

 1) [Add Validation to your build](adding-to-build.md)
 2) [Define constraints in `.proto` files](first-model.md)
 3) [Build your project](build-project.md)
 4) [Use the generated validation API](generated-code.md)

## What’s next

- [Adding Validation to your build](adding-to-build.md)
