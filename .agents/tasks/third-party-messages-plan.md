# Task: Document validating third-party (external) messages

## Goal

Explain how to enforce validation rules for Protobuf message classes that are **already generated**
by third parties and therefore cannot be updated via `.proto` option annotations + codegen.

Target outcome: a reader can pick the right strategy depending on whether they control the `.proto`
source, and can implement an external message validator that is automatically applied when
validating local messages.

## Placement

- Placement of the page: after “Built-in options”, before “Custom validation”.
- Hugo section (minimal change): add the page under `docs/content/docs/validation/03-built-in-options/`.
  If the site navigation later gains an “Advanced topics” section, the page can move there.

## Planned content

- Define “local” vs “external” messages:
  - Local: `.proto` sources compiled in the current build; Validation injects checks into generated code.
  - External: message classes already compiled (e.g., come from dependencies); codegen cannot be applied.
- What does and does not work:
  - You cannot attach Validation options to fields of external messages unless you rebuild their `.proto`.
  - External validators are applied **only when an external message is a field inside a local message**.
  - External validators are not applied transitively inside other external messages.
- Recommended strategy decision tree:
  - If you control `.proto`: prefer built-in options or custom validation options (codegen).
  - If you don’t control `.proto`: use `MessageValidator` + `@Validator`.
- How external validation works (high-level):
  - Implement `io.spine.validation.MessageValidator<M>`.
  - Annotate the implementation with `@io.spine.validation.Validator(M::class)`.
  - Ensure a `public` no-args constructor (required by discovery/instantiation).
  - Validation invokes the validator for:
    - singular fields of type `M`;
    - repeated fields of type `M`;
    - map values of type `M`.
- Error reporting shape:
  - Return `List<DetectedViolation>`.
  - Use `FieldViolation` (and other available violation types) to point at a field path and value.
  - Mention that the runtime converts `DetectedViolation` into `ConstraintViolation`/`ValidationError`.
- Constraints and guardrails:
  - Exactly one validator per external message type (duplicate is an error).
  - Validators for local messages are prohibited (use options/codegen instead).
- Example walkthrough (short, copy-pastable):
  - Implement `EarphonesValidator` (from `:tests:validator`) and show how it affects a local message
    that contains an `Earphones` field.

## Source references to anchor the docs

- External validation API and requirements:
  - `jvm-runtime/src/main/kotlin/io/spine/validation/MessageValidator.kt`
  - `jvm-runtime/src/main/kotlin/io/spine/validation/Validator.kt`
- Example implementation:
  - `main/validator/src/main/kotlin/io/spine/validation/TimestampValidator.kt`

## Output

Implemented as `docs/content/docs/validation/02-concepts/third-party-messages.md`.

