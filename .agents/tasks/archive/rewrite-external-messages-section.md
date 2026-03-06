# Task: Rewrite the "External messages" section of the documentation
- New name for the section: "Using Validators"
- Purpose: explain how to use validators in code, including how to create and apply them to proto messages.
- Target outcome: a reader can create a validator for a proto message, apply it to an external
  message and to a local message likewise.
- Cases for using validators
  - External messages: the message the modification of source code is not possible (e.g. messages in libraries).
  - Validation of local messages that require computation or custom logic that cannot be expressed in proto options.
- Describe that multiple validations can be applied to the same message type.

## Describe creating a validator
- Implement `io.spine.validation.MessageValidator<M>`.
- Describe automatic discovered of validators using the `ServiceLoader` mechanism.
- Mention using `AutoService` to generate the service provider configuration file.
- Mention that it is possible to add a validator to `ValidatorRegistry` explicitly during
  application startup or configuration. This is an alternative to service discovery approach.
- Describe the requirement for a public no-args constructor.

## Keep the `implement-an-external-validator.md` page
- Rename the file to `implement-a-validator.md`.
- Remove using the `@Validator` annotation from the example, and instead show using `@AutoService`.
- Add the page to `sidenav.yml`.
