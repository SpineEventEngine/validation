---
title: Using Generated Code
description: How to use the generated validation API in Java and Kotlin.
headline: Documentation
---

# Using the generated validation API

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

To get a `ValidationError` instead of an exception, build the message partially and call
`validate()`:

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
<embed-code file="first-model/src/test/kotlin/io/spine/validation/docs/firstmodel/BankCardKtTest.kt" fragment="error-message"></embed-code>
```kotlin
val card = BankCard.newBuilder()
    .setOwner("ALEX SMITH")
    .setDigits("wrong number")
    .buildPartial() // There is no Kotlin DSL for this.
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

## What’s next

- Learn how Validation works internally:
  [Architecture](../09-developers-guide/architecture.md).
- If you need organization-specific rules:
  [Custom validation](../08-custom-validation/).
