# Spine Validation — Table of Contents

## 0. Introduction
- [Overview](00-intro/index.md)
- [Target Audience](00-intro/target-audience.md)
- [Philosophy](00-intro/philosophy.md)

## 1. Getting Started
- [Getting Started](01-getting-started/index.md)
- [Installation](01-getting-started/installation.md)
- [Your First Validated Model](01-getting-started/first-model.md)
- [Validation Workflow](01-getting-started/workflow.md)

## 2. Concepts
- [Concepts Overview](02-concepts/index.md)
- [Validation Options Overview](02-concepts/options-overview.md)
- [The Validation Engine](02-concepts/validation-engine.md)
- [Integration with Protobuf & Compiler](02-concepts/protobuf-integration.md)

## 3. Built-in Validation Options
- [Built-in Options Overview](03-built-in-options/index.md)

### 3.1. Field Constraints
- [Overview](03-built-in-options/fields/index.md)
- [required](03-built-in-options/fields/required.md)
- [pattern](03-built-in-options/fields/pattern.md)
- [min/max](03-built-in-options/fields/min-max.md)
- [range](03-built-in-options/fields/range.md)
- [length/size](03-built-in-options/fields/length-size.md)
- [unique](03-built-in-options/fields/unique.md)

### 3.2. Collection Constraints
- [Overview](03-built-in-options/collections/index.md)
- [non_empty](03-built-in-options/collections/non-empty.md)
- [distinct](03-built-in-options/collections/distinct.md)
- [collection size](03-built-in-options/collections/collection-size.md)

### 3.3. String Constraints
- [Overview](03-built-in-options/strings/index.md)
- [Advanced Patterns](03-built-in-options/strings/pattern-advanced.md)
- [email](03-built-in-options/strings/email.md)
- [hostname](03-built-in-options/strings/hostname.md)
- [uri](03-built-in-options/strings/uri.md)

### 3.4. Numeric Constraints
- [Overview](03-built-in-options/numbers/index.md)
- [Numeric Bounds](03-built-in-options/numbers/numeric-bounds.md)

### 3.5. Temporal Constraints
- [Overview](03-built-in-options/temporal/index.md)
- [when](03-built-in-options/temporal/when.md)
- [Timestamp & Duration](03-built-in-options/temporal/timestamp-duration.md)

### 3.6. Message-level Constraints
- [Overview](03-built-in-options/message/index.md)
- [required_for](03-built-in-options/message/required-for.md)
- [Nested Validation](03-built-in-options/message/nested-validation.md)
- [Cross-field Validation](03-built-in-options/message/cross-field.md)

## 4. Using Validation in Code
- [Overview](04-using-validation/index.md)
- [Validating Messages](04-using-validation/validating-messages.md)
- [Handling Errors](04-using-validation/handling-errors.md)
- [Kotlin Usage](04-using-validation/kotlin-usage.md)
- [Framework Integration](04-using-validation/framework-integration.md)

## 5. Configuration & Tooling
- [Overview](05-configuration/index.md)
- [Compiler Configuration](05-configuration/compiler.md)
- [Library Modules](05-configuration/modules.md)
- [Debugging Generated Code](05-configuration/debugging.md)

## 6. Extending Validation
- [Overview](06-extending/index.md)
- [Architecture](06-extending/architecture.md)
- [Custom Validation Options](06-extending/custom-options.md)
- [Custom Runtime Validators](06-extending/custom-runtime-validators.md)

## 7. Recipes (Cookbook)
- [Overview](07-recipes/index.md)
- [Domain IDs](07-recipes/domain-ids.md)
- [Common Cases](07-recipes/common-cases.md)
- [Temporal Logic](07-recipes/temporal-logic.md)
- [Cross-field Logic](07-recipes/cross-field-logic.md)
- [API Validation](07-recipes/api-validation.md)

## 8. Migration Guide
- [Overview](08-migration/index.md)
- [Migrating from `spine.base` Validation](08-migration/from-spine-base.md)
- [Version Changes](08-migration/version-changes.md)

## 9. Reference
- [Reference Overview](09-reference/index.md)
- [List of Validation Options](09-reference/options.md)
- [Java/Kotlin API Index](09-reference/api.md)
- [Glossary](09-reference/glossary.md)
