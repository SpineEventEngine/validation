---
title: Custom Validation
description: Extending the library with custom options and code generation.
headline: Documentation
---

# Custom validation

Users can extend the Validation library by providing custom Protobuf options and code generation logic.

Follow these steps to create a custom option:

1. [Declare the option](declare-the-option.md) as a Protobuf extension in your `.proto` files.
2. [Register the option](register-the-option.md) via `io.spine.option.OptionsProvider`
   and wire up the entities via `io.spine.tools.validation.java.ValidationOption`.
3. [Declare the event and view state](declare-event-and-view.md) — the Protobuf types that
   track the option's discovery during compilation.
4. [Implement the `Reaction`](implement-the-reaction.md) — discovers and validates the option.
5. [Implement the `View`](implement-the-view.md) — accumulates valid option applications.
6. [Implement the `Generator`](implement-the-generator.md) — generates Java code for the option.

Below is a workflow diagram for a typical option:

![Typical custom option](typical_custom_option.jpg)

Note that a custom option can provide several reactions and views, but only one generator.
This allows building more complex models using more entities and events.

## Components

### `Reaction`

Subscribes to `*OptionDiscovered` events and filters them by option name.
See [Implement the `Reaction`](implement-the-reaction.md) for details.

### `View`

Accumulates events emitted by the `Reaction` so the `Generator` can query them.
See [Implement the `View`](implement-the-view.md) for details.

### `Generator`

Produces Java code for every application of the option within a message type.
See [Implement the `Generator`](implement-the-generator.md) for details.

## Running example

Throughout this section, the `(when)` option from the
[Spine Time](https://github.com/SpineEventEngine/time) library serves as the running example.
Spine Time is a library of Protobuf-based date and time types for business models. It defines
its own Protobuf message types — such as `LocalDate`, `LocalTime`, and `ZonedDateTime` — and
provides converters to and from the standard Java Time API. Among other features, it ships a
`(when)` validation option that constrains a time-typed field to hold either a past or a
future value.

## What’s next

- [Declare the option in Protobuf](declare-the-option.md)
- [Register the option](register-the-option.md)
- [Declare the event and view state](declare-event-and-view.md)
- [Implement the `Reaction`](implement-the-reaction.md)
- [Implement the `View`](implement-the-view.md)
- [Implement the `Generator`](implement-the-generator.md)
- [Using validators](../04-validators/)
- Learn where this plugs in: [Architecture](../09-developers-guide/architecture.md).
