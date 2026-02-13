# Adding Validation to a Gradle build

Spine Validation can be added to a JVM project in two different ways.
Choose the setup that matches your project:

- Standalone use in any Protobuf-based project.
- As part of a Spine-based project that already uses the CoreJvm toolchain.

Both modes integrate the Validation compiler into the build and add the runtime library.


## Mode 1: standalone via Validation Gradle Plugin

Use this mode if you want to run Validation without the rest of Spine.

1) Add the Validation plugin to the build.

```kotlin
plugins {
    id("io.spine.validation") version "<spine-version>"
}
```

2) Make sure your repositories include Spine artifacts (the same repositories you use
   for other Spine tools and libraries).

3) Optional: control Validation generation explicitly.

```kotlin
spine {
   validation {
       enabled.set(true) // `true` by default
   }
}
```

The plugin wires Validation into Spine Compiler, adds the Validation Java codegen bundle,
and brings in the JVM runtime dependency automatically.


## Mode 2: Spine-based project via CoreJvm Gradle Plugin

If your project is base on the Spine CoreJvm library, apply the CoreJvm Gradle plugin instead of
adding Validation directly. CoreJvm brings in the Validation Gradle plugin for you.

```kotlin
plugins {
    id("io.spine.core-jvm") version "<spine-version>"
}
```

Validation is available right away, and you can configure it using the same `validation`
extension if needed.


## Next step

Continue with [Your first validated model](first-model.md).
