# Spine Validation — Table of contents

## 0. Introduction
- [Overview](00-intro/index.md)
- [Target audience](00-intro/target-audience.md)
- [Philosophy](00-intro/philosophy.md)

## 1. Getting started
- [Getting started](01-getting-started/index.md)
- [Installation](01-getting-started/installation.md)
- [Your first validated model](01-getting-started/first-model.md)
- [Validation workflow](01-getting-started/workflow.md)

## 2. Concepts
- [Concepts overview](02-concepts/index.md)
- [Validation options overview](02-concepts/options-overview.md)
- [The validation engine](02-concepts/validation-engine.md)
- [Integration with Protobuf & Spine Compiler](02-concepts/protobuf-integration.md)

## 3. Built-in validation options
- [Built-in options overview](03-built-in-options/index.md)

### 3.1. Field constraints
- [Overview](03-built-in-options/fields/index.md)
- [required](03-built-in-options/fields/required.md)
- [pattern](03-built-in-options/fields/pattern.md)
- [min/max](03-built-in-options/fields/min-max.md)
- [range](03-built-in-options/fields/range.md)
- [length/size](03-built-in-options/fields/length-size.md)
- [unique](03-built-in-options/fields/unique.md)

### 3.2. Collection constraints
- [Overview](03-built-in-options/collections/index.md)
- [non_empty](03-built-in-options/collections/non-empty.md)
- [distinct](03-built-in-options/collections/distinct.md)
- [Collection size](03-built-in-options/collections/collection-size.md)

### 3.3. String constraints
- [Overview](03-built-in-options/strings/index.md)
- [Advanced patterns](03-built-in-options/strings/pattern-advanced.md)

### 3.4. Numeric constraints
- [Overview](03-built-in-options/numbers/index.md)
- [Numeric bounds](03-built-in-options/numbers/numeric-bounds.md)

### 3.5. Temporal constraints
- [Overview](03-built-in-options/temporal/index.md)
- [`(when)`](03-built-in-options/temporal/when.md)
- [`Timestamp` & `Duration`](03-built-in-options/temporal/timestamp-duration.md)

### 3.6. Message-level constraints
- [Overview](03-built-in-options/message/index.md)
- [required_for](03-built-in-options/message/required-for.md)
- [Nested validation](03-built-in-options/message/nested-validation.md)
- [Cross-field validation](03-built-in-options/message/cross-field.md)

## 4. Using validation in code
- [Overview](04-using-validation/index.md)
- [Validating messages](04-using-validation/validating-messages.md)
- [Handling errors](04-using-validation/handling-errors.md)
- [Kotlin usage](04-using-validation/kotlin-usage.md)
- [Framework integration](04-using-validation/framework-integration.md)

## 5. Configuration & tooling
- [Overview](05-configuration/index.md)
- [Compiler configuration](05-configuration/compiler.md)
- [Library modules](05-configuration/modules.md)
- [Debugging generated code](05-configuration/debugging.md)

## 6. Extending validation
- [Overview](06-extending/index.md)
- [Architecture](06-extending/architecture.md)
- [Custom validation options](06-extending/custom-options.md)
- [Custom runtime validators](06-extending/custom-runtime-validators.md)

## 7. Recipes (cookbook)
- [Overview](07-recipes/index.md)
- [Domain IDs](07-recipes/domain-ids.md)
- [Common cases](07-recipes/common-cases.md)
- [Temporal logic](07-recipes/temporal-logic.md)
- [Cross-field logic](07-recipes/cross-field-logic.md)
- [API validation](07-recipes/api-validation.md)

## 8. Reference
- [Reference overview](08-reference/index.md)
- [List of validation options](08-reference/options.md)
- [Java/Kotlin API index](08-reference/api.md)
- [Glossary](08-reference/glossary.md)
