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

## Inner-class pattern

`GenerateWhen` is a private nested class that separates two concerns:

- The generator (`WhenGenerator`) decides *which* view entries to process.
- The nested class (`GenerateWhen`) decides *how* to turn one view entry into a code string.

This pattern keeps `codeFor` readable when the code generation logic is non-trivial. For simple
options, the nested class can be replaced by a local function or lambda.

## What's next

- [Back to Custom Validation](../)
- [Architecture](../09-developers-guide/architecture.md)
