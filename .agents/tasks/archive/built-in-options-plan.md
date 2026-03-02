# Task: Publish a reference set on built-in validation options

## Goal

Publish an actionable catalog of built-in *validation-related* options, organized by where
the option can be declared (field, `oneof`, message), and optimized for selecting the right
option for a use case.

## Placement and structure

- Placement: the "Built-in options" section comes after the "Concepts" section.
- Structure: implement "Built-in options" as a folder with a landing page and 4 child pages
  (not a single long page).
- Avoid having a separate page per option.

Proposed doc files under `docs/content/docs/validation/03-built-in-options/`:

- `docs/content/docs/validation/03-built-in-options/_index.md` (landing page)
- `docs/content/docs/validation/03-built-in-options/field-level-options.md`
  - Title: "Field-level options"
- `docs/content/docs/validation/03-built-in-options/oneof-fields.md`
  - Title: "Options for `oneof` fields"
- `docs/content/docs/validation/03-built-in-options/message-level-options.md`
  - Title: "Message-level options"
- `docs/content/docs/validation/03-built-in-options/repeated-and-map-fields.md`
  - Title: "Options for `repeated` and `map` fields"

Order of pages: exactly as listed above.

## Scope and source of truth

- Source of truth for what exists: `docs/_options/options.proto`.
- Scope: validation-related options only.
  - Exclude non-validation options (API annotations, entity metadata, etc.).
  - Exclude deprecated options (do not document them; do not add “deprecated catalogs”).
- Do not duplicate the full option documentation from `spine/options.proto`.
  - Prefer short, “how to choose” descriptions, and link out for authoritative details.

## Content principles (what each page should contain)

Each page should:

- Start with “When to use this page” and a small “Choose an option” list (use case → option).
- Group options by intent (presence, range, pattern, uniqueness, recursion, dependencies),
  not by field number order.
- Include 1–2 minimal examples per group (copy-pasteable `.proto` snippets).
- Include “Applies to” and “Common combinations / gotchas” where the behavior is easy to
  misunderstand (e.g., `(validate)` + default instances, uniqueness for collections, etc.).
- Link to `spine/options.proto` on GitHub when referencing the canonical definition.

## What options to cover (high-level checklist)

The catalog should cover (non-deprecated) validation-related options defined in
`docs/_options/options.proto`, including:

- Field-level:
  - Presence: `(required)`, `(if_missing)`
  - Numeric constraints: `(min)`, `(max)`, `(range)`
  - String/bytes constraints: `(pattern)`
  - Nested validation: `(validate)`
  - Cross-field dependency: `(goes)`
  - Immutability: `(set_once)`, `(if_set_again)`
  - Uniqueness for collections: `(distinct)`, `(if_has_duplicates)`
- `oneof`:
  - Required selection: `(choice)`
- Message-level:
  - Field-group requirements: `(require)`

Note: keep this list aligned with `docs/_options/options.proto` and update it if the proto
changes.

## External links

- Link to canonical `spine/options.proto` where appropriate:
  - https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto
