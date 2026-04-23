# Task: handle codegen of `(when)` option via `ValidationOption` service interface

## Current state
The `(when)` option is currently handled in two modules:
1. In the `context` module where...
  - `WienFieldView::class.java` is passed to the `views` parameter of `ValidationPlugin`
  - `WhenReaction()` is passed to the `reactions` parameter of `ValidationPlugin`
  - Correspodingly, these types belong to the `context` module.
2. In the `java` module where...
  - `JavaValidationRenderer` creates `WhenGenerator` as one of the built-in generators in
   the function `builtInGenerators()`.

There is also a Protobuf enum `TimeFieldType` which belongs to the `context` module.

## Goal
- Consolidate the code under the `java` module preparing for further migration
  to the Spine Time library.

## Package structure
- `io.spine.tools.time.validation` - the root package for all code related to `(when)`
  option handling in the `java` module.
- The Protobuf package for `TimeFieldType` should be changed to `spine.time.validation`.
  It's Java package should be `io.spine.time.validation`.
- `WhenFieldView` and `WhenReaction` should be under the `io.spine.tools.time.validation` package.
- `WhenGenerator` should be under the `io.spine.tools.time.validation.java` package.

## Employ the `ValidationOption` service interface
- The `ValidationOption` service interface should be implemented in the `java` module 
  wrapping all the logic related to the `(when)` option. 
- The implementation should be registered using `AutoService`.
- Use KSP AutoService processor as the dependency for the `java` module.

## Working with Git
- Do not remove and copy files. Move them using `git mv` to preserve the history instead.
