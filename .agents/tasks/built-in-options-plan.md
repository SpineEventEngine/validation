# Task: Publish a minimal reference set on built-in validation options
- Placement: a separate section comping after the "Concepts" section.
- From `docs/_options/options.proto`,
  enumerate the built-in options and group them (fields, strings, numbers, collections, message-level).
- For each documented option: purpose, supported field types, common pitfalls, and a short `.proto` example.
- Start with the options already used in docs/examples: `(required)`, `(pattern)`, `(min)/(max)`,
  `(distinct)`, `(validate)`.
