# Getting started

This section helps you set up Spine Validation, define your first validated Protobuf model,
and see validation in action in Java and Kotlin.

If you are new to the library, read the short overview first:
- Introduction → [Overview](../00-intro/index.md)
- Who this is for → [Target Audience](../00-intro/target-audience.md)
- Design principles → [Philosophy](../00-intro/philosophy.md)


## What you’ll learn

- How to add Spine Validation to a JVM project using Protobuf models.
- How to declare validation rules in `.proto` files.
- How validation is enforced at build time and at runtime.

## Prerequisites

- Java 17+
- Gradle (Kotlin DSL or Groovy)
- Protobuf compiler (`protoc`)
- Optional: Kotlin 2.2.20+ for the Kotlin Protobuf DSL

If your project already generates Java/Kotlin sources from `.proto` files, you’re 90% there.
Spine Validation integrates into the build to generate and inject validation logic into
the code produced by `protoc`.


## Quick path

1) Install the library and compiler integration
- Follow [Installation](installation.md) to add the necessary repositories and build configuration.
- If you use a custom Protobuf setup, see [Compiler Configuration](../05-configuration/compiler.md).

2) Define constraints in your `.proto` files
- Import `spine/options.proto` (and `spine/time_options.proto` for temporal constraints).
- Add declarative options like `(required)`, `(min)`, `(pattern)`, `(when).in = PAST`.

3) Build your project
- The Spine compiler plugin scans your model and generates validation code.

4) Use the generated API
- Validate on builder `build()` or call `validate()` explicitly. See [Your First Validated Model](first-model.md).


## What’s next

- [Installation](installation.md)
- [Your First Validated Model](first-model.md)
- [Validation Workflow](workflow.md)
