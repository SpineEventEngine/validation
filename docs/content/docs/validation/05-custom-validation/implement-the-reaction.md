---
title: 'Implement the `Reaction`'
description: 'How to implement a `Reaction` that discovers and validates a custom option.'
headline: Documentation
---

# Implement the `Reaction`

The `Reaction` is the entry point for option handling. It subscribes to
`FieldOptionDiscovered` events (or one of the other `*OptionDiscovered` variants),
filters them by option name, validates the option application, and emits a domain event
when the option is applied correctly.

## Class declaration

```kotlin
internal class WhenReaction : Reaction<FieldOptionDiscovered>()
```

The type parameter is the event the `Reaction` listens to. For a field-level option, use
`FieldOptionDiscovered`. Other variants include `FileOptionDiscovered`,
`MessageOptionDiscovered`, and `OneofOptionDiscovered`.

## `Reaction` method

```kotlin
@React
override fun whenever(
    @External @Where(field = OPTION_NAME, equals = WhenOption.NAME)
    event: FieldOptionDiscovered
): EitherOf2<WhenFieldDiscovered, NoReaction> {
    val field = event.subject
    val file = event.file

    val timeType = checkFieldType(field, typeSystem, file)

    val option = event.option.value.unpack<TimeOption>()
    val timeBound = option.`in`
    if (timeBound == Time.TIME_UNDEFINED) {
        return ignore()
    }

    val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
    message.checkPlaceholders(SUPPORTED_PLACEHOLDERS, field, file, WhenOption.NAME)

    return whenFieldDiscovered {
        id = field.ref
        subject = field
        errorMessage = message
        bound = timeBound
        type = timeType
    }.asA()
}
```

### Annotations

- `@React` marks the method as the reaction handler; only one such method is allowed per class.
- `@External` tells the framework that `FieldOptionDiscovered` originates from the compiler's
  bounded context, not the current one.
- `@Where(field = OPTION_NAME, equals = WhenOption.NAME)` narrows the subscription so that
  `whenever` receives only events where the option name equals `"when"`. `OPTION_NAME` is a
  constant from `io.spine.tools.validation` that names the filter field. Without this filter,
  the `Reaction` would be called for every field option discovered during compilation.

### Return type

`EitherOf2<WhenFieldDiscovered, NoReaction>` expresses that the method either emits a domain
event or signals that no reaction should take place.

## Three possible outcomes

### 1. Unsupported field type

The main `whenever` snippet calls `checkFieldType(field, typeSystem, file)`, which is a
private helper that wraps `Compilation.check`:

```kotlin
private fun checkFieldType(field: Field, typeSystem: TypeSystem, file: File): TimeFieldType {
    val timeType = typeSystem.determineTimeType(field.type)
    Compilation.check(timeType != TFT_UNKNOWN, file, field.span) {
        "The field type `${field.type.name}` of `${field.qualifiedName}` " +
        "is not supported by the `(${WhenOption.NAME})` option."
    }
    return timeType
}
```

`Compilation.check` throws a compilation exception when the condition is `false`, causing the
build to fail with the supplied message pointing to the source file and span. Extracting the
check into a helper keeps the main reaction method readable and allows the helper to also return
the resolved `TimeFieldType` for later use.

### 2. Disabled option

Short-circuit with `return ignore()` (which returns `NoReaction`) when the option value equals
the sentinel `TIME_UNDEFINED`. This represents a correctly formed but effectively disabled
option — for example, `[(when).in = TIME_UNDEFINED]`. No domain event is emitted and no code is
generated for that field.

### 3. Valid, enabled option

Validate the error message template with `checkPlaceholders`, then build and emit the domain
event using the Kotlin DSL:

```kotlin
return whenFieldDiscovered {
    id = field.ref
    subject = field
    errorMessage = message
    bound = timeBound
    type = timeType
}.asA()
```

`checkPlaceholders` reports a compilation error if the template contains placeholder names that
the option does not support. `.asA()` wraps the event in the `EitherOf2` left slot.

## What's next

- [Implement the `View`](implement-the-view.md)
- [Back to Custom Validation](../)
