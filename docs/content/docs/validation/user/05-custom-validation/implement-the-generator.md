---
title: 'Implement the `Generator`'
description: How to implement an OptionGenerator that produces Java validation code.
headline: Documentation
---

# Implement the `Generator`

The `Generator` produces Java code for every application of the option within a compiled message
type. The framework calls `codeFor` once per message type and inlines each returned
`SingleOptionCode` into the generated `validate()` method.

## Class declaration

```kotlin
internal class WhenGenerator : OptionGeneratorWithConverter()
```

Use `OptionGeneratorWithConverter` when your generated code needs to convert a Protobuf field
value to a Java expression — for example, to format a field value as a JSON string in an error
message. This base class injects a `JavaValueConverter` that handles the conversion. Use the
plain `OptionGenerator` base class when no value conversion is needed.

## Querying the `View`

```kotlin
private val allWhenFields by lazy {
    querying.select<WhenField>().all()
}
```

`querying` is injected by the framework and provides read access to all accumulated `View`
instances. The `by lazy` delegate is required because `querying` is not available until the
framework initialises the generator; accessing it during construction causes an error. The
query result is cached after the first call.

## `codeFor` override

```kotlin
override fun codeFor(type: TypeName): List<SingleOptionCode> =
    allWhenFields
        .filter { it.id.type == type }
        .map { GenerateWhen(it, converter).code() }
```

The framework calls `codeFor` once for each message type it processes. Filter the view list
by `id.type == type` to select only the fields that belong to the current message. Each
filtered view is passed to a helper that composes the actual `CodeBlock`.

Each `SingleOptionCode` wraps a `CodeBlock` that is inlined directly into the generated
`validate()` method, so the code must be a valid Java statement or block.

For complete context, see [`WhenGenerator.kt`][when-generator-kt] in the Spine Time repository.

## Generated code paths

The `GenerateWhen.code()` method chooses the Java code shape for a single application of
the `(when)` option:

- For a single message field, it generates one validation block for the field value.
- For a repeated message field, it generates a `for` loop and validates each element inside
  that loop.
- For a map field, it generates a `for` loop over the map's `.values()` and validates each
  value inside that loop.

```kotlin
fun code(): SingleOptionCode = when {
    fieldType.isMessage -> validateTime(fieldValue)
    fieldType.isRepeatedMessage ->
        CodeBlock(
            """
            for (var element : $fieldValue) {
                ${validateTime(ReadVar("element"))}
            }
            """.trimIndent()
        )

    fieldType.isMap ->
        CodeBlock(
            """
            for (var element : $fieldValue.values()) {
                ${validateTime(ReadVar("element"))}
            }
            """.trimIndent()
        )

    else -> unsupportedFieldType()
}.run { SingleOptionCode(this) }
```
All three branches delegate to the same `validateTime(...)` helper, so the time comparison,
violation construction, and placeholder handling stay in one place. The difference is only
where the checked value comes from: the field getter for a single message, the loop variable
for each repeated element, or the map value variable for each map entry.

```kotlin
private fun validateTime(fieldValue: Expression<Any>): CodeBlock {
    val isTimeOutOfBound = when (view.type) {
        TFT_TIMESTAMP -> {
            val operator = if (view.bound == FUTURE) "<" else ">"
            "$TimestampsClass.compare($fieldValue, $SpineTime.currentTime()) $operator 0"
        }

        TFT_TEMPORAL -> {
            val checkBound = if (view.bound == FUTURE) "isInPast" else "isInFuture"
            "$fieldValue.$checkBound()"
        }

        else -> unsupportedFieldType()
    }
    return CodeBlock(
        """
        if (!${field.hasDefaultValue()} && $isTimeOutOfBound) {
            var fieldPath = ${parentPath.resolve(field.name)};
            var typeName =  ${parentName.orElse(declaringType)};
            var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), fieldValue)};
            $violations.add(violation);
        }
        """.trimIndent()
    )
}
```

See the full source around [`GenerateWhen.code()`][when-generator-kt] for more details 
on the implementation.

## What's next

- [Pass the option to the Compiler](pass-to-compiler.md)
- [Back to Custom Validation](_index.md)

[when-generator-kt]: https://github.com/SpineEventEngine/time/blob/master/validation/src/main/kotlin/io/spine/tools/time/validation/java/WhenGenerator.kt
