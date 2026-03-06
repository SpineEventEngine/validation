---
title: Using Validators
description: How to validate messages with custom code using `MessageValidator`.
headline: Documentation
---

# Using Validators

Spine Validation enforces most constraints via **code generation** from `.proto` options.
Sometimes this is not enough, or not possible.

Use validators when you need **custom logic in code**:

- validate **external** message types whose `.proto` files you cannot change (e.g. `google.protobuf.Timestamp`);
- validate **local** messages when the rule requires computation and cannot be expressed as proto options.

Validators are implemented via `io.spine.validation.MessageValidator<M>` and executed by
`io.spine.validation.ValidatorRegistry`.

## When to use validators

Prefer `.proto` options when you can:

1. Use [built-in options](../03-built-in-options/).
2. If built-ins are not enough, implement [custom validation options](../08-custom-validation/).

Use `MessageValidator` when:

- You cannot modify the `.proto` source of a message type (external messages).
- You need checks that depend on multiple fields, computations, or library calls (local messages).

## Create a validator

To validate a message type `M`:

1. Implement `io.spine.validation.MessageValidator<M>`.
2. Make the validator discoverable via Java `ServiceLoader` (recommended), or register it in
   `ValidatorRegistry` explicitly at application startup.
3. Ensure the class has a public no-args constructor (Kotlin/Java default constructors work).

### ServiceLoader discovery (recommended)

`ValidatorRegistry` loads implementations of `MessageValidator` from the classpath via
`ServiceLoader`.
On the JVM, the easiest way to generate the required `META-INF/services/...` entry is to annotate
your validator with `@AutoService(MessageValidator::class)`:

```kotlin
import com.google.auto.service.AutoService
import io.spine.validation.DetectedViolation
import io.spine.validation.MessageValidator

@AutoService(MessageValidator::class)
public class MeetingValidator : MessageValidator<Meeting> {
    override fun validate(message: Meeting): List<DetectedViolation> = emptyList()
}
```

### Explicit registration (alternative)

If you prefer not to rely on classpath discovery, add validators during application startup:

```kotlin
ValidatorRegistry.add(Meeting::class, MeetingValidator())
```

## Apply a validator

You can apply validators in three common ways:

1. **Validate a message directly via the registry**:

   ```kotlin
   val violations = ValidatorRegistry.validate(message)
   ```
   Please note that this approach does not apply any checks generated from `.proto` options,
   only registered validators.

2. **Validate a message as part of the generated message validation**.  
   Spine Validation-generated `validate()` methods include registry-based validation, so a
   validator registered for the message type is applied alongside checks produced from `.proto`
   options.

3. **Validate nested message fields** by marking them with `(validate) = true`.  
   Nested validation runs both the nested message’s generated constraints (if any) and any
   validators registered for the nested message type — including external message types.

## Multiple validators per message type

You can register more than one validator for the same message type.
When the message is validated, Spine Validation applies all registered validators and reports
their violations together.

## What’s next
- [Implement a validator](implement-a-validator.md)
- [Using `ValidatorRegistry`](validator-registry.md)
- [Custom validation](../08-custom-validation/)
- [Architecture](../09-developers-guide/architecture.md)
