---
name: Backtick tool/repo names when used as link text to their repository
description: In Validation docs, render repo/tool identifiers (site-commons, embed-code-go, …) as inline code when they appear as link text pointing to their repository.
type: feedback
---

When a repo or tool identifier — for example `site-commons`, `embed-code-go`,
or a similar slug — appears as Markdown link text whose target is the
project's repository, format the link text as inline code:

- Yes: `[`site-commons`][site-commons]`
- No:  `[site-commons][site-commons]`

**Why:** These are identifiers (repo names, package names, executable
names), so the documentation guidelines' rule of "format file and directory
names as code" extends naturally. Reviewers explicitly asked for this after
I had left such link text unformatted.

**How to apply:** When you introduce a new link to one of these
repositories — or to any similarly named tool — wrap the visible link text
in backticks. Contrast with brand/site names like spine.io (see
"Do not use backticks around spine.io"), which stay plain prose.

In tables and other places where the same name appears outside a link, the
existing rule still applies: format identifiers as inline code.
