# Task: Write documentation on working with validation error messages
- Placement of the page: Under the Concepts section after the “Options overview” page.
- Purpose: explain how validation error messages work, how to customize them, and how to use them in code.
- Target outcome: a reader can explain the relationship between validation options and error messages,
- how to define custom messages, and how to format them for end users or diagnostics.
- Terminology: `TemplateString`, `ValidationError`, `ConstraintViolation`, `format()`, `formatUnsafe()`
- Describe that validation options have default error messages and user-defined error messages
  (via the `error_msg` field of a validation option, such as `(pattern).error_msg`).
- Give an example of a custom validation error message with placeholders
  defined in a proto field option. Use `spine/options.proto` as a reference for
  the option definition and the `error_msg` field.
- Explain the notion of placeholders in error messages and how they correspond to
  values provided at runtime when a violation occurs.
- Describe how `TemplateString` works (placeholders + values) and how to convert it to a
  human-readable message (formatting).
- Clarify the recommended ways to work with Validation errors in:
    - Kotlin: `TemplateString.format()` / `TemplateString.formatUnsafe()`.
    - Java: `io.spine.validation.TemplateStrings.format(TemplateString)`
    - Java: `io.spine.validation.TemplateStrings.formatUnsafe(TemplateString)`.
- Explain the structure of `ValidationError` / `ConstraintViolation`, and what fields developers
  should use when:
    - displaying messages to end users;
    - logging diagnostics (e.g. include `type_name`, `field_path`, and the unformatted template).
- Add troubleshooting notes for common runtime formatting problems (e.g. missing placeholder
  values; choosing `formatUnsafe()` when partial substitution is acceptable).
- Source references to anchor the docs:
    - `jvm-runtime/src/main/proto/spine/validation/error_message.proto`
    - `jvm-runtime/src/main/proto/spine/validation/validation_error.proto`
    - `jvm-runtime/src/main/kotlin/io/spine/validation/TemplateStringExts.kt`
    - `jvm-runtime/src/main/kotlin/io/spine/validation/RuntimeErrorPlaceholder.kt`

## Output

Implemented as `docs/content/docs/validation/02-concepts/error-messages.md`.
