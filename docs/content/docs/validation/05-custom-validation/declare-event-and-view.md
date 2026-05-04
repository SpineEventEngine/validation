---
title: Declare the event and view state
description: How to define the domain event and view state Protobuf types for a custom option.
headline: Documentation
---

# Declare the event and view state

After defining the option, declare the Protobuf types that track its discovery during
compilation. This takes two parts:
 1. a domain event emitted by the `Reaction` when a valid option application is found,
 2. a `View` state that persists the event data for the `Generator` to query.

## Declare the event

The `Reaction` emits a domain event carrying all data that the `View` and `Generator` need. The event
travels through the compilation bounded context, so it must be a proper Protobuf message.

```protobuf
message WhenFieldDiscovered {

    spine.compiler.FieldRef id = 1;

    spine.compiler.Field subject = 2;

    string error_message = 3;

    Time bound = 4;

    TimeFieldType type = 5;
}
```

The `id` field must be the **first** field in the declaration order, and must be the same
type that the `View` uses as its entity identity (`compiler.FieldRef` in this case).
The framework uses the identity field to route the event to the correct `View` instance.

## Declare the `View` state

The view state is the persistent accumulator queried by the `Generator`.
It mirrors the event fields:

```protobuf
message WhenField {
    option (entity).kind = VIEW;

    spine.compiler.FieldRef id = 1;

    spine.compiler.Field subject = 2;

    string error_message = 3;

    Time bound = 4;

    TimeFieldType type = 5;
}
```

The `id` field type must match the event `id` type exactly. Without this match, the framework
cannot route `WhenFieldDiscovered` events to the correct `WhenField` view instance.

For the full source, see [`events.proto`][events-proto] and [`views.proto`][views-proto]
in the Spine Time repository.

## What's next

- [Implement the `Reaction`](implement-the-reaction.md)
- [Back to Custom Validation](_index.md)

[events-proto]: https://github.com/SpineEventEngine/time/blob/master/validation/src/main/proto/spine/tools/time/validation/events.proto
[views-proto]: https://github.com/SpineEventEngine/time/blob/master/validation/src/main/proto/spine/tools/time/validation/views.proto
