---
title: Implement a validator
description: How to validate messages with custom logic using `MessageValidator`.
headline: Documentation
---

# Implement a validator

To validate a Protobuf message type `M` with custom logic:

1. Implement `io.spine.validation.MessageValidator<M>`.
2. Make the implementation discoverable via Java `ServiceLoader` (recommended), or register it in
   `ValidatorRegistry` explicitly.
3. Ensure the class has a public no-args constructor.

Keep validators stateless and cheap to construct.

## Reference implementation: `TimestampValidator`

Let's review the `MessageValidator` implementation on the example of
`io.spine.validation.TimestampValidator` from the Validation JVM runtime.

It validates `com.google.protobuf.Timestamp` and reports violations for invalid
`seconds` and `nanos` values.

### Service discovery

The validator is a regular `MessageValidator<Timestamp>` implementation and is discoverable via
`ServiceLoader`.
To generate the required service provider configuration automatically, annotate it with
`@AutoService(MessageValidator::class)`:

```kotlin
import com.google.auto.service.AutoService
import io.spine.validation.MessageValidator

@AutoService(MessageValidator::class)
public class TimestampValidator : MessageValidator<Timestamp> {
    // ...
}
```

### Validation logic

The core logic is intentionally small: it first delegates to `Timestamps.isValid(message)` and,
if invalid, adds a field-specific violation for each invalid field (`seconds` and/or `nanos`).
For range checks, it relies on `Timestamps.MIN_VALUE` and `Timestamps.MAX_VALUE`.

```kotlin
import com.google.auto.service.AutoService
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import com.google.protobuf.util.Timestamps.MAX_VALUE
import com.google.protobuf.util.Timestamps.MIN_VALUE
import io.spine.validation.DetectedViolation
import io.spine.validation.MessageValidator

@AutoService(MessageValidator::class)
public class TimestampValidator : MessageValidator<Timestamp> {

    override fun validate(message: Timestamp): List<DetectedViolation> {
        if (Timestamps.isValid(message)) {
            return emptyList()
        }
        val violations = mutableListOf<DetectedViolation>()
        if (message.seconds < MIN_VALUE.seconds ||
            message.seconds > MAX_VALUE.seconds) {
            violations.add(invalidSeconds(message.seconds))
        }
        if (message.nanos !in 0..MAX_VALUE.nanos) {
            violations.add(invalidNanos(message.nanos))
        }
        return violations
    }
}
```

### Reporting violations with placeholders

`TimestampValidator` reports errors via `FieldViolation`, providing:

- `fieldPath` — which field is invalid (for example, `"seconds"`),
- `fieldValue` — the actual invalid value, and
- `message` — a `TemplateString` with placeholders and a placeholder-to-value map.

The message is defined as a template (via `withPlaceholders`) and populated by specifying
values in `placeholderValue`. This keeps error messages machine-friendly and allows consistent
formatting, logging, and customization.

When violations are converted to regular `ConstraintViolation`s, Spine Validation also populates
the `validator` placeholder with the fully qualified class name of the validator.

Below is the helper that creates a violation for invalid `seconds`
(the `invalidNanos()` function is similar):

```kotlin
private fun invalidSeconds(seconds: Long): FieldViolation = FieldViolation(
    message = templateString {
        withPlaceholders =
            "The ${FIELD_PATH.value} value is out of range" +
                    " (${RANGE_VALUE.value}): $seconds."
        placeholderValue.put(FIELD_PATH.value, "seconds")
        placeholderValue.put(RANGE_VALUE.value,
            "${MIN_VALUE.seconds}..${MAX_VALUE.seconds}")

    },
    fieldPath = fieldPath {
        fieldName.add("seconds")
    },
    fieldValue = seconds
)
```

## Walkthrough: validate a nested message field

To validate a message nested inside another message, mark the field with `(validate) = true`.
This applies both generated constraints (if any) and validators registered for the nested type.

Suppose your local model uses `Timestamp`:

```proto
import "google/protobuf/timestamp.proto";
import "spine/options.proto";

message Meeting {
    google.protobuf.Timestamp starts_at = 1 [(validate) = true];
}
```

Once you add a validator for `Timestamp`, validation of `Meeting`
reports a violation for the `starts_at` field if the timestamp is invalid pointing
to the nested field in error (for example, to `starts_at.seconds`).

## What’s next

- [Custom validation](../08-custom-validation/)
- [Architecture](../09-developers-guide/architecture.md)
