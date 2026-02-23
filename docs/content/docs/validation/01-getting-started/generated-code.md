# Using the generated validation API

This guide shows how the generated JVM API enforces constraints at runtime.

Validation runs automatically on builder `build()`. To validate without throwing, call
`validate()` explicitly.

## Validation on `build()`

When a message violates declared constraints, `build()` throws `ValidationException`.

<embed-code file="first-model/src/test/java/io/spine/validation/docs/firstmodel/BankCardTest.java" fragment="invalid-digits"></embed-code>
```java
assertThrows(ValidationException.class, () ->
    BankCard.newBuilder()
        .setDigits("invalid")
        .setOwner("ALEX SMITH")
        .build()
);
```

<embed-code file="first-model/src/test/kotlin/io/spine/validation/docs/firstmodel/BankCardKtTest.kt" fragment="invalid-digits"></embed-code>
```kotlin
shouldThrow<ValidationException> {
    bankCard {
        digits = "invalid"
        owner = "ALEX SMITH"
    }
}
```

## Validate without throwing

To get a `ValidationError` instead of an exception, build the message partially and call
`validate()`:

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

## Generated code

The generated Java builder calls `validate()` on `build()` and throws `ValidationException`
if any constraint is violated:

<embed-code file="first-model/generated/main/java/io/spine/validation/docs/firstmodel/BankCard.java" start="public io.spine.validation.docs.firstmodel.@io.spine.validation.Validated BankCard build()" end="return result;"></embed-code>

A constraint like `(required) = true` generates a field check inside the `validate(...)`
method:

<embed-code file="first-model/generated/main/java/io/spine/validation/docs/firstmodel/BankCard.java" start="if \\(getDigits\\(\\)\\.equals\\(\"\"\\)\\) \\{" end="violations.add\\(violation\\);"></embed-code>

## What’s next

- Learn how Validation works internally:
  [Architecture](../09-developers-guide/architecture.md).
- If you need organization-specific rules:
  [Custom validation](../08-custom-validation/).
