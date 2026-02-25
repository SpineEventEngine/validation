---
title: Options overview
description: Where validation options come from and how they influence generated code.
headline: Documentation
---

# Options overview

Spine Validation rules are expressed as **Protobuf options**.
You annotate your `.proto` model with built-in options, and the **Validation Compiler**
turns those option values into runtime checks in the generated JVM code.

This page explains where the built-in options come from, how they are applied at build time,
and what API you get at runtime.


## Where options come from

The built-in validation options are defined in Spine option protos:

- `spine/options.proto` — general validation options.
- `spine/time_options.proto` — time-related options.

To use an option, import the proto that defines it and annotate your fields and messages.

```protobuf
import "spine/options.proto";
import "spine/time_options.proto";
```

If you need a refresher on how custom options work in Protobuf, see
[Protobuf custom options](https://protobuf.dev/programming-guides/proto3/#customoptions).


## How options are applied (build time)

Spine Validation is enforced by **generated code**, not by interpreting option values at runtime.

At build time:

1. `protoc` compiles your `.proto` files into **descriptors** and generates Java sources.
2. The Spine Validation Gradle plugin wires the **Validation Compiler** into the build.
3. The Validation Compiler reads the compiled descriptors (including your option values) and
   augments `protoc` output with validation logic.

The result is a JVM API that enforces the rules you declared in the model.


## What code you get (runtime)

Generated messages and builders provide a small validation-focused API surface:

- `build()` performs validation and throws `ValidationException` if a constraint is violated.
- `buildPartial()` builds without validation.
- `validate()` checks an existing instance and returns `Optional<ValidationError>`.

See [Using the generated code](../01-getting-started/generated-code.md) for Java and Kotlin examples.


## What does not happen

At runtime, Spine Validation does **not** parse descriptor option data to decide what to validate.
All checks are already compiled into the generated message/builder code.


## Tiny examples: options → generated checks

These examples are intentionally minimal.
They are only meant to illustrate the “annotate with options → get runtime checks” flow.

### Required field

```protobuf
import "spine/options.proto";

message UserEmail {
  string value = 1 [(required) = true];
}
```

### String pattern

```protobuf
import "spine/options.proto";

message OrderId {
  string value = 1 [(pattern).regex = "^[A-Z]{3}-\\d{6}$"];
}
```

### Numeric range

```protobuf
import "spine/options.proto";

message Temperature {
  int32 celsius = 1 [
    (min).value = "0",
    (max).value = "100"
  ];
}
```


## What’s next

- Learn how to define organization-specific rules:
  [Custom validation](../08-custom-validation/).
- Built-in options reference (planned).

