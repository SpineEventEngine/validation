---
title: Using the generated code
description: How to use the generated validation code in Java and Kotlin.
headline: Documentation
---

# Using the generated code

This guide shows how the generated JVM API enforces constraints at runtime.

Validation runs automatically on builder `build()`. To validate without throwing, call
`validate()` explicitly.

## Validation on `build()`

When a message violates declared constraints, `build()` throws `ValidationException`.

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
<embed-code file="first-model/src/test/kotlin/io/spine/validation/docs/firstmodel/BankCardKtTest.kt" fragment="invalid-digits"></embed-code>
```kotlin
shouldThrow<ValidationException> {
    // Kotlin proto DSL delegates to a Java builder.
    bankCard {
        digits = "invalid"
        owner = "ALEX SMITH"
    }
}
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
<embed-code file="first-model/src/test/java/io/spine/validation/docs/firstmodel/BankCardTest.java" fragment="invalid-digits"></embed-code>
```java
assertThrows(ValidationException.class, () ->
    BankCard.newBuilder()
        .setDigits("invalid")
        .setOwner("ALEX SMITH")
        .build()
);
```
{{< /code-tab >}}
{{< /code-tabs >}}

## Validate without throwing

To get a `ValidationError` instead of an exception, build the message using the `buildPartial()`
and call `validate()`:

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
<embed-code file="first-model/src/test/kotlin/io/spine/validation/docs/firstmodel/BankCardKtTest.kt" fragment="error-message"></embed-code>
```kotlin
// There is no Kotlin DSL which allows building a non-valid message.
// So we use a builder from Java.
val card = BankCard.newBuilder()
    .setOwner("ALEX SMITH")
    .setDigits("wrong number")
    .buildPartial()
val error = card.validate()
error.shouldBePresent()

val violation = error.get().constraintViolationList[0]
val formatted = violation.message.format()

formatted shouldContain "digits"
formatted shouldContain "wrong number"
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
<embed-code file="first-model/src/test/java/io/spine/validation/docs/firstmodel/BankCardTest.java" fragment="error-message"></embed-code>
```java
var card = BankCard.newBuilder()
        .setOwner("ALEX SMITH")
        .setDigits("wrong number")
        .buildPartial();
var error = card.validate();
assertThat(error).isPresent();

var violation = error.get().getConstraintViolation(0);
var formatted = TemplateStrings.format(violation.getMessage());

assertThat(formatted).contains("digits");
assertThat(formatted).contains("wrong number");
```
{{< /code-tab >}}
{{< /code-tabs >}}

You are not likely to use `buildPartial()` for building invalid messages in production code.
But you may want to use `validate()` for checking messages received from external sources,
such as API requests or deserialized data.

## What’s next

- Learn how Validation works internally:
  [Architecture](../09-developers-guide/architecture.md).
- If you need organization-specific rules:
  [Custom validation](../08-custom-validation/).
