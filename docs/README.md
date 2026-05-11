# Spine Validation — `docs/` module

This directory hosts the source of the Spine Validation documentation.
It is a Hugo project supplemented by Gradle wiring and a Go-based
code-embedding tool. The content under `content/` and `data/` is the
contributor-editable source; the rest of the directories — `_preview/`,
`_examples/`, `_time/`, `_settings/`, `_script/`, `_bin/`, `_options/` —
support local rendering, code embedding, and the example projects the
documentation references.

The `docs/` module is a **staging area**, not a published site. Content
under `content/` is merged into the main documentation project at
[SpineEventEngine/documentation][main-documentation] and published from
there to the spine.io website.

For the full technical description of the documentation pipeline — the
Gradle tasks, the embedded-examples mechanism, the external tooling, and
the recurring contributor procedures — see the in-repository overview at
[content/docs/validation/developer/documentation/_index.md][overview].

[main-documentation]: https://github.com/SpineEventEngine/documentation
[overview]: content/docs/validation/developer/documentation/_index.md
