---
title: Key Modules
description: Overview of the main modules in the Spine Validation project.
headline: Documentation
---

# Key modules

This repository is a Gradle multi-project build. Module names below are shown as Gradle
project paths like `:java` and `:tests:vanilla`.

## Core modules

- **`:context`**: Language-agnostic validation model and built-in option handling for views and reactions shared by language plugins.
- **`:java`**: Spine Compiler plugin for Java: generates/injects validation code; loads custom options via `ValidationOption` SPI.
- **`:jvm-runtime`**: Runtime library used by generated code: `ValidationException`, validation/constraint APIs, `MessageValidator`, and error Protobuf types.
- **`:java-bundle`**: Fat JAR bundling `:java` for distribution, which is the artifact typically used as the compiler plugin dependency.
- **`:gradle-plugin`**: The `io.spine.validation` Gradle plugin that configures Spine Compiler to run the Validation compiler for consumer projects.
- **`:docs`**: Sources for the Hugo documentation site, scripts, and example projects used in docs.

## Test modules

- **`:context-tests`**: ProtoTap-based compilation tests for `:context`, focusing on invalid option usage and error reporting.
- **`:tests`**: Parent project for integration tests that run the compiler plugins and exercise generated code.
- **`:tests:vanilla`**: “Vanilla” integration tests: validation without any custom extensions.
- **`:tests:extensions`**: Example implementation of the `(currency)` custom option used by test suites to verify custom reactions, views, and generators.
- **`:tests:consumer`**: Integration tests for a consuming project that uses validation plus custom extensions.
- **`:tests:consumer-dependency`**: A dependency module with `.proto` sources used by `:tests:consumer` to verify “protos in dependencies” scenarios.
- **`:tests:validator`**: Integration tests for custom `MessageValidator`s discovered through `ServiceLoader`.
- **`:tests:validator-dependency`**: A dependency module used by `:tests:validator` for validator-related dependency scenarios.
- **`:tests:runtime`**: Tests focused on runtime behavior of validation APIs and error messages.
- **`:tests:validating`**: Shared fixtures and tests for validation behavior across multiple scenarios, including `testFixtures`.
