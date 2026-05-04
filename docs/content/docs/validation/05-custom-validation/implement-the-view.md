---
title: 'Implement the `View`'
description: 'How to implement a `View` that accumulates option data for code generation.'
headline: Documentation
---

# Implement the `View`

The `View` is a Spine projection that persists the data emitted by the `Reaction`.
The `Generator` queries the `View` to obtain the information it needs to produce Java code.

## Class declaration

```kotlin
internal class WhenFieldView : View<FieldRef, WhenField, WhenField.Builder>()
```

The three type parameters are:

| Parameter | Type                | Description                                                                 |
|-----------|---------------------|-----------------------------------------------------------------------------|
| ID        | `FieldRef`          | Entity identity; must match the `id` field type in the Protobuf view state. |
| `State`   | `WhenField`         | The generated Protobuf message that holds the accumulated data.             |
| `Builder` | `WhenField.Builder` | The corresponding Protobuf builder, used by the `alter { }` DSL.            |

## Subscriber method

```kotlin
@Subscribe
fun on(e: WhenFieldDiscovered) = alter {
    subject = e.subject
    errorMessage = e.errorMessage
    bound = e.bound
    type = e.type
}
```

The `@Subscribe` annotation registers `on` as the event handler. The `alter { }` DSL block
provides access to the state builder; any assignments inside the block are applied atomically
when the block exits. One `@Subscribe` method is required for each event type the `View` handles.

`View`s only accumulate data. Validation and business logic belong in the `Reaction`; by the time
an event reaches the `View`, the `Reaction` has already confirmed that the option application is
correct.

## What's next

- [Implement the `Generator`](implement-the-generator.md)
- [Back to Custom Validation](../)
