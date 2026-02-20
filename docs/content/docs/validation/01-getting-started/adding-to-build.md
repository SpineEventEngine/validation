# Adding Validation to a Gradle build

## Adding Spine-specific Gradle plugin repositories

<embed-code file="settings.gradle.kts" start="pluginManagement {" end="^}"></embed-code>
```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            url = java.net.URI("https://europe-maven.pkg.dev/spine-event-engine/snapshots")
        }
        maven {
            url = java.net.URI("https://europe-maven.pkg.dev/spine-event-engine/releases")
        }
    }
}
```
The repositories at https://europe-maven.pkg.dev are needed for obtaining the versions of
Spine tools and libraries that are not yet published to the Gradle Plugin Portal or Maven Central.

## Adding Gradle plugins to the build

Spine Validation can be added to a JVM project in two different ways.
Choose the setup that matches your project:

- Standalone use in any Protobuf-based project.
- As part of a Spine-based project that uses the CoreJvm toolchain.

Both modes integrate the Validation compiler into the build and add the runtime library.


### Mode 1: standalone via Validation Gradle plugin

Use this mode if you want to run Validation without the rest of Spine.

Add the Validation plugin to the build.

<embed-code file="first-model/build.gradle.kts" start="plugins {" end="^}"></embed-code>
```kotlin
plugins {
    module
    id("io.spine.validation") version "2.0.0-SNAPSHOT.395"
}
```

> ##### What is the `module` plugin?
> The plugin `module` in the snippet above refers to the name of the script plugin
> `buildSrc/src/kotlin/module.gradle.kts` which provides common configuration for
> the subprojects of the multi-module examples project.
> For more details on this, clone the [Validation examples repository][valildation-examples].

The plugin wires Validation into Spine Compiler, adds the Validation Java codegen bundle,
and brings in the JVM runtime dependency automatically.


### Mode 2: Spine-based project via CoreJvm Gradle plugin

If your project is based on the Spine CoreJvm library, apply the CoreJvm Gradle plugin instead of
adding Validation directly. CoreJvm brings in the Validation Gradle plugin for you.

<embed-code file="first-model-with-framework/build.gradle.kts" start="plugins {" end="^}"></embed-code>
```kotlin
plugins {
    module
    id("io.spine.core-jvm") version "2.0.0-SNAPSHOT.053"
}
```

## Next step

Continue with [Your first validated model](first-model.md).

[valildation-examples]: https://github.com/spine-examples/hello-validation
