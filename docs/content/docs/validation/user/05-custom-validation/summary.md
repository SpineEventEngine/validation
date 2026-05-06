---
title: 'Summary'
description: A recap of the steps for implementing and integrating a custom validation option.
headline: Documentation
---

# Summary

This section walked through the complete workflow for adding a custom validation option to the
Spine Validation library, from declaring the option in Protobuf to making it available to the
Spine Compiler.

## What you covered

- **[Declare the option in Protobuf](declare-the-option.md)** — define a Protobuf `extend`
  block targeting a standard descriptor option type, with a unique field number and an error
  message template that supports named placeholders.

- **[Register the option](register-the-option.md)** — wire an `OptionsProvider` to register the
  Protobuf extension at runtime, and a `ValidationOption` to bind the option name to its
  `Reaction`, `View`, and `Generator`. Both are discovered via Java `ServiceLoader`.

- **[Declare the event and view state](declare-event-and-view.md)** — define a domain event
  emitted when the option is encountered, and a projection state message the `Generator`
  queries to accumulate option data.

- **[Implement the `Reaction`](implement-the-reaction.md)** — subscribe to `FieldOptionDiscovered`
  events, filter by option name, validate the field type and option value, and emit the domain
  event — or signal no reaction when the option is disabled.

- **[Implement the `View`](implement-the-view.md)** — build a projection that accumulates event
  data, making the full set of option applications queryable by the `Generator`.

- **[Implement the `Generator`](implement-the-generator.md)** — query the `View` and produce
  Java validation code inlined into the generated `validate()` method, handling both single
  and repeated field cardinalities.

- **[Pass the option to the Compiler](pass-to-compiler.md)** — place the `ValidationOption`
  implementation on the Compiler's user classpath via a direct `spineCompiler` dependency or
  a distributable Gradle plugin.

## What's next

- Explore library internals: [Developer's guide](../06-developers-guide/)
- See the modules that back this pipeline:
  [Key modules](../06-developers-guide/key-modules.md)

The complete source for all running examples is in the
[`validation` module][time-validation-src] of the Spine Time repository.

[time-validation-src]: https://github.com/SpineEventEngine/time/tree/master/validation
