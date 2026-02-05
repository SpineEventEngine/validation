# Gradle project of the Validation Documentation

This document describes the Gradle project structure and build configuration for
building the documentation [preview site](./_preview) for the Spine Validation library.

The site built by this project is used for development and preview purposes.
It is not meant to be published as-is, but rather to be used as a staging area for
the documentation before it is merged into the main [documentation][main-documentation] project.

## Build tasks

The [Gradle build](build.gradle.kts) under the `docs/` provides the following tasks:

1. **`buildAll`** — building the code of the examples embedded in the documentation. 
    This ensures that all code snippets are valid and can be compiled.

2. **`embedCode`** — embedding the source code of the examples into the Markdown files. 
    This allows the documentation to include up-to-date code snippets from the source files.
   This task is meant to be run manually when you write or update the documentation.

3. **`checkSamples`** — checking that the embedded code snippets in the Markdown files are
   consistent with the source code. 
   This is a validation step to ensure that the documentation accurately reflects the example code.
   This task is executed automatically via the [GitHub workflow][check-code-embedding].

4. **`buildSite`** — building the documentation site using Hugo. 
   The site is generated in the [_preview/](./_preview) directory.

5. **`runSite`** — running a local Hugo server to preview the documentation site. 
   This allows you to view the documentation in a web browser during development.

6. **`installDependencies`** — installing the Node dependencies for the preview site. 
   Tasks related to Hugo depend on this task. It is not meant to be run manually.

## Including example sources

It is done in the [`settings.gradle.kts`](settings.gradle.kts) file via `includeBuild` directive.
If you add another example proejct, please remember to update
the [`settings.gradle.kts`](settings.gradle.kts) file.

## Linking to the main project build

The example projects share the following directories and files with
the main code project via symlinks:
 * `buildSrc`
 * `gradle.properties`
 * `gradlew`
 * `gradlew.bat`
        
When you add an example project, make sure to create the necessary symlinks to
these files and directories.

[check-code-embedding]: ../.github/workflows/check-code-embedding.yml
[main-documentation]: https://github.com/SpineEventEngine/documentation/
