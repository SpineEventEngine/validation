---
title: Built-in options
description: Reference for built-in Spine Validation options.
headline: Documentation
---

# Built-in options

{{% note-block class="lead" %}}
This section is a “how to choose” catalog of the **built-in validation options**
from `spine/options.proto`.

It focuses on the options that affect **Spine Validation** and skips deprecated and
non-validation options.
{{% /note-block %}}

## When to use this section

Use this section when you want to:

- pick the right option for a specific validation need, like presence, bounds, uniqueness, etc.;
- understand where an option can be declared (field, `oneof`, message);
- see minimal `.proto` snippets you can copy into a model.

## Choose a page

- **Most field constraints** — presence, bounds, patterns, nested validation, dependencies:  
  [Field-level options](field-level-options.md)
- **Enforce “one of these fields must be set” in a `oneof`:**  
  [Options for `oneof` fields](oneof-fields.md)
- **Cross-field rules on a message (“at least one group is set”):**  
  [Message-level options](message-level-options.md)
- **Collections (`repeated` / `map`) — non-empty, uniqueness, per-element validation:**  
  [Options for `repeated` and `map` fields](repeated-and-map-fields.md)

## Source of truth

This catalog is based on the non-deprecated, validation-related options defined in
[spine/options.proto](https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto).

For the canonical option definitions, see `spine/options.proto` in the Spine base libraries
on GitHub: [spine/options.proto](https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto).

## What’s next
- [Field-level options](field-level-options.md)
- [Options for `oneof` fields](oneof-fields.md)
- [Message-level options](message-level-options.md)
- [Options for `repeated` and `map` fields](repeated-and-map-fields.md)
- [Using validators](../04-validators/)
- [Custom validation](../08-custom-validation/)
