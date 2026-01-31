# Philosophy

Spine Validation is built on a small set of principles that shape how developers
model, generate, and enforce constraints on their data.  
This section outlines those principles and explains why the library is designed
the way it is.


## 1. Validation belongs to the model

In many systems, validation rules drift between:

- UI forms,
- REST controllers,
- service layers,
- persistence layers,
- scattered hand-written validators.

Spine Validation takes the opposite approach:

> **The source of truth for validation must be the data model itself.**

By placing constraints in `.proto` definitions, validation becomes:

- versioned,
- reviewable,
- testable,
- shared across all services,
- consistent across languages and platforms.

This prevents the typical “broken windows” of duplicated or inconsistent
validation logic.


## 2. Generated code over reflection

Reflection-based frameworks (Jakarta Validation, etc.) are convenient but come with:

- runtime penalties,
- fragile metadata conventions (annotations, naming),
- limited static guarantees,
- late detection of misconfiguration.

Spine Validation uses **compile-time code generation**.

Benefits:

- fast, predictable runtime without reflection,
- validation logic is type-safe,
- errors in validation configuration are caught early,
- generated validation code integrates naturally into the Protobuf builder model.

This approach scales better for complex domain models or high-throughput services.

## 3. Declarative, not imperative

Validation is expressed declaratively through Protobuf options:

```proto
message Name {
    string value = 1 [(required) = true, (pattern).regex = "^[A-Za-z ]+$"];
}
```
Declarative rules:
 * are concise,
 * are tool-friendly,
 * reduce boilerplate,
 * improve readability,
 * align with Protobuf’s design philosophy.

Developers describe intent, and the library handles implementation.

## 4. Predictability and consistency

Validation should behave the same regardless of:

 * framework (Spring, Ktor, Micronaut),
 * transport (REST, gRPC),
 * service boundaries,
 * the developer writing the code.

Spine Validation is built around deterministic rules and error structures.
Given the same inputs, you always get the same:

 * error messages,
 * error paths,
 * semantics of constraints.

Consistency is especially important in distributed systems and domain-driven
design contexts.

## 5. Domain-oriented constraints

Instead of focusing only on primitive checks (`min`/`max`, `required`), the library
embraces **domain semantics**, such as:

 * cross-field logic,
 * nested validation,
 * constraints on identity fields,
 * collection semantics.

## 6. Extensibility as a first-class feature

Spine Validation is extensible via:

 * custom Protobuf options,
 * custom validation policies,
 * custom code generation units.

This makes the library a foundation for building consistent validation standards
across teams and services.

## 7. No UI or presentation layer concerns

Spine Validation intentionally does not attempt to validate UI forms,
front-end models, or JSON schemas.

Its focus is entirely on:

```
Protobuf → generated Java/Kotlin/TypeScript → domain logic
```

Everything else (frontend validation, OpenAPI, view models) should build on top
of API responses and field-level error metadata, not duplicate rules.

## Summary

Spine Validation is principled around:

 * model-first validation,
 * compile-time generation,
 * predictability,
 * domain semantics,
 * controlled extensibility.

This makes it suitable for teams that need reliable, maintainable,
and evolvable validation for complex systems built on top of Protobuf.
