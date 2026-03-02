---
title: Validating external messages
description: How to validate message types generated outside your build.
headline: Documentation
---

# Validating external messages

Sometimes your model includes Protobuf messages that are **not generated in your build**.
For example, `google.protobuf.Timestamp` or a message type that comes as a part 
of the Protobuf library.

Because Spine Validation enforces most constraints through **code generation**, you cannot attach
Validation options to such messages unless you rebuild their `.proto` sources.

This page explains how to validate those message types using **external message validators**.


## Local vs external messages

Spine Validation deals with two kinds of message types:

- **Local messages** — `.proto` sources are compiled in the current build, so Validation can inject
  checks into the generated code.
- **External messages** — message classes are already compiled (come from
  dependencies), so Validation cannot apply code generation to them.


## What works (and what does not)

**If you control the `.proto` sources**, prefer declaring constraints in the model:

- Use [built-in options](../03-built-in-options/).
- If built-ins are not enough, implement [custom validation options](../08-custom-validation/).

**If you do not control the `.proto` sources**, options are not an option:

- You cannot attach Validation options to fields of an external message unless you rebuild its
  `.proto`.
- Instead, you can declare a `MessageValidator<M>` for the external message type `M`.

**Important limitations**

- An external validator is applied only when an external message is used as a field in a
  **local** message.
- External validators are not applied transitively inside other external messages.
- A standalone external message instance (validated “by itself”) is not validated via
  `MessageValidator`.


## Choose a strategy

Use this rule of thumb:

- You can rebuild/own the `.proto` → use Validation options and codegen.
- You cannot rebuild/own the `.proto` → use `MessageValidator` + `@Validator`.


## Implement an external validator

This topic is detailed on a separate page:
[Implement an external validator](implement-an-external-validator.md)
which reviews the details of the `TimestampValidator` implementation.

## When external validators are invoked

Once a validator is declared for `M`, Spine Validation generates code that invokes it for fields
of type `M` in local messages, including:

- singular fields of type `M`;
- `repeated` fields of type `M`;
- `map<..., M>` values.

No `.proto` options are required on the field: the validator is applied automatically anywhere the
external message type appears in a local model.

## Reporting violations

Your validator returns `List<DetectedViolation>`.
Detected violations are converted by the generated code into regular Spine Validation
`ConstraintViolation`s, which then become part of a `ValidationError`.

Use the provided violation types:

- `FieldViolation` — points to a field within the external message.
- `MessageViolation` — describes a message-level issue (not tied to a specific field).

If you return a `FieldViolation`, the generated code resolves its `fieldPath` against the parent
field name in the local message.
This lets you report a nested path like `meeting.starts_at.seconds`, even though the validator sees
only `Timestamp`.

## Guardrails and common errors

- **Exactly one validator per message type.**  
  Declaring multiple `@Validator`s for the same message type is an error.
- **Validators are for external messages only.**  
  Declaring a validator for a local message type is prohibited — use built-in options or custom
  options instead.
- **No `inner` classes.**  
  The validator class cannot be `inner` (nested classes are OK).
- **Types must match.**  
  The message type in `@Validator(...)` must match the type argument of `MessageValidator<M>`.

## What’s next
- [Implement an external validator](implement-an-external-validator.md)
- [Custom validation](../08-custom-validation/)
- [Architecture](../09-developers-guide/architecture.md)
