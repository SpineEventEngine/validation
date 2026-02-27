# Task: Publish a reference set on built-in validation options
- Placement: a separate section coming after the "Concepts" section.
- From `docs/_options/options.proto`,
  enumerate the built-in options and group them (fields, strings, numbers, collections, message-level).
- We don't need to repeat the documentation of the options available from
  the source code of `spine/options.proto`.
- The option catalog should help navigate to the relevant options for a particular use case,
  but it should not be a copy of the documentation of an option.
- If needed, we can have several pages under the "Built-in options" section,
  but we should avoid having a separate page for each option.
- Pages that we can have:
  - Field-level options (required, pattern, min/max, etc.)
  - Options for `oneof` fields.
  - Message-level options
  - Options for `repeated` and `map` fields.
- Give the link to the `spine/options.proto` source code at GitHub where appropriate.
  - https://github.com/SpineEventEngine/base-libraries/blob/master/base/src/main/proto/spine/options.proto

