---
title: Custom Validation
description: Extending the library with custom options and code generation.
headline: Documentation
---

# Custom validation

Users can extend the library by providing custom Protobuf options and code generation logic.

Follow these steps to create a custom option:

1. [Declare](declare-the-option.md) a Protobuf extension, a domain event, and a view state in
   your `.proto` files.
2. [Register](register-the-option.md) the Protobuf extension via `io.spine.option.OptionsProvider`
   and wire up the entities via `io.spine.tools.validation.java.ValidationOption`.
3. [Implement the Reaction](implement-the-reaction.md) — discovers and validates the option.
4. [Implement the View](implement-the-view.md) — accumulates valid option applications.
5. [Implement the Generator](implement-the-generator.md) — generates Java code for the option.

Below is a workflow diagram for a typical option:

![Typical custom option](typical_custom_option.jpg)

Note that a custom option can provide several reactions and views, but only one generator.
This allows building more complex models using more entities and events.

## Components

### Reaction

Subscribes to `*OptionDiscovered` events and filters them by option name.
See [Implement the Reaction](implement-the-reaction.md) for details.

### View

Accumulates events emitted by the Reaction so the Generator can query them.
See [Implement the View](implement-the-view.md) for details.

### Generator

Produces Java code for every application of the option within a message type.
See [Implement the Generator](implement-the-generator.md) for details.

## What’s next

- [Declare the option in Protobuf](declare-the-option.md)
- [Register the option](register-the-option.md)
- [Implement the Reaction](implement-the-reaction.md)
- [Implement the View](implement-the-view.md)
- [Implement the Generator](implement-the-generator.md)
- [Using validators](../04-validators/)
- Learn where this plugs in: [Architecture](../09-developers-guide/architecture.md).

The `:tests:extensions` module contains a full example of the custom `(currency)` option.
