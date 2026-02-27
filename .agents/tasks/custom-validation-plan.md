# Task: Expand “Custom validation” docs (custom options + codegen)

## Goal

Turn the existing “Custom validation” landing page into a complete, end-to-end guide for
implementing organization-specific rules via custom Protobuf options and code generation.

Target outcome: a reader can define a custom option, register it, implement reaction/view/generator,
and verify it works in a consumer project.

## Placement

- Placement of the pages: `docs/content/docs/validation/08-custom-validation/`.
- Keep the current landing page as an overview and add a single practical walkthrough page.

## Planned content

- Clarify the extension surface:
  - Custom Protobuf option definition (`extend google.protobuf.*Options`).
  - Option discovery + validation (reaction).
  - Accumulating a model (views, plus policies if needed).
  - Generating/injecting Java validation code (generator).
- Make the steps actionable:
  - Show file/Gradle locations where each piece belongs in a consumer project.
  - Explain registration points:
    - `io.spine.option.OptionsProvider`
    - `io.spine.tools.validation.java.ValidationOption` (SPI for custom option implementations)
- Provide a minimal “walkthrough” with the existing `(currency)` sample:
  - Point to the option declaration, the reaction/view/generator, and the registration.
  - Explain the intended contract: what rule is enforced, where the error message comes from.
- Add a short troubleshooting section:
  - Option not discovered (missing `OptionsProvider`).
  - Generator not invoked (missing `ValidationOption` SPI entry).
  - Illegal option application should fail compilation (where to look for error messages).

## Source references to anchor the docs

- Existing overview page:
  - `docs/content/docs/validation/08-custom-validation/_index.md`
- Full working example:
  - `:tests:extensions` module (custom `(currency)` option implementation)

## Output

- Update: `docs/content/docs/validation/08-custom-validation/_index.md`.
- Add: `docs/content/docs/validation/08-custom-validation/currency-example.md`.

