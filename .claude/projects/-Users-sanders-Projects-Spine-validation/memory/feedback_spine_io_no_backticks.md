---
name: Do not use backticks around "spine.io"
description: When writing docs in this repo, write spine.io as plain prose (or as a link), never wrapped in backticks.
type: feedback
---

Do not put backticks around `spine.io` — write it as plain prose or, where
appropriate, as a Markdown link to https://spine.io. The user removed
backticks I had added in `_index.md` and `build-tasks.md` and explicitly
asked me not to use them.

**Why:** "spine.io" is a brand/site name in this project, not code or a
file/directory identifier. The repo's documentation-guidelines say to format
file and directory names as code; brand names are not in that bucket.

**How to apply:** In all docs and prose under this repo, render "spine.io"
as `spine.io` (no backticks) or as `[spine.io](https://spine.io)`. The same
default likely applies to other brand/site names — keep them plain unless
they unambiguously refer to a file or identifier.
