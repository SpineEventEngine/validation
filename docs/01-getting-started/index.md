# Getting started

This section helps you set up Spine Validation, define your first validated Protobuf model,
and see validation in action in Java and Kotlin.

If you are new to the library, read the short overview first:
- Introduction → [Overview](../00-intro/index.md)
- Who this is for → [Target audience](../00-intro/target-audience.md)
- Design principles → [Philosophy](../00-intro/philosophy.md)


## What you’ll learn

- How to add Spine Validation to a JVM project using Protobuf models.
- How to declare validation rules in `.proto` files.
- How validation is enforced at build time and at runtime.

## Prerequisites

- Java 17+
- Gradle (Kotlin DSL or Groovy)
- Protobuf compiler (`protoc`)
- Optional: Kotlin 2.2.21+ for the Kotlin Protobuf DSL

If your project already generates Java/Kotlin sources from `.proto` files, you’re 90% there.
Spine Validation integrates into the build to generate and inject validation logic into
the code produced by `protoc`.


## Quick path

1) Add Spine Validation to your Gradle build
- Follow the [instructions](adding-to-build.md).

2) Define constraints in your `.proto` files
- Import `spine/options.proto` (and `spine/time_options.proto` for temporal constraints).
- Add declarative options like `(required)`, `(min)`, `(pattern)`, `(when).in = PAST`.

3) Build your project
- The Validation plugin to Spine Compiler scans your model and generates validation code.

4) Use the generated API
- Validate on builder `build()` or call `validate()` explicitly.
  See [Your first validated model](first-model.md).


## What’s next

- [Adding Validation to your build](adding-to-build.md)
- [Your First Validated Model](first-model.md)
- [Validation Workflow](workflow.md)
