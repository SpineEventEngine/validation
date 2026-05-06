---
title: Options for `repeated` and `map` fields
description: How to choose built-in validation options for collections.
headline: Documentation
weight: 40
---

# Options for `repeated` and `map` fields

{{% note-block class="lead" %}}
Use this page when you want to validate **collections** declared as `repeated` fields
or `map` fields.
{{% /note-block %}}

## Choose an option

- Require a collection to be non-empty: `(required) = true`
- Enforce uniqueness: `(distinct) = true`, customize with `(if_has_duplicates).error_msg`
- Validate nested message elements/values: `(validate) = true`
- Enforce numeric constraints per element: `(min)`, `(max)`, `(range)`
- Enforce a pattern per element: `(pattern).regex`
- Require a companion field when the collection is present: `(goes).with = "other_field"`

For canonical definitions, see
[spine/options.proto](https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto).

## Presence: `(required)` 

Use `(required) = true` when a `repeated` field or a `map` must contain **at least one element**.

```protobuf
import "spine/options.proto";

message Team {
  repeated string members = 1 [(required) = true];
}
```

## Uniqueness: `(distinct)` and `(if_has_duplicates)` 

Use `(distinct) = true` when all elements in a collection must be unique.

**Applies to**

- `repeated` fields — elements must be unique.
- `map` fields — values must be unique; keys are already unique by Protobuf rules.

**Minimal example**

```protobuf
import "spine/options.proto";

message Labels {
  repeated string value = 1 [(distinct) = true];
}
```

**Map values**

```protobuf
import "spine/options.proto";

message Emails {
  map<string, string> by_type = 1 [(distinct) = true];
}
```

**Common gotcha**

Uniqueness is checked by full element equality, for example, `equals()` in Java.
If you need “unique by ID”, model the ID as the element itself, or use a `map` keyed by the ID.

## Nested validation: `(validate)` for elements and map values

Use `(validate) = true` when collection elements (or map values) are messages and must satisfy
their own constraints.

```protobuf
import "spine/options.proto";

message PhoneNumber {
  string value = 1 [(required) = true, (pattern).regex = "^\\+?[0-9\\s\\-()]{1,30}$"];
}

message ContactBook {
  repeated PhoneNumber number = 1 [(validate) = true];
}
```

## Per-element value constraints

### Numeric options: `(min)`, `(max)`, `(range)`

Use numeric constraints on `repeated` numeric fields to validate each element.

```protobuf
import "spine/options.proto";

message Measurements {
  repeated int32 sample = 1 [(min).value = "0", (max).value = "100"];
}
```

### String patterns: `(pattern)`

Use `(pattern)` on `repeated string` fields to validate each element.

```protobuf
import "spine/options.proto";

message Tags {
  repeated string value = 1 [(pattern).regex = "^[a-z0-9-]{1,32}$"];
}
```
