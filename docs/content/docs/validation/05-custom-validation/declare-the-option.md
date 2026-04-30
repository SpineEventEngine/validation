---
title: Declare the option in Protobuf
description: How to define the option message, domain event, and view state in Protobuf.
headline: Documentation
---

# Declare the option in Protobuf

A custom validation option requires three distinct Protobuf definitions:

1. The option message itself (extends a standard descriptor option type).
2. A domain event emitted by the Reaction when a valid option application is found.
3. A view state that persists the event data for the Generator to query.

The examples below are taken from the `(when)` option in `docs/_time/`.

## Declare the option message

An option is declared as a Protobuf `extend` block targeting one of the standard
descriptor option types: `google.protobuf.FieldOptions`, `MessageOptions`, and so on.

```protobuf
extend google.protobuf.FieldOptions {
    TimeOption when = 73819;
}

message TimeOption {

    option (default_message) = "The field `${parent.type}.${field.path}`"
        " of the type `${field.type}` must be in the `${when.in}`."
        " The encountered value: `${field.value}`.";

    Time in = 1;

    // field 2 is reserved (deprecated msg_format).

    // A user-defined error message.
    string error_msg = 3;
}

enum Time {
    TIME_UNDEFINED = 0;
    PAST = 1;
    FUTURE = 2;
}
```

The `(default_message)` option on `TimeOption` sets the error template used when the caller
does not supply a custom `error_msg`. Field number `73819` is the globally registered extension
number for this option; every extension must have a unique number in the allowed range.

### Packaging trade-off

If you omit the `package` declaration from the `.proto` file that defines the extension, callers
can write `[(when).in = FUTURE]` instead of `[(spine.time.when).in = FUTURE]`. This is a
deliberate trade-off: shorter option syntax at the cost of no package-level namespacing. The
`time_options.proto` file explains this choice in its header comment.

## Declare the event

The Reaction emits a domain event carrying all data that the View and Generator need. The event
travels through the compilation bounded context, so it must be a proper Protobuf message.

```protobuf
message WhenFieldDiscovered {

    compiler.FieldRef id = 1;

    compiler.Field subject = 2;

    string error_message = 3;

    Time bound = 4;

    spine.tools.time.validation.TimeFieldType type = 5;
}
```

The `id` field must be the **first** field and must be the same type that the View uses as its
entity identity (`compiler.FieldRef` in this case). The framework uses the identity field to
route the event to the correct View instance.

## Declare the view state

The view state is the persistent accumulator queried by the Generator. It mirrors the event
fields and is marked as a Spine projection:

```protobuf
message WhenField {
    option (entity).kind = PROJECTION;

    compiler.FieldRef id = 1;

    compiler.Field subject = 2;

    string error_message = 3;

    Time bound = 4;

    spine.tools.time.validation.TimeFieldType type = 5;
}
```

The `id` field type must match the event `id` type exactly. Without this match, the framework
cannot route `WhenFieldDiscovered` events to the correct `WhenField` view instance.

## What's next

- [Register the option](register-the-option.md)
- [Back to Custom Validation](../)
