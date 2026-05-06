---
title: Overview and audience
description: Deep dive into Spine Validation architecture and internals.
headline: Documentation
---

# Overview and audience

This guide is for contributors to Spine Validation and for readers who want a deep
understanding of how the library works internally. It complements the User's Guide,
which targets consumers of the library: where the User's Guide shows *how to use*
Validation, this guide shows *how Validation is built*.

## Who this guide is for

Read this guide if you are:

- contributing changes to the Validation library itself,
- adding a new built-in validation option,
- working on a fork or a downstream tool that integrates with Validation's internals,
- or simply curious about how the pieces fit together.

If you only want to apply validation rules to your own `.proto` files, the
[User's Guide](../00-intro/) is the right starting point. The two guides are designed to
be read independently: this one assumes you have already met Validation as a *user*.

## The mental model

Spine Validation has a single, load-bearing architectural decision: constraints declared
in `.proto` files are *compiled*, not *interpreted*. There is no rule engine running at
message construction time. Two stages cooperate:

1. **At build time** — a Spine Compiler plugin reads validation options from your
   Protobuf model, builds a language-agnostic representation of the constraints, and
   emits Java code that enforces them. This work happens once, during the consumer
   project's build.

2. **At runtime** — generated code calls into a small library (`:jvm-runtime`) that
   provides the validator entry points, the violation Protobuf types, and the exception
   raised on failure. No reflection, no interpretation.

Almost everything in this guide is, ultimately, an explanation of one half of that split
or of the seam between them. Holding the picture in mind makes the rest of the guide
easier to navigate; the [Architecture](architecture.md) page expands it in detail.

## Prerequisite knowledge

This guide assumes working familiarity with:

- **Protocol Buffers** — message definitions, custom options, descriptors.
- **Gradle** — multi-project builds, plugin application, configuration phases.
- **Spine Compiler** — its role as a `protoc` post-processor and its plugin model.
  See the [Spine Compiler documentation][spine-compiler] for an introduction.
- **Spine Bounded Contexts** — events, views (projections), and reactions. The
  validation model in `:context` is itself a Bounded Context, and several sections
  describe it in those terms.

You do not need to be an expert in any of these, but you should not be meeting them for
the first time here.

## How to read this guide

The pages are arranged so that earlier sections introduce vocabulary used by later ones.
Read [Architecture](architecture.md) first; the rest can be consulted in order or by
need.

1. [Architecture](architecture.md) — the compile-time/runtime split and the modules
   that implement it.
2. [Key modules](key-modules.md) — one-line descriptions of every module in the
   repository, including the test modules. Use it as a reference.
3. [The validation model](validation-model.md) — the language-agnostic model in
   `:context`: views, events, reactions, and the `ValidationOption` SPI from the model side.
4. [Java code generation](java-code-generation.md) — how the Spine Compiler plugin in
   `:java` turns the model into Java validators.
5. [Runtime library](runtime-library.md) — `MessageValidator`, `ValidationException`,
   the violation Protobuf types, and runtime hooks in `:jvm-runtime`.
6. [Extension points](extension-points.md) — `MessageValidator`, the validator
   registry, and the `ValidationOption` SPI end-to-end.
7. [Adding a new built-in validation option](adding-a-built-in-option.md) — the
   contributor walkthrough that exercises the model, codegen, and runtime sections.
8. [Testing strategy](testing-strategy.md) — a map of the test modules and how to
   choose the right one for a new test.
9. [Build, packaging, and release](build-and-release.md) — the multi-project build,
   `:java-bundle`, and the Gradle plugin distribution flow.

Each section assumes the architecture page has already been read. Sections 3–6 can be
read in order for a top-down tour of the internals, or jumped into directly when
diagnosing a specific area.

[spine-compiler]: https://github.com/SpineEventEngine/compiler/
