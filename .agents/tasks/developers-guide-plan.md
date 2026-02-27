# Task: Complete Developer’s guide (architecture and key modules)

## Goal

Make the “Developer’s guide” pages sufficient for maintainers and contributors:
the reader should understand the compilation pipeline, where each responsibility lives,
and where to start when debugging a doc/codegen/runtime issue.

## Placement

- Placement of the pages: `docs/content/docs/validation/09-developers-guide/`.
- Expand the existing pages without adding many new sections (keep it minimal).

## Planned content

- Architecture page (`architecture.md`)
  - Add a legend / step-by-step explanation for the existing diagram:
    - where options are discovered;
    - where the validation model is built (policies/views);
    - where Java code is generated/injected;
    - what artifacts flow between stages (descriptors, generated sources, resources).
  - Call out the main extension points and where they plug in:
    - custom options (reaction/view/generator + SPI);
    - external message validators (`MessageValidator` + `@Validator` + KSP discovery).
  - Add “Where to look” links:
    - built-in options reference;
    - custom validation section;
    - key modules page.

- Key modules page (`key-modules.md`)
  - Keep the tables, but add a 1–2 paragraph “debugging map”:
    - build-time problems (compiler/plugin) vs runtime problems (generated code / runtime library).
  - Add a short list of “common entry points” by scenario:
    - option semantics or compilation errors → `:context`, `:java`, `:gradle-plugin`;
    - validator discovery problems → `:ksp`, `:java`;
    - error message formatting → `:jvm-runtime`.

## Source references to anchor the docs

- Existing pages:
  - `docs/content/docs/validation/09-developers-guide/architecture.md`
  - `docs/content/docs/validation/09-developers-guide/key-modules.md`
- External message validator mechanism (for cross-linking):
  - `jvm-runtime/src/main/kotlin/io/spine/validation/MessageValidator.kt`
  - `jvm-runtime/src/main/kotlin/io/spine/validation/Validator.kt`

## Output

- Update: `docs/content/docs/validation/09-developers-guide/architecture.md`.
- Update: `docs/content/docs/validation/09-developers-guide/key-modules.md`.

