---
title: Concepts
description: Describes Spine Validation concepts.
headline: Documentation
---

# Concepts

This page introduces the core vocabulary and mechanics of Spine Validation.
It answers two questions:

- **How do I express validation rules?**
- **How does the runtime report violations?**

If you are new to the library, start with [declaring constraints](../01-getting-started/first-model)
in `.proto` files and then come back here for the details.


## Vocabulary

Spine Validation uses a small set of concepts consistently across code generation and runtime:

- **Constraint** — a validation rule declared in Protobuf using an option (for example,
  `(required)`, `(min)`, `(max)`, `(pattern)`, `(distinct)`, `(validate)`, `(set_once)`).
- **Option application** — a concrete place where an option is applied (a field, a message,
  a `oneof`, etc.).
- **Violation** — an instance of a constraint being broken. Violations are represented
  as `ConstraintViolation`.
- **Validation error** — a collection of violations. Validation results are represented
  as `ValidationError`.


## Where constraints live

Constraints are part of the model.
You declare them next to message fields in your `.proto` files by importing
`spine/options.proto`.

This makes validation rules:

- versioned together with the data model,
- shared between services that reuse the same Protobuf definitions,
- enforced automatically by generated code.


## How validation is executed

Validation is executed through **generated code** for your messages and builders.
The generated API exposes two main ways to validate data:

1. **Fail-fast validation on creation.**  
   If a constraint is violated, `build()` throws `ValidationException`.

2. **Validation of an existing instance.**  
   Generated messages implement `ValidatableMessage` and provide `validate()`, which returns
   `Optional<ValidationError>`.

See [Using the generated code](../01-getting-started/generated-code.md) for end-to-end examples.


## What a violation contains

Each `ConstraintViolation` points to the invalid value and explains what went wrong:

- `field_path` — the path to the invalid field (for example, `contacts.email.value`).
- `type_name` — the name of the validated root message type.
- `message` — a templated, machine-friendly error message (`TemplateString`).
- `field_value` — the invalid value packed as `google.protobuf.Any`.

When you need a human-readable message, format the `TemplateString` from the violation.
See [Working with error messages](working-with-error-messages.md) for message formatting,
placeholders, and customization.


## Nested validation and `(validate)`

Protobuf messages often contain other messages.
Spine Validation supports validating nested structures and reporting correct field paths.

When a nested message is validated as part of another message’s validation, a violation:

- keeps the **root** type in `type_name`, and
- reports the **full path** to the invalid field in `field_path`.

This allows you to surface errors as “`contacts.email.value` is invalid” while still knowing
which message was validated at the top level.


## Custom constraints

If built-in options are not enough, you can add organization-specific options and generate
code for them.

See [Custom validation](../08-custom-validation/) for the workflow and a reference example.


## What’s next

- Declare rules in your model:
  [Define constraints in `.proto` files](../01-getting-started/first-model.md).
- Learn how runtime validation behaves:
  [Using the generated code](../01-getting-started/generated-code.md).
- See how options influence generated code:
  [Options overview](options-overview.md).
 - Customize and format messages:
   [Working with error messages](working-with-error-messages.md).
- [Built-in options](../03-built-in-options/)
- [Validating third-party messages](../04-third-party-messages/)
 - Add custom validation options:
   [Custom validation](../08-custom-validation/).
 - Explore the implementation:
   [Architecture](../09-developers-guide/architecture.md).
