# Target audience

Spine Validation is designed for developers who model data and APIs using
**Protocol Buffers** and need a structured, type-safe way to express and enforce
constraints on that data.

The library serves several overlapping groups:

## 1. Application developers

Developers building backend services in **Java** or **Kotlin** who need
validation for:

- incoming API requests (REST / gRPC),
- commands and events,
- domain objects stored in persistence,
- user-generated data.

These developers benefit from:

- concise `.proto`-based validation rules,
- generated validation code,
- unified constraints across all layers of the application.

This group typically works directly with generated message builders and calls
`validate()` at appropriate points in their workflow.

## 2. Teams using the Spine Event Engine

Spine Validation is the standard validation mechanism used inside the
**Spine Event Engine**.  
Teams who adopt Spine for CQRS/ES applications already rely on the runtime to
validate:

- commands,
- events,
- value objects within aggregates and processes.

For Spine users, this library provides:

- consistent validation semantics across the entire model,
- rich domain-focused constraints,
- cross-field and message-level validations.

## 3. Framework and platform integrators

Engineers who build frameworks, platforms, or infrastructure around Protobuf
often need to enforce rules across:

- transport layers,
- boundary adapters,
- persistence and serialization,
- API gateways.

For these integrators, Spine Validation provides:

- predictable validation lifecycle,
- well-structured diagnostics (`ValidationException`, error trees),
- hooks for custom validators,
- extension points via custom validation options.

## 4. Library authors and tooling developers

Those who maintain shared data models across a large organization can use Spine Validation to:

- standardize validation behavior for all services,
- define custom domain-specific validation options,
- generate validator code for multiple runtime targets.

This group interacts with advanced parts of the library such as:

- validation model configuration,
- option policies,
- custom code generation units.


## 5. Not the target audience (explicitly)

The library is **not** designed for:

- UI-level form validation,
- general-purpose validation of arbitrary POJOs,
- Java Bean Validation (Jakarta Validation) replacement at the presentation layer,
- JSON-schema or OpenAPI validation.

Spine Validation is intentionally focused on the **Protobuf → runtime code**
pipeline and the domain layer of applications.


## Summary

If you:

- model your data in Protobuf,
- generate Java/Kotlin code,
- and want to enforce clear, versionable, and strongly typed validation rules,

then Spine Validation is designed for you.
