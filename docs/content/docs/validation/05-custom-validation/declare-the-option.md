---
title: Declare the option in Protobuf
description: How to define the option message in Protobuf.
headline: Documentation
---

# Declare the option in Protobuf

Declaring a custom validation option requires two Protobuf declarations:
a message that carries the option's data, and an `extend` block that
registers it as a descriptor option.

## Declare the option message

Define a Protobuf message to hold the option's parameters.
Use the `(default_message)` option to set the error template rendered when the
caller does not supply a custom `error_msg`:

```protobuf
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

## Extend an option descriptor type

Register the message as a Protobuf option by declaring an `extend` block
targeting one of the standard descriptor option types —
`google.protobuf.FieldOptions`, `MessageOptions`, and so on:

```protobuf
extend google.protobuf.FieldOptions {
    TimeOption when = 73819;
}
```

Field number `73819` is the globally registered extension number for this
option; every extension must have a unique number in the allowed range.

{{% note-block class="note" %}}
#### Packaging trade-off

If you omit the `package` declaration from the `.proto` file that defines the extension, callers
can write `[(when).in = FUTURE]` instead of `[(spine.time.when).in = FUTURE]`. This is a
deliberate trade-off: shorter option syntax at the cost of no package-level namespacing.
{{% /note-block %}}

## What's next

- [Declare the event and view state](declare-event-and-view.md)
- [Back to Custom Validation](_index.md)
