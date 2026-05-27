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

## Table of contents

- [Who this is for](#who-this-is-for)
- [How it works](#how-it-works)
- [Quick example](#quick-example)
- [Adding to your build](#adding-to-your-build)
- [Compatibility](#compatibility)
- [Error handling](#error-handling)
- [Troubleshooting](#troubleshooting)
- [Extending the library](#extending-the-library)
- [Links](#links)

## Who this is for

Use Spine Validation if you want to:

- Keep validation rules close to your data model in `.proto` files.
- Enforce constraints automatically during message construction.
- Avoid hand-written validators and reflection-based runtime checks.
- Generate validation once at build time and use it from Java/Kotlin code.

## How it works

1. You declare constraints in Protobuf options such as `required`, `pattern`, and time constraints.
2. During build, the Validation plugin integrates with the Spine Compiler.
3. Generated message code contains validation assertions.
4. Invalid values fail fast when messages are built.

## Quick example

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

At build time, Spine Validation injects assertions into generated code, so
constructing an invalid message fails fast:

```kotlin
val card = cardNumber {
    digits = "invalid"
} // throws ValidationException
```

See the “[Getting started][getting-started]” guide for the full Java/Kotlin
walkthrough, non-throwing validation, and the complete list of
[built-in options][options-docs].

## Adding to your build

Spine Validation plugs into a Gradle/Protobuf build. The minimal standalone setup:

```kotlin
plugins {
    id("io.spine.validation") version("<latest-version>")
}
```

The plugin wires Validation into the Spine Compiler and brings in the runtime library
automatically.

For repository configuration, standalone/CoreJvm setup modes, and current
artifact/plugin versions, see [Adding Validation to your build][adding-to-build].

> Tip: If you are integrating into an existing multi-module build, apply the plugin
> only to modules that own Protobuf schemas requiring validation.

### Prerequisites

- Java 17+
- Gradle (Kotlin DSL or Groovy)

## Compatibility

This README assumes Java 17+ and a Gradle-based Protobuf build.
For authoritative, up-to-date compatibility and version guidance, see
[Adding Validation to your build][adding-to-build].

## Error handling

Validation can be used in:

- **Fail-fast mode**: invalid message construction throws `ValidationException`.
- **Non-throwing workflows**: supported patterns are documented in
  [Getting started][getting-started].

## Troubleshooting

Common first-run issues:

- **Plugin resolves but build fails later**: ensure required Spine repositories are configured.
- **No validation code generated**: verify the plugin is applied in the module that owns `.proto` files.
- **Constraints appear ignored**: confirm option imports are present in the schema file.
- **IDE does not see generated code**: refresh Gradle project and generated source roots.

If issues persist, check docs or open an issue: [Report an issue][issues].

## Extending the library

You can define your own Protobuf options and code-generation logic. See the
“[Custom validation][custom-validation]” section of the User Guide.

## Links

- [Documentation][docs]
- [Getting started][getting-started]
- [Built-in options reference][options-docs]
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
