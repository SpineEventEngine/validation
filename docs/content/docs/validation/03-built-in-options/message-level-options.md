---
title: Message-level options
description: How to express validation rules that depend on multiple fields.
headline: Documentation
weight: 30
---

# Message-level options

{{% note-block class="lead" %}}
Use this page when message validity depends on **multiple fields together**, such as:

- “At least one of these fields must be set.”
- “Either this group of fields is set, or that group is set.”
{{% /note-block %}}


For canonical definitions, see
[spine/options.proto](https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto).

## Field group requirements: `(require)`

Use `option (require).fields` on a message to declare **alternative groups** of required fields.

**How to write the expression**

- Use `|` to separate alternative groups.
- Use `&` inside a group to require multiple fields together.
- You can use `oneof` group names as operands (useful together with `(choice)`).

**Applies to**

- Message types (declared as a message option).

**Minimal example**

```protobuf
import "spine/options.proto";

message PersonName {
  option (require).fields = "given_name | honorific_prefix & family_name";

  string honorific_prefix = 1;
  string given_name = 2;
  string family_name = 3;
}
```

## Common gotchas

- This option works only with field types where “set” is well-defined: messages/enums
  (non-default), `string`/`bytes` (non-empty), and collections (non-empty). If you need a similar
  rule for scalars, wrap the scalar into a message or use a `oneof`.
