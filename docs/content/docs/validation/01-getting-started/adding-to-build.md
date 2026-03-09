---
title: Adding to build
description: How to add Spine Validation to your Gradle project.
headline: Documentation
---

# Adding Validation to a Gradle build

## Adding Spine-specific Gradle plugin repositories

<embed-code file="settings.gradle.kts" start="pluginManagement {" end="^}"></embed-code>
```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://europe-maven.pkg.dev/spine-event-engine/snapshots")
        maven("https://europe-maven.pkg.dev/spine-event-engine/releases")
    }
}
```
The repositories at https://europe-maven.pkg.dev are needed to obtain versions of
Spine tools and libraries that are not yet published to the Gradle Plugin Portal or Maven Central.

## Optional: adding Spine-specific Maven repositories

Similar to the plugin repositories, a project using Validation may need artifacts that
are not yet published to Maven Central. Our Gradle plugins take care of this by adding the 
necessary repositories to the project when applied. Normally, you don't need to add
repositories manually when using the plugins.

**But there is one exception to this rule:** if you use centralized repository management
in your `settings.gradle.kts` file and the `repositoriesMode` is set to
a value other than `PREFER_PROJECT`. For example:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        // Other repositories...
    }
}
```
In such a case, an attempt to add a repository at the project level would fail the build,
so our plugins do not add the repositories to avoid the failure. Therefore,
you need to add the repositories manually to the `repositories` block in `settings.gradle.kts`
using the `maven()` calls as shown in the snippet below.

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        // Spine-specific repositories:
        maven("https://europe-maven.pkg.dev/spine-event-engine/snapshots")
        maven("https://europe-maven.pkg.dev/spine-event-engine/releases")
        // Other repositories...
    }
}
```

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
    id("io.spine.validation") version "2.0.0-SNAPSHOT.402"
}
```

{{% note-block class="note" %}}
#### What is the `module` plugin?

The plugin `module` in the snippet above refers to the name of the script plugin
`buildSrc/src/kotlin/module.gradle.kts` which provides common configuration for
the subprojects of the multi-module examples project.
For more details on this, clone the [Validation examples repository][validation-examples].
{{% /note-block %}}

The plugin wires Validation into the Spine Compiler and brings in
the JVM runtime dependency automatically.


### Mode 2: Spine-based project via CoreJvm Gradle plugin

If your project is based on the Spine CoreJvm library, apply the CoreJvm Gradle plugin instead of
adding Validation directly. CoreJvm brings in the Validation Gradle plugin for you.

<embed-code file="first-model-with-framework/build.gradle.kts" start="plugins {" end="^}"></embed-code>
```kotlin
plugins {
    module
    id("io.spine.core-jvm") version "2.0.0-SNAPSHOT.054"
}
```

## Next step

Continue with [Define constraints in `.proto` files](first-model.md).

[validation-examples]: https://github.com/spine-examples/hello-validation
