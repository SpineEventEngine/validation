---
name: Do not replicate upstream tool documentation in Validation docs
description: When the Validation docs touch an external tool (embed-code-go, site-commons, …), point at the upstream README — don't restate build/test/release recipes.
type: feedback
---

When the Validation documentation describes an external tool — for example
`embed-code-go` or `site-commons` — link to the upstream README for build,
test, and release details. Do not restate those instructions in this repo's
docs.

**Why:** Upstream is the source of truth and changes independently. Copying
its recipes here creates drift the moment upstream changes its build
command, Go version requirement, or output paths. The user specifically
called this out after I had inlined the full `embed-code-go` cross-compile
sequence into "Procedures" — they pointed out that the Validation build does
not compile `embed-code-go`, so a fetch-and-drop recipe is the only thing
this repo should document.

**How to apply:** For each external tool, write what *this* repository does
with it (where binaries land, which file pins the version, which Gradle
task consumes it) and link to the upstream README/section for everything
else. Default to "fetch the prebuilt artifact" over "build from source"
when the upstream publishes binaries.
