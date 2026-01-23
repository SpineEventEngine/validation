# Your first validated model

This guide shows how to define a Protobuf model with validation rules and how those
rules are enforced in the generated JVM code.

The validation options come from `spine/options.proto` and include constraints like
`(required)`, `(min)`, `(max)`, `(pattern)`, `(distinct)`, and `(validate)`.


## 1) Define a validated message

Create a `.proto` file and import the validation options you need:

```protobuf
syntax = "proto3";

package example;

import "spine/options.proto";

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

    // All tags must be unique. Tags are optional.
    repeated string tags = 3 [
        (distinct) = true
    ];
}
```

Notes on the options used:

- `(required)` ensures the field is set and not default or empty.
- `(pattern).regex` validates string contents with a regular expression.
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
