---
title: Implement an external validator
description: How to validate message types generated outside your build using `MessageValidator`.
headline: Documentation
---

# Implement an external validator

To validate an external message type `M` (a Protobuf message generated outside your build):

1. Implement `io.spine.validation.MessageValidator<M>`.
2. Annotate the implementation with `@io.spine.validation.Validator(M::class)`.
3. Ensure the class has a `public`, no-args constructor.

Spine Validation creates a new validator instance per invocation, so keep validators stateless and
cheap to construct.

## Reference implementation: `TimestampValidator`

Let's review the `MessageValidator` implementation on the example of
`io.spine.validation.TimestampValidator` from the Validation JVM runtime.

It validates `com.google.protobuf.Timestamp` and reports violations for invalid
`seconds` and `nanos` values.


## Walkthrough: validate `google.protobuf.Timestamp` inside a local message

Suppose your local model uses `Timestamp`:

```proto
import "google/protobuf/timestamp.proto";

message Meeting {
    google.protobuf.Timestamp starts_at = 1;
}
```

Once you add a validator for `Timestamp` , validation of `Meeting`
reports a violation for the `starts_at` field if the timestamp is invalid pointing
to the nested field in error (for example, to `starts_at.seconds`).

## What’s next

- [Custom validation](../08-custom-validation/)
- [Architecture](../09-developers-guide/architecture.md)
