---
title: Field-level options
description: How to choose built-in validation options applied to fields.
headline: Documentation
weight: 10
---

# Field-level options

{{% note-block class="lead" %}}
Use this page when you want to validate a **single field** by declaring options next to it in a
`.proto` file.
{{% /note-block %}}

## Choose an option

- Require a value to be present: `(required)`, customize with `(if_missing).error_msg`
- Enforce a numeric boundary: `(min)`, `(max)`, or `(range)`
- Enforce a string format: `(pattern).regex`
- Validate a nested message: `(validate) = true`
- Require another field when this one is set: `(goes).with = "other_field"`
- Prevent reassignment: `(set_once) = true`, customize with `(if_set_again).error_msg`

For canonical definitions, see
[spine/options.proto](https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto).

## Presence: `(required)` and `(if_missing)`

Use `(required)` when a field must not be “unset” for its type.

**Applies to**

- Message and enum fields (must be non-default).
- `string` / `bytes` fields (must be non-empty).
- Collections (`repeated` / `map`) (must be non-empty; see also
  [Options for `repeated` and `map` fields](repeated-and-map-fields.md)).

**Minimal example**

```protobuf
import "spine/options.proto";

message UserEmail {
  string value = 1 [(required) = true];
}
```

**Custom message**

```protobuf
import "spine/options.proto";

message Student {
  string id = 1 [
    (required) = true,
    (if_missing).error_msg = "Student ID must be set."
  ];
}
```

## Numeric constraints

Use `(min)`, `(max)`, and `(range)` when a numeric value must fall within a bound.

**Applies to**

- Singular numeric fields.
- `repeated` numeric fields — each element is checked.

### Choose between `(min)` / `(max)` and `(range)`

- Use `(min)` / `(max)` for **unbounded** ranges (for example, “`>= 0`”).
- Use `(range)` for a **bounded** interval in one option.

**Minimal example**

```protobuf
import "spine/options.proto";

message Temperature {
  int32 kelvin = 1 [(min).value = "0"];
}
```

**Bounded range**

```protobuf
import "spine/options.proto";

message Percent {
  int32 value = 1 [(range).value = "[0..100]"];
}
```

## Patterns: `(pattern)`

Use `(pattern).regex` when a `string` must match a regular expression.

**Applies to**

- Singular `string` fields.
- `repeated string` fields — each element is checked.

**Minimal example**

```protobuf
import "spine/options.proto";

message OrderId {
  string value = 1 [(pattern).regex = "^[A-Z]{3}-\\d{6}$"];
}
```

## Nested validation: `(validate)`

Use `(validate) = true` when a field refers to another **message type** and you want to enforce
the nested message’s own constraints.

**Applies to**

- Singular message fields.
- Repeated fields of message type.
- Map fields with message values.

**Common gotcha: default instances**

For **singular** message fields, default instances are treated like “no value set”, even when
`(validate) = true`. If you want to reject default instances, make the field required.

```protobuf
import "spine/options.proto";

message Address {
  string value = 1 [(required) = true];
}

message Student {
  Address address = 1 [(validate) = true, (required) = true];
}
```

## Cross-field dependency: `(goes)`

Use `(goes).with = "companion"` when this field is only valid if another field is also set.

**Applies to**

- Message and enum fields.
- `string` / `bytes` fields.
- Collections (`repeated` / `map`).

**Minimal example**

```protobuf
import "spine/options.proto";

message ScheduledItem {
  string date = 1;
  string time = 2 [(goes).with = "date"];
}
```

## Single assignment: `(set_once)` and `(if_set_again)` 

Use `(set_once) = true` when a field must be assigned at most once, for example, a permanent ID.

**Applies to**

- Singular fields of supported scalar, enum, and message types.

**Does not apply to**

- `repeated` / `map` fields.
- Fields with explicit `optional` cardinality.

**Minimal example**

```protobuf
import "spine/options.proto";

message UserId {
  string value = 1 [(required) = true];
}

message User {
  UserId id = 1 [(set_once) = true];
}
```

