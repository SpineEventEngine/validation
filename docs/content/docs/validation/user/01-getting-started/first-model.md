---
title: Defining constraints
description: How to declare validation rules in Protobuf files.
headline: Documentation
---

# Define constraints in `.proto` files

This guide shows how to import Spine Validation options and declare constraints
in your Protobuf model.

The validation options come from `spine/options.proto` and include constraints like
`(required)`, `(min)`, `(max)`, `(pattern)`, `(distinct)`, and `(validate)`.

## Import options and declare constraints

Create a `.proto` file and import `spine/options.proto`:

<embed-code 
    file="$examples/first-model/src/main/proto/spine/validation/docs/first_model/bank_card.proto" 
    start="syntax" 
    end="^}">
</embed-code>
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

## Next step

Continue with [Build your project](build-project.md).
