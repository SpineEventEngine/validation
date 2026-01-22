# Your first validated model

This guide shows how to define a Protobuf model with validation rules and how those
rules are enforced in the generated JVM code.

The validation options come from two files:

- `spine/options.proto` for general constraints like `(required)`, `(min)`, `(max)`,
  `(pattern)`, `(distinct)`, and `(validate)`.
- `spine/time_options.proto` for time constraints such as `(when).in = PAST | FUTURE`.


## 1) Define a validated message

Create a `.proto` file and import the validation options you need:

```protobuf
syntax = "proto3";

package example;

import "google/protobuf/timestamp.proto";
import "spine/options.proto";
import "spine/time_options.proto";

// Provides bank card information with validation rules.
//
// The digits of the card are simplified for the sake of the example.
//
message BankCard {
    
    // Must be present and match a simple card pattern.
    string digits = 1 [
        (required) = true,
        (pattern).regex = "^\\d{4}(?: \\d{4}){3}"
    ];

    // Must be present and contain at least 4 Latin letters or spaces.
    string owner = 2 [
        (required) = true,
        (pattern).regex = "^[A-Z](?:[A-Za-z ]{2,}[a-z])$"
    ];

    // Must be in the past.
    google.protobuf.Timestamp issued_at = 3 [
        (when).in = PAST,
        (required) = true,
        (validate) = true
    ];

    // All tags must be unique. Tags are optional.
    repeated string tags = 4 [
        (distinct) = true
    ];
}
```

Notes on the options used:

- `(required)` ensures the field is set and not default or empty.
- `(pattern).regex` validates string contents with a regular expression.
- `(when).in` restricts time values to `PAST` or `FUTURE`.
- `(distinct)` enforces uniqueness in `repeated` and `map` fields.


## 2) Build the project

Run your Gradle build as usual. The Validation Gradle plugin integrates with Spine Compiler
and injects validation checks into the generated Java code.


## 3) Use the generated validation

Validation runs on `build()` and can be triggered manually with `validate()`.

```java
var card = BankCard.newBuilder()
    .setDigits("invalid")
    .setOwner("Al")
    .build(); // Throws `ValidationException`.
```

```kotlin
val card = bankCard {
    digits = "invalid"
    owner = "Al"
} // Throws `ValidationException`.
```

To validate without throwing, use `validate()` on a built message:

```java
var card = BankCard.newBuilder()
    .setDigits("invalid")
    .buildPartial();

var error = card.validate();
error.ifPresent(err -> System.out.println(err.getMessage()));
```
   

## What’s next

Continue with [Validation Workflow](workflow.md).
