# Your first validated model

This guide shows how to define a Protobuf model with validation rules and how those
rules are enforced in the generated JVM code.

The validation options come from `spine/options.proto` and include constraints like
`(required)`, `(min)`, `(max)`, `(pattern)`, `(distinct)`, and `(validate)`.

## 1) Configure the project

First, make sure your project is configured to use the Validation library.
See [Adding Validation to a Gradle build](adding-to-build.md) for detailed instructions.

## 2) Define a validated message

Create a `.proto` file and import the validation options you need:

<embed-code file="first-model/src/main/proto/spine/validation/docs/first_model/bank_card.proto" start="syntax" end="^}"></embed-code>
```protobuf
syntax = "proto3";

package spine.validation.docs.first_model;

import "spine/options.proto";

option (type_url_prefix) = "type.spine.io";
option java_package = "io.spine.validation.docs.firstmodel";
option java_multiple_files = true;

// Provides bank card information with validation rules.
//
// The digits of the card are simplified for the sake of the example.
//
message BankCard {

    // The digits of the card number.
    //
    // Must be present and match a 16-digit card number formatted as four
    // groups of four digits separated by spaces (for example, "1234 5678 1234 5678").
    //
    string digits = 1 [
        (required) = true,
        (pattern).regex = "^\\d{4}(?: \\d{4}){3}"
    ];

    // The name of the card owner.
    //
    // Must be present, contain at least 4 characters, start with a Latin letter,
    // and have exactly one space between words.
    //
    string owner = 2 [
        (required) = true,
        (pattern).regex = "^(?=.{4,})[A-Z][A-Za-z]*(?: [A-Za-z]+)+$"
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


## 3) Build the project

Run your Gradle build as usual.

For macOS and Linux:
```
./gradlew clean build
```

For Windows:
```
gradlew.bat clean build
```

The Validation Gradle plugin integrates with Spine Compiler
and injects validation checks into the generated Java code.

The code will be generated under the `generated` directory of your project.

## 4) Use the generated validation

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
