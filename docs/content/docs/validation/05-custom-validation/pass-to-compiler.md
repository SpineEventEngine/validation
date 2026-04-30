---
title: 'Pass the option to the Compiler'
description: How to make a ValidationOption implementation available to the Spine Compiler.
headline: Documentation
---

# Pass the option to the Compiler

`JavaValidationPlugin` — the Spine Compiler plugin that generates Java validation code —
discovers `ValidationOption` implementations via the Java `ServiceLoader` mechanism. To be
found, a `ValidationOption` implementation must be present on the Compiler's user classpath.

The `io.spine.validation` Gradle plugin places `JavaValidationPlugin` itself on that classpath
automatically. Adding your custom `ValidationOption` requires one additional step.

There are two approaches:

1. **Direct dependency** — add the implementation module to the `spineCompiler` dependency
   configuration. Use this for a single project or a single module in a multi-module build.

2. **Gradle plugin** — wrap the classpath configuration in a Gradle plugin. Use this when
   the custom option is packaged as a library or shared across multiple modules.

## Direct dependency

The `io.spine.validation` plugin registers a `spineCompiler` dependency configuration that
populates the Compiler's user classpath. Add your implementation as a `spineCompiler` dependency:

```kotlin
dependencies {
    spineCompiler("com.example:my-validation-option:1.0.0")
}
```

When the Compiler runs, all `spineCompiler` artifacts are placed on the user classpath and
`ServiceLoader` can discover your `ValidationOption` implementation.

## Gradle plugin

When the custom option is distributed as a library or reused across projects, a dedicated
Gradle plugin is the preferred approach. The plugin registers the validation module on the
Compiler's user classpath whenever `io.spine.validation` is applied to a project.

The Spine Time library uses this pattern. Its Gradle plugin reacts to the presence of
`io.spine.validation` and calls `addUserClasspathDependency`:

```kotlin
import io.spine.tools.compiler.gradle.api.addUserClasspathDependency

private fun Project.passValidationToCompiler() {
    pluginManager.withPlugin(TimeValidation.validationPluginId) {
        addUserClasspathDependency(TimeValidation.artifact)
    }
}
```

`pluginManager.withPlugin(...)` fires only when the `io.spine.validation` plugin is applied to
the target project. Projects that do not use Spine Validation are unaffected.

`addUserClasspathDependency` from `io.spine.tools.compiler.gradle.api` adds a `MavenArtifact`
to the Compiler's user classpath. The artifact version is resolved from metadata embedded in
the plugin at build time.

For users of the `io.spine.time` Gradle plugin, no extra configuration is needed: applying the
plugin to a project that has `io.spine.validation` automatically places the `time-validation`
module on the Compiler's classpath.

See the full source in
[`TimeGradlePlugin.kt`](https://github.com/SpineEventEngine/time/blob/master/gradle-plugin/src/main/kotlin/io/spine/tools/time/gradle/TimeGradlePlugin.kt)
and the artifact definition in
[`TimeValidation.kt`](https://github.com/SpineEventEngine/time/blob/master/gradle-plugin/src/main/kotlin/io/spine/tools/time/gradle/TimeValidation.kt)
in the Spine Time repository.

## What's next

- [Summary](summary.md)
- [Architecture](../09-developers-guide/architecture.md)
