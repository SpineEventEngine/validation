# Task: Document Passing Custom Validation

We need to describe how `ValidationOption` descendant comes into play for a custom
validation option.
This should be a new section which comes after the "Implement the `Generator`" section.

## Current implementation details

A `ValidationOption` descendants are discovered using the `ServiceLoader` mechanism
by `JavaValidationPlugin` class. This class is a plugin to the Spine Compiler.
Such plugins are passed to the Compiler using the user's classpath in a Gradle build file.
The Validation Gradle plugin arranges this when it is added to a Gradle project.
In order to be discovered, a `ValidationOption` must be present in the user's classpath too.

There are two scenarios for achieving this:

1. Add `io.spine.compiler` to the project and use `spineCompiler` configuration
passing the implementation of `ValidationOption` as a dependency. 
This is a simple option which would work when a custom validation option is 
not frequently used, e.g. in a single project of a multi-module build.

2. Create a Gradle plugin that would add the `ValidationOption` implementation
to the Compiler's user classpath of the project. This is the option that the Time library uses and
we need to describe it in the documentation as well.
It is more complex but allows to reuse the custom validation option across multiple projects
of a multi-module build or when a custom option is packaged as a library.

## Gradle plugin implementation

It's available in the Spine Time module `gradle-plugin` which is locally available
via the directory `docs/_time/gradle-plugin`. It is available via the GitHub repository
of Spine Time too: https://github.com/SpineEventEngine/time.
Use the GitHub repository for the links to the full source code.
Use locally downloaded code for embedding of smaller code samples.
