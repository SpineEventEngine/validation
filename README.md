[![Ubuntu build][ubuntu-build-badge]][gh-actions]
[![codecov][codecov-badge]][codecov] &nbsp;
[![license][license-badge]][license]
[![docs][docs-badge]][docs]

# Spine Validation

Spine Validation generates type-safe validation code from constraints you declare
directly in your Protobuf messages, and enforces those checks automatically when
messages are built — no hand-written validators, reflection, or separate validation
API calls. It is part of the Spine SDK but works standalone in any Java/Kotlin
project that models data with Protocol Buffers.

📖 **Full documentation: [spine.io/docs/validation][docs]**

## Validation in action

Declare constraints right in the `.proto` file:

```protobuf
import "spine/options.proto";
import "spine/time_options.proto";
import "google/protobuf/timestamp.proto";

message CardNumber {
    string digits = 1 [(pattern).regex = "\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}"];
    string owner = 2 [(required) = true];
    google.protobuf.Timestamp issued_at = 3 [(when).in = PAST];
}
```

At build time, Spine Validation injects the assertions into the generated code, so
constructing an invalid message fails fast:

```kotlin
val card = cardNumber {
    digits = "invalid"
} // ← throws ValidationException
```

See the “[Getting started][getting-started]” guide for the full Java/Kotlin
walkthrough, non-throwing validation, and the complete list of
[built-in options][options-docs].

## Adding to your build

Spine Validation plugs into a Gradle/Protobuf build. The minimal standalone setup:

```kotlin
plugins {
    id("io.spine.validation") version "$version"
}
```

The plugin wires Validation into the Spine Compiler and brings in the runtime
library automatically. Spine artifacts are published to Spine-specific
repositories, and there is also a CoreJvm-based setup. See
[Adding Validation to your build][adding-to-build] for the repositories, both setup
modes, and the current version.

### Prerequisites

- Java 17+
- Gradle (Kotlin DSL or Groovy)

## Extending the library

You can define your own Protobuf options and code-generation logic. See the
“[Custom validation][custom-validation]” section of the User Guide.

## Links

- [Documentation][docs]
- [GitHub project][gh-project]
- [Report an issue][issues]
- [Examples: `hello-validation`][examples]
- [Spine Event Engine][spine-home]

[codecov]: https://codecov.io/gh/SpineEventEngine/validation
[codecov-badge]: https://codecov.io/gh/SpineEventEngine/validation/branch/master/graph/badge.svg
[license-badge]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0
[gh-actions]: https://github.com/SpineEventEngine/validation/actions
[ubuntu-build-badge]: https://github.com/SpineEventEngine/validation/actions/workflows/build-on-ubuntu.yml/badge.svg
[docs-badge]: https://img.shields.io/badge/docs-spine.io-blue.svg?style=flat
[docs]: https://spine.io/docs/validation/

[getting-started]: https://spine.io/docs/validation/user/01-getting-started/
[adding-to-build]: https://spine.io/docs/validation/user/01-getting-started/adding-to-build/
[options-docs]: https://spine.io/docs/validation/user/03-built-in-options/
[custom-validation]: https://spine.io/docs/validation/user/05-custom-validation/
[gh-project]: https://github.com/SpineEventEngine/validation
[issues]: https://github.com/SpineEventEngine/validation/issues/new
[examples]: https://github.com/spine-examples/hello-validation
[spine-home]: https://spine.io/
