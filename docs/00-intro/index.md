# Overview

Spine Validation is a Protobuf-centric validation framework that generates
type-safe validation code directly from your `.proto` definitions.  
It allows you to describe constraints on fields, messages, and collections using
declarative options and then automatically enforces these constraints at
runtime.

The library is part of the Spine toolchain but can also be used independently
in any Java/Kotlin backend that models data using Protocol Buffers.

---

## Key Capabilities

### 🔹 Declarative constraints in `.proto`
Validation rules are expressed as Protobuf options such as:

- `required`
- `min` / `max`
- `pattern`
- `when.in = PAST | FUTURE`
- cross-field rules and message-level constraints

This keeps validation close to the data model and ensures it evolves together
with it.

### 🔹 Generated validators
The Spine compiler plugin processes your Protobuf model and generates:

- validation code for messages and builders,
- runtime checks,
- detailed error reports with field paths and messages.

No manual validators, reflection, or annotations are required.

### 🔹 Runtime validation API
Every generated message can be validated at runtime via:

- `validate()`,
- builder-based validation during construction,
- fail-fast or aggregated error reporting.

Errors are represented as structured diagnostics suitable for API responses,
logs, or domain exception flows.

### 🔹 Rich domain-oriented constraints
Beyond simple “required/min/max”, the library includes:

- collection rules (`distinct`, `non_empty`),
- nested and cross-field validation,
- advanced string formats (using regex),
- temporal constraints (`PAST`, `FUTURE`).

### 🔹 Extensible architecture
Teams can define custom validation options by:

- declaring new `.proto` options,
- providing model configuration,
- contributing custom validation logic and code generation.

This allows entire organizations to standardize domain validation rules.

---

## When to Use Spine Validation

Use Spine Validation if:

- your data model is defined in Protobuf,
- you want robust validation with compile-time guarantees,
- you want validation rules to be versioned and reviewed along with the model,
- you need consistent semantics across services and APIs,
- you prefer generated code over reflection-based validators.

It is especially useful in:

- backend services (Java/Kotlin),
- event-driven and CQRS systems,
- systems with rich domain models,
- multi-service environments where shared `.proto` models are common.

---

## Relationship to the Spine Event Engine

Within the Spine Event Engine, this library is the canonical way to validate:

- incoming commands,
- events,
- entity states,
- domain value objects.

Validation occurs at well-defined points in the lifecycle and integrates with the
framework’s error reporting mechanisms.

However, the library is fully standalone and can be used without the rest of the
Spine stack.

---

## What’s Next

- [Target Audience](target-audience.md)
- [Philosophy](philosophy.md)
- [Getting Started](../01-getting-started/index.md)
