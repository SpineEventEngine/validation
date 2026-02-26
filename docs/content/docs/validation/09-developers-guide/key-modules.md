---
title: Key Modules
description: Overview of the main modules in the Spine Validation project.
headline: Documentation
---

# Key modules

This repository is a Gradle multi-project build. Module names below are shown as Gradle
project paths (e.g. `:java`, `:tests:vanilla`).

## Core modules

| **Module**       | **Description**                                                                                                                                                |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `:context`       | Language-agnostic validation model and built-in option handling (views/reactions) shared by language plugins.                                                  |
| `:java`          | Spine Compiler plugin for Java: generates/injects validation code; loads custom options via `ValidationOption` SPI and custom validators discovered by `:ksp`. |
| `:ksp`           | KSP processor that discovers classes annotated with `@io.spine.validation.Validator` and writes a message→validator mapping resource consumed by `:java`.      |
| `:jvm-runtime`   | Runtime library used by generated code: `ValidationException`, validation/constraint APIs, `MessageValidator`, and error Protobuf types.                       |
| `:java-bundle`   | Fat JAR bundling `:java` for distribution (the artifact typically used as the compiler plugin dependency).                                                     |
| `:gradle-plugin` | Gradle plugin (`io.spine.validation`) that configures Spine Compiler to run the Validation compiler for consumer projects.                                     |
| `:docs`          | Documentation site (Hugo) sources, scripts, and example projects used in docs.                                                                                 |

## Test modules

| **Module**                    | **Description**                                                                                                            |
|-------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `:context-tests`              | Compilation tests for `:context` (Prototap-based), focusing on invalid option usage and error reporting.                   |
| `:tests`                      | Parent project for integration tests that run the compiler plugins and exercise generated code.                            |
| `:tests:vanilla`              | “Vanilla” integration tests: validation without any custom extensions.                                                     |
| `:tests:extensions`           | Example custom option (`(currency)`) implementation used by test suites (custom reactions/views/generator).                |
| `:tests:consumer`             | Integration tests for a consuming project that uses validation plus custom extensions.                                     |
| `:tests:consumer-dependency`  | A dependency module with `.proto` sources used by `:tests:consumer` to verify “protos in dependencies” scenarios.          |
| `:tests:validator`            | Integration tests for custom `MessageValidator`s annotated with `@Validator` (covers `:ksp` discovery and `:java` wiring). |
| `:tests:validator-dependency` | A dependency module used by `:tests:validator` for validator-related dependency scenarios.                                 |
| `:tests:runtime`              | Tests focused on runtime behavior of validation APIs and error messages.                                                   |
| `:tests:validating`           | Shared fixtures and tests for validation behavior across multiple scenarios (includes `testFixtures`).                     |

## What’s next

- Build custom validation rules: [Custom validation](../08-custom-validation/).
