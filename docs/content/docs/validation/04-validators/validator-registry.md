---
title: Using `ValidatorRegistry`
description: How to register, query, and apply `MessageValidator`s explicitly via `ValidatorRegistry`.
headline: Documentation
---

# Using `ValidatorRegistry`

In most cases, you don’t need to interact with `io.spine.validation.ValidatorRegistry` directly.
Spine Validation discovers validators automatically and applies them as part of the generated
validation API.

Use `ValidatorRegistry` directly when you need to:

- register a validator explicitly, for example, at application startup;
- inspect which validators are registered for a message type;
- remove or replace validators, for example, to override ones discovered automatically;
- validate a message using validators only.

{{% note-block class="note" %}}
`ValidatorRegistry` uses Java `ServiceLoader` to discover validators when the object is first
initialized.
{{% /note-block %}}

## Validate a message

There are two common ways to validate a message when you have validators:

1. `ValidatorRegistry.validate(message)` — applies **validators only**.
2. `message.validate()` — applies **generated checks** (from `.proto` options) and also applies
   all validators registered for that message type.

{{% note-block class="note" %}}
`ValidatorRegistry.validate(message)` is useful when you need to run custom logic independently
from generated checks, or when you validate external message types that do not have a generated
`validate()` method.
{{% /note-block %}}

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
```kotlin
import com.google.protobuf.Timestamp
import io.spine.validation.ValidatorRegistry

val timestamp: Timestamp = // ...

// Applies validators only.
val violations = ValidatorRegistry.validate(timestamp)

// Applies generated checks (if any) and validators registered for the type.
val error = myMessage.validate()
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
```java
import com.google.protobuf.Timestamp;
import io.spine.validation.ValidatorRegistry;

Timestamp timestamp = /* ... */;

// Applies validators only.
var violations = ValidatorRegistry.validate(timestamp);

// Applies generated checks (if any) and validators registered for the type.
var error = myMessage.validate();
```
{{< /code-tab >}}

{{< /code-tabs >}}

## Add a validator

To register a validator explicitly, call `ValidatorRegistry.add()` with:

- the message type the validator applies to, and
- the validator instance.

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
```kotlin
import com.google.protobuf.Timestamp
import io.spine.validation.TimestampValidator
import io.spine.validation.ValidatorRegistry

ValidatorRegistry.add(Timestamp::class, TimestampValidator())
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
```java
import com.google.protobuf.Timestamp;
import io.spine.validation.TimestampValidator;
import io.spine.validation.ValidatorRegistry;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

ValidatorRegistry.add(getKotlinClass(Timestamp.class), new TimestampValidator());
```
{{< /code-tab >}}

{{< /code-tabs >}}

## Query registered validators

To obtain the currently registered validators for a message type, call `ValidatorRegistry.get()`.

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
```kotlin
import com.google.protobuf.Timestamp
import io.spine.validation.ValidatorRegistry

val validators = ValidatorRegistry.get(Timestamp::class)
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
```java
import com.google.protobuf.Timestamp;
import io.spine.validation.ValidatorRegistry;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

var validators = ValidatorRegistry.get(getKotlinClass(Timestamp.class));
```
{{< /code-tab >}}

{{< /code-tabs >}}

## Remove and replace validators

To remove all validators registered for a message type, call `ValidatorRegistry.remove()`.

This is also the simplest way to **override automatically discovered validators**: remove
validators for the message type and then add the desired ones.

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
```kotlin
import com.google.protobuf.Timestamp
import io.spine.validation.ValidatorRegistry

ValidatorRegistry.remove(Timestamp::class)
ValidatorRegistry.add(Timestamp::class, MyTimestampValidator())
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
```java
import com.google.protobuf.Timestamp;
import io.spine.validation.ValidatorRegistry;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

var type = getKotlinClass(Timestamp.class);
ValidatorRegistry.remove(type);
ValidatorRegistry.add(type, new MyTimestampValidator());
```
{{< /code-tab >}}

{{< /code-tabs >}}

## The `${validator}` placeholder

When `ValidatorRegistry` converts validator-reported violations into `ConstraintViolation`s,
it automatically populates the `validator` placeholder with the validator’s fully qualified
class name.

If your error message template references `${validator}`, you can format it after validation:

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
```kotlin
import com.google.protobuf.Timestamp
import io.spine.validation.DetectedViolation
import io.spine.validation.MessageValidator
import io.spine.validation.MessageViolation
import io.spine.validation.ValidatorRegistry
import io.spine.validation.templateString

class MyTimestampValidator : MessageValidator<Timestamp> {
    override fun validate(message: Timestamp): List<DetectedViolation> =
        listOf(
            MessageViolation(
                templateString {
                    withPlaceholders = "Rejected by `\${validator}`."
                }
            )
        )
}

ValidatorRegistry.add(Timestamp::class, MyTimestampValidator())

val timestamp = Timestamp.newBuilder().setNanos(-1).build()
val violation = ValidatorRegistry.validate(timestamp).single()

val placeholder = ValidatorRegistry.VALIDATOR_PLACEHOLDER
val validatorClass = violation.message.placeholderValueMap[placeholder]

// If the template references `${validator}`, it will be substituted here.
val text = violation.message.format()
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
```java
import com.google.protobuf.Timestamp;
import io.spine.validation.DetectedViolation;
import io.spine.validation.MessageValidator;
import io.spine.validation.MessageViolation;
import io.spine.validation.TemplateString;
import io.spine.validation.TemplateStrings;
import io.spine.validation.ValidatorRegistry;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import java.util.List;

final class MyTimestampValidator implements MessageValidator<Timestamp> {
    @Override
    public List<DetectedViolation> validate(Timestamp message) {
        return List.of(
                new MessageViolation(
                        TemplateString.newBuilder()
                                .setWithPlaceholders("Rejected by ${validator}.")
                                .build()
                )
        );
    }
}

ValidatorRegistry.add(getKotlinClass(Timestamp.class), new MyTimestampValidator());

var timestamp = Timestamp.newBuilder().setNanos(-1).build();
var violation = ValidatorRegistry.validate(timestamp).get(0);

var placeholder = ValidatorRegistry.VALIDATOR_PLACEHOLDER;
var validatorClass = violation.getMessage().getPlaceholderValueMap().get(placeholder);

// If the template references `${validator}`, it will be substituted here.
var text = TemplateStrings.format(violation.getMessage());
```
{{< /code-tab >}}

{{< /code-tabs >}}

{{% note-block class="note" %}}
The `validator` entry is always added to `placeholder_value`, even if the template does not
reference `${validator}`.
{{% /note-block %}}

## Clear the registry

To remove all validators for all message types, call `ValidatorRegistry.clear()`.

This API is typically useful in tests to ensure isolation between test cases.

{{< code-tabs langs="Kotlin, Java">}}

{{< code-tab lang="Kotlin" >}}
```kotlin
import io.spine.validation.ValidatorRegistry

ValidatorRegistry.clear()
```
{{< /code-tab >}}

{{< code-tab lang="Java" >}}
```java
import io.spine.validation.ValidatorRegistry;

ValidatorRegistry.clear();
```
{{< /code-tab >}}

{{< /code-tabs >}}
