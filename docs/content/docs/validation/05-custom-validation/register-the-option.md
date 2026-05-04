---
title: Register the option
description: How to register the Protobuf extension and wire up the ValidationOption.
headline: Documentation
---

# Register the option

Two registrations are required before the build plugin can use a custom option:

1. Register the Protobuf extension, so the descriptor machinery can resolve it at runtime.
2. Wire up the `Reaction`, `View`, and `Generator` via `ValidationOption`.

## Register the proto extension

Create a class that implements `OptionsProvider` and annotate it with
`@AutoService(OptionsProvider.class)`:

```java
import com.google.auto.service.AutoService;
import com.google.protobuf.ExtensionRegistry;
import io.spine.option.OptionsProvider;

@AutoService(OptionsProvider.class)
public class TimeOptionsProvider implements OptionsProvider {

    @Override
    public void registerIn(ExtensionRegistry registry) {
        TimeOptionsProto.registerAllExtensions(registry);
    }
}
```

Call `registerAllExtensions` on the generated outer class of the `.proto` file that contains
the `extend` block. The `java_outer_classname` option in the proto file controls this class name
(for example, `option java_outer_classname = "TimeOptionsProto"`). Without this registration,
the Protobuf runtime cannot deserialize the extension field and the option will be silently
ignored.

## Wire up `ValidationOption`

Create a class that implements `ValidationOption` and annotate it with
`@AutoService(ValidationOption::class)`:

```kotlin
import com.google.auto.service.AutoService
import io.spine.tools.validation.java.ValidationOption
import io.spine.tools.validation.java.generate.OptionGenerator

@AutoService(ValidationOption::class)
public class WhenOption : ValidationOption {

    public companion object {
        public const val NAME: String = "when"
    }

    override val reactions: Set<Reaction<*>> = setOf(WhenReaction())

    override val view: Set<Class<out View<*, *, *>>> = setOf(WhenFieldView::class.java)

    override val generator: OptionGenerator = WhenGenerator()
}
```

Key points:

- `NAME` is a `const val` in the companion object. Its value must exactly match the field name
  used in the `extend` block (for example, `when`). The `Reaction` uses this constant in its
  `@Where` filter to subscribe only to events for this option.
- `reactions` can contain multiple `Reaction` instances; `view` can list multiple `View` classes.
- `generator` accepts exactly **one** `OptionGenerator`. Only one generator per
  `ValidationOption` is allowed.

## Configure service discovery

Both `OptionsProvider` and `ValidationOption` are discovered via Java `ServiceLoader`.
We use `@AutoService` annotation for creating a service file under `META-INF/services`.
Correspondingly, an annotation processor must run during compilation.

For Kotlin implementations, configure AutoService's KSP processor in the module that contains the
`@AutoService` classes:

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.2.0")
}
```

If the provider is written in Java, use AutoService's `annotationProcessor` dependency instead of
KSP. See the [AutoService documentation][auto-service] for Gradle examples.

For the full source, see [`TimeOptionsProvider.java`][time-options-provider] and
[`WhenOption.kt`][when-option-kt] in the Spine Time repository.

## What's next

- [Declare the event and view state](declare-event-and-view.md)
- [Back to Custom Validation](_index.md)

[auto-service]: https://github.com/google/auto/tree/main/service
[time-options-provider]: https://github.com/SpineEventEngine/time/blob/master/time/src/main/java/io/spine/time/validation/TimeOptionsProvider.java
[when-option-kt]: https://github.com/SpineEventEngine/time/blob/master/validation/src/main/kotlin/io/spine/tools/time/validation/java/WhenOption.kt
