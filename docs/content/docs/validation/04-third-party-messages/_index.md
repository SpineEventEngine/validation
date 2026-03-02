---
title: Validating third-party messages
description: How to validate message types generated outside your build.
headline: Documentation
---

# Validating third-party messages

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
- **External (third-party) messages** — message classes are already compiled (come from
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

To validate an external message type `M`:

1. Implement `io.spine.validation.MessageValidator<M>`.
2. Annotate the implementation with `@io.spine.validation.Validator(M::class)`.
3. Ensure the class has a `public`, no-args constructor.

For Kotlin, you can implement a validator using the `Timestamps.MIN_VALUE` and
`Timestamps.MAX_VALUE` static fields from the Protobuf Util library:

<embed-code file="../../../../jvm-runtime/src/main/kotlin/io/spine/validation/TimestampValidator.kt" fragment="core"></embed-code>
```kotlin
@Validator(Timestamp::class)
public class TimestampValidator : MessageValidator<Timestamp> {

    override fun validate(message: Timestamp): List<DetectedViolation> {
        val violations = mutableListOf<DetectedViolation>()
        val minSeconds = Timestamps.MIN_VALUE.seconds
        val maxSeconds = Timestamps.MAX_VALUE.seconds
        if (message.seconds !in minSeconds..maxSeconds) {
            violations.add(
                FieldViolation(
                    message = templateString {
                        withPlaceholders =
                            "The `seconds` value is out of range ($minSeconds..$maxSeconds):" +
                                " ${message.seconds}."
                    },
                    fieldPath = fieldPath { fieldName.add("seconds") },
                    fieldValue = message.seconds
                )
            )
        }
        val maxNanos = Timestamps.MAX_VALUE.nanos
        if (message.nanos !in 0..maxNanos) {
            violations.add(
                FieldViolation(
                    message = templateString {
                        withPlaceholders =
                            "The `nanos` value is out of range (0..$maxNanos): ${message.nanos}."
                    },
                    fieldPath = fieldPath { fieldName.add("nanos") },
                    fieldValue = message.nanos
                )
            )
        }
        return violations
    }
}
```

Spine Validation creates a new validator instance per invocation, so keep validators stateless and
cheap to construct.


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


## Walkthrough: validate `google.protobuf.Timestamp` inside a local message

Suppose your local model uses `Timestamp`:

```proto
import "google/protobuf/timestamp.proto";

message Meeting {
    google.protobuf.Timestamp starts_at = 1;
}
```

If you add `TimestampValidator` (as shown above) in the same module and then create an invalid
timestamp, validation of `Meeting` reports a violation for the `starts_at` field.

If you need the violation to point deeper (for example, to `starts_at.seconds`), return
`FieldViolation` with a `fieldPath` inside `Timestamp` instead of `MessageViolation`.


## What’s next

- [Custom validation](../08-custom-validation/)
- [Architecture](../09-developers-guide/architecture.md)
