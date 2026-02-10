# Updater for plugin versions in a `build.gradle.kts` files

## Task Description
This task is designed to replace plugin versions in a `build.gradle.kts` files under the 
given directory.

It searches for plugin declarations in the format `id("plugin-id") version "version-number"`
and replaces the version number taken from the version script file `version.gradle.kts`
in the root of the Gradle project.

A version file format looks like this:

```kotlin
val versionToPublish by extra("2.0.0-SNAPSHOT.394")
```
... and the version of the plugin is defined as the argument the `extra()` call.

## Input
- `directory`: The directory where the `build.gradle.kts` files are located.
   The task will search for all `build.gradle.kts` files in this directory and its subdirectories.
- `versionScriptPath`: The path to the `version.gradle.kts` file that contains the plugin versions.
- `pluginId`: The ID of the plugin whose version needs to be replaced.

## Output
The task will update the `build.gradle.kts` files by replacing the plugin version with
the version specified in the `version.gradle.kts` file.
If the version was updated, the task will print the path of the updated file.

## Example Usage
The below example assumes that the task belongs to a Gradle build under the `docs` directory
of the root main code project. This is the main intended usage.

```kotlin
tasks.register<updateePluginVersion>("updatePluginVersion") {
    directory.set(file("_code/"))
    versionScriptPath.set(file("../version.gradle.kts"))
    pluginId.set("com.example.plugin")
}
```

## Implementation details
The code should be placed under the `buildSrc` directory of this Gradle project,
in a file named `UpdatePluginVersion.kt`.
The package for the task class should be `io.spine.gradle.docs`.
