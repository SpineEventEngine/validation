---
title: Validation Gradle plugin
description: What the plugin does, and how to configure it via the `validation { ... }` extension.
headline: Documentation
---

# Validation Gradle plugin

The Validation Gradle plugin wires Spine Validation into your build. This page
describes what the plugin does and how to configure it through the
`validation { ... }` extension.

If you only need to install the plugin, see
“[Adding to build](../01-getting-started/adding-to-build.md)”; this page
focuses on configuration.


## What the plugin does

When applied under the ID `io.spine.validation`, the plugin:

- registers the Validation plugin to Spine Compiler so it runs as part of
  code generation;
- adds the Validation JVM runtime as an `implementation` dependency;
- transitively applies the [Protobuf Gradle plugin][protobuf-gradle] and the
  Spine Compiler Gradle plugin so users do not have to apply either manually.

The plugin can be added directly or pulled in through the CoreJvm toolchain;
both modes end up integrating the same Validation Compiler. See
“[Adding Gradle plugins to the build][adding-plugins]” for the install steps.


## Configuration via the extension

The plugin registers an extension named `validation` under the project-wide
`spine { }` block:

```kotlin
spine {
    validation {
        // All defaults: validation is enabled; all warnings are emitted.
    }
}
```

The block is optional — applying the plugin alone gives you the default
behavior. Use `validation { ... }` only when you need to turn something on or off.

The DSL uses lazy `Property<T>` values throughout, so settings can be combined
with other plugins and resolved at configuration time without forcing eager
evaluation. Toggles are written in positive polarity (`enabled = true`,
`unsignedFields = true`), matching the way the warning is described in build output.


## Properties

### `enabled`

Type: `Property<Boolean>`. Default: `true`.

Controls whether the Validation plugin to Spine Compiler is registered.
When set to `false`, code generation for validation rules is skipped:

```kotlin
spine {
    validation {
        enabled = false
    }
}
```

The Validation runtime dependency is still added to the project even when
`enabled` is `false`. This keeps build files stable when you toggle code
generation on and off, and keeps the runtime API available — for example, for
[validators](../04-validators/) registered via `MessageValidator`, which do not
rely on generated checks.

### `java { ... }`

Nested block. Holds configuration specific to the Java target of the Validation
Compiler — currently the per-kind warning toggles.

#### `java.warnings { ... }`

Nested block. Suppresses warnings emitted by the Java target of the Validation
Compiler on a per-kind basis. Each property is positive: `true` means the
warning is emitted (the default); set it to `false` to silence the corresponding
warning.

##### `unsignedFields`

Type: `Property<Boolean>`. Default: `true`.

When `false`, suppresses the *"unsigned integer types are not supported in
Java"* warning emitted for `uint32` and `uint64` fields that carry `(range)`,
`(min)`, or `(max)`:

```kotlin
spine {
    validation {
        java {
            warnings {
                unsignedFields = false
            }
        }
    }
}
```

The warning highlights a real Java limitation: `uint32` and `uint64` values are
stored in signed `int` and `long` fields, so range checks have to handle the
sign bit explicitly. Turn the warning off only after reading it once and
accepting the trade-off for the affected fields.


## What's next

- Install the plugin: [Adding Validation to your build](../01-getting-started/adding-to-build.md).
- Extend the plugin with project-specific options: [Custom validation](../05-custom-validation/).

[protobuf-gradle]: https://github.com/google/protobuf-gradle-plugin
[adding-plugins]: ../01-getting-started/adding-to-build.md#adding-gradle-plugins-to-the-build
