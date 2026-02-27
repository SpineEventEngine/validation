---
title: Working with error messages
description: How validation error messages are built, customized, and formatted.
headline: Documentation
---

# Working with error messages

When a message violates validation constraints, Spine Validation reports violations as
`ConstraintViolation` entries inside a `ValidationError`.

Each violation contains a machine-friendly error message (`TemplateString`) which you can:

- format for end users,
- log for diagnostics, and
- customize in your `.proto` model.


## Where error messages come from

Every built-in validation option defines a **default** error message.
These messages come as the value of the `default_message` option declared for each option type.

For example, the `pattern` option defines the following default message:

```protobuf
message PatternOption {

    // The default error message.
    option (default_message) = "The `${parent.type}.${field.path}` field"
        " must match the regular expression `${regex.pattern}` (modifiers: `${regex.modifiers}`)."
        " The passed value: `${field.value}`.";
    
    // ...
}
```

When you apply an option in a `.proto` file, you can **override** the default message by
setting the option’s `error_msg` field.

Example: custom message for a regex pattern.

```protobuf
import "spine/options.proto";

message CreateAccount {
    string id = 1 [
        (pattern).regex = "^[A-Za-z0-9+]+$",
        (pattern).error_msg = "ID must be alphanumerical in `${parent.type}`. Provided: `${field.value}`."
    ];
}
```

The placeholders (like `${field.value}`) are substituted at runtime when the violation is created.

{{% note-block class="note" %}}
Each option documents the placeholders it supports next to its `error_msg` field
in `spine/options.proto`.
{{% /note-block %}}

## Placeholders and `TemplateString`

`ConstraintViolation.message` is a `TemplateString`:

- `with_placeholders` — a template string that may contain placeholders like `${field.path}`.
- `placeholder_value` — a map from placeholder keys to their runtime values.

The placeholder keys in the map do **not** include `${}` — for example, `field.path`.

{{% note-block class="warning" %}}
The map may include extra keys that are not referenced by the template, but every placeholder
used in `with_placeholders` **must** have a corresponding value.
Otherwise, the template is invalid.
{{% /note-block %}}

## Formatting messages in code

To format a `TemplateString`, use:

- Kotlin: `TemplateString.format()` / `TemplateString.formatUnsafe()`
- Java: `TemplateStrings.format(TemplateString)` / `TemplateStrings.formatUnsafe(TemplateString)`

`format()` validates that all placeholders have values and throws `IllegalArgumentException` otherwise.
`formatUnsafe()` does not validate and leaves missing placeholders unsubstituted.

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
```kotlin
val error = message.validate().orElse(null) ?: return
val violation = error.constraintViolationList.first()
val text = violation.message.format()
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
```java
var error = message.validate();
if (error.isEmpty()) {
    return;
}
var violation = error.get().getConstraintViolation(0);
var text = TemplateStrings.format(violation.getMessage());
```
{{< /code-tab >}}

{{< /code-tabs >}}

## Choosing what to show vs what to log

For **end-user** output:

- format and display `ConstraintViolation.message`;
- avoid leaking internal type names unless your product requires them.

For **diagnostics** and support logs:

- include `type_name` and `field_path` to pinpoint the location of the violation;
- include the raw template (`message.with_placeholders`) and the placeholder map for debugging.


## Troubleshooting formatting issues

### `IllegalArgumentException` from `format()`

This means `with_placeholders` references a placeholder that has no entry in `placeholder_value`.

Recommended actions:

- treat this as a bug in the option/message definition, and fix the custom `error_msg` in your `.proto`;
- if you can tolerate partial substitution (for example, in logs), use `formatUnsafe()`.

## What’s next

- See how constraints are expressed as options and compiled into runtime checks:
  [Options overview](options-overview.md).
- Explore the built-in options:
  [Built-in options](../03-built-in-options/).
- Learn how to validate message types from third-party libraries:
  [Validating third-party messages](../04-third-party-messages/).
- If built-in options are not enough, define your own constraints and messages:
  [Custom validation](../08-custom-validation/).
