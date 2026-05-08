---
title: Options for `oneof` fields
description: How to enforce selection rules for `oneof` groups.
headline: Documentation
weight: 20
---

# Options for `oneof` fields

{{% note-block class="lead" %}}
Use this page when you have a `oneof` group and want to enforce that **one of its cases is set**.
{{% /note-block %}}

For canonical definitions, see
[spine/options.proto](https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto).

## Required selection: `(choice)`

Use `(choice).required = true` on a `oneof` group to require selecting one of its fields.

**Applies to**

- `oneof` groups (declared on the `oneof` itself, not on a field inside it).

**Minimal example**

```protobuf
import "spine/options.proto";

message Contact {
  oneof channel {
    option (choice).required = true;

    string email = 1;
    string phone = 2;
  }
}
```

## Common combinations / gotchas

- Use `(choice)` for the group, and apply field-level constraints (like `(pattern)` or
  `(required)`) to individual cases when needed.
- If the requirement is “at least one of these fields OR one of these other fields”, use
  [Message-level options](message-level-options.md) with `(require).fields` and include `oneof`
  group names.
