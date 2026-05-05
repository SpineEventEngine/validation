# Writing Developer's Guide Plan

This plan outlines the steps required to write a comprehensive Developer's Guide
for the Spine Validation project.

## Audience and scope

The Developer's Guide is for contributors to the Validation library itself, and for
readers who want a deep understanding of how the library works internally. It is
distinct from the User's Guide (under `docs/content/docs/validation/00-intro` …
`05-custom-validation`), which targets consumers of the library.

The guide lives under `docs/content/docs/validation/06-developers-guide/`.

## Sections

### 1. Overview and audience

Explains who this guide is for, how it complements the User's Guide, and the
high-level mental model (compile-time codegen + runtime checks). Sets expectations
about prerequisite knowledge (Protobuf, Gradle, Spine Compiler).

### 2. Architecture

Replaces the previously deleted `architecture.md` with an up-to-date description:

- The compile-time vs. runtime split.
- How `:context` (language-agnostic validation model), `:java` (Java codegen via
  Spine Compiler plugin), and `:jvm-runtime` (runtime APIs and error types)
  collaborate.
- The role of `:java-bundle` and `:gradle-plugin` in distribution.
- A diagram showing the flow: `.proto` + options → `:context` model → `:java`
  generator → generated validation code → `:jvm-runtime` APIs at execution time.

### 3. Key modules

Already drafted in `key-modules.md`. Keep current; add cross-links from sections 2
and 4–7 as those sections are written.

### 4. Adding a new built-in validation option

Contributor-side counterpart to the User's Guide `05-custom-validation/` section.
Where the User's Guide explains how a *consumer* adds a custom option to their
own project, this section explains how a *contributor* adds a new **standard**
option to the Validation library. Outline:

- Declaring the option in `spine/options.proto` in the `base-libraries` repo
  (link to that repo and to `options.proto`); versioning/coordination notes.
- Modeling the option in `:context`: the corresponding view, events, and
  reactions.
- Implementing code generation in `:java`.
- Adding runtime support in `:jvm-runtime` if the option needs new runtime
  helpers or error types.
- Writing tests across `:context-tests`, `:tests:vanilla`, and any specialized
  `:tests:*` modules.
- Documenting the new option in the User's Guide `03-built-in-options/`.

### 5. The validation model in `:context`

Deep dive into the language-agnostic model:

- Views, events, and reactions; how built-in options are translated into model
  state.
- How custom options plug in via the `ValidationOption` SPI from the model side.
- Error reporting conventions inside the model.

### 6. Java code generation in `:java`

How the Spine Compiler plugin in `:java` produces validation code:

- Plugin entry points and lifecycle.
- How the model from `:context` drives generation.
- `ValidationOption` SPI from the codegen side: how a custom option contributes
  generated code.
- Conventions for the shape of generated validators.

### 7. Runtime library `:jvm-runtime`

What ships in `:jvm-runtime` and how generated code uses it:

- `MessageValidator`, validation/constraint APIs, `ValidationException`.
- Error Protobuf types and how violations are surfaced.
- Extension hooks available at runtime, including `@Validator`.

### 8. Extension points (deep dive)

Internals behind the public extension surface, complementing the User's Guide
`04-validators/` and `05-custom-validation/`:

- `@Validator` and the validator registry: discovery, ordering, lifecycle.
- `ValidationOption` SPI end-to-end (cross-references sections 5 and 6).
- Constraints on what extensions can and cannot do, and why.

### 9. Testing strategy

Map of the test modules and when to add to which:

- `:context-tests` — Prototap-based compilation tests for the model.
- `:tests:vanilla` — baseline integration without custom extensions.
- `:tests:extensions`, `:tests:consumer`, `:tests:consumer-dependency` — custom
  options and consumer-side scenarios.
- `:tests:validator`, `:tests:validator-dependency` — `@Validator` scenarios.
- `:tests:runtime`, `:tests:validating` — runtime behavior and shared fixtures.
- Guidance on choosing the right module for a new test.

### 10. Build, packaging, and release

How the multi-project build is wired and how artifacts are produced:

- Gradle multi-project layout; relationship between `:java`, `:java-bundle`, and
  `:gradle-plugin`.
- Why `:java-bundle` exists (fat JAR for compiler plugin distribution).
- Version flow and how the Gradle plugin is consumed by downstream projects.

## Conventions

- **Depth of code walkthroughs**: sections 5–7 use short snippets paired with
  links to the source code. No long inline listings — the source is the source
  of truth.
- **Diagrams**: section 2 (and any later section that benefits) uses Mermaid.
  Pick the Mermaid diagram type that best fits the content (flowchart, sequence,
  class, etc.).

## Out of scope

- A reference for standard validation options. The User's Guide
  `03-built-in-options/` already covers this; the Developer's Guide instead
  teaches contributors how to add new standard options (section 4).
- Contributing workflow / coding standards. These remain in `CONTRIBUTING.md`
  and the `.agents` directory; the Developer's Guide does not duplicate them.

## Execution order

1. Section 2 (Architecture) — anchors everything else.
2. Section 1 (Overview) — short, written after section 2 so framing is accurate.
3. Sections 5, 6, 7 — internals, in dependency order.
4. Section 8 — builds on 5–7.
5. Section 4 — the "adding a new built-in option" walkthrough, which exercises
   sections 2 and 5–7.
6. Sections 9 and 10 — testing and build/release.
7. Update `_index.md` and `docs/data/docs/validation/2-0-0-snapshot/sidenav.yml`
   as each section lands.
