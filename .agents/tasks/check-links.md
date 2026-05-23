---
slug: check-links
branch: validate-doc-links
owner: claude
status: in-review
started: 2026-05-22
---

## Context

The Spine `validation` repo's Hugo site under `docs/` accumulates link rot
silently. A concrete example: `docs/content/docs/validation/developer/runtime-library.md`
references `error_message.proto` at a path that no longer exists in this repo —
the `TemplateString` type it describes actually lives in the sibling
`SpineEventEngine/base-libraries` repo. The same broken reference appears in
`architecture.md`. We need both a local pre-push check and a CI gate, mirroring
the well-tested setup in the sibling `spine.io` repo
(`/Users/sanders/Projects/Spine/spine.io/lychee.toml`,
`/Users/sanders/Projects/Spine/spine.io/.github/workflows/check-links.yml`).

The new skill is scoped to the Hugo Markdown docs only. A separate skill for
KDoc/Javadoc HTML is out of scope here and will be planned independently. The
`../config` fan-out is also out of scope — it will happen separately in that
repo.

## Decisions (approved 2026-05-22)

- **Lychee binary** — prefer installed, fall back to auto-download the version
  pinned by `LYCHEE_VERSION_TAG` in `check-links.yml` into
  `.agents/skills/check-links/.cache/lychee/`.
- **`pre-pr` wiring** — yes, wire it in.
- **Hugo server port** — 1414 (avoid clashing with developer's default 1313).

## Plan

### 1. Fix the two broken doc links

- [ ] Read both files end-to-end first to understand the surrounding prose:
  `docs/content/docs/validation/developer/runtime-library.md`,
  `docs/content/docs/validation/developer/architecture.md`.
- [ ] Verify the canonical GitHub URL for `template_string.proto` by browsing
  `https://github.com/SpineEventEngine/base-libraries/tree/master/src/main/proto/spine/string/`.
- [ ] In `runtime-library.md`: update the broken `error_message.proto` link to point at
  the `SpineEventEngine/base-libraries` URL for `template_string.proto`. Rename the
  reference id to `[template-string-proto]` and the prose mention from
  "`error_message.proto`" to "`template_string.proto`".
- [ ] In `architecture.md`: same fix.
- [ ] Sanity-check there are no other lingering `error_message.proto` mentions
  via `Grep`.

### 2. Add Lychee config at repo root

- [ ] Create `lychee.toml` as a near-copy of
  `/Users/sanders/Projects/Spine/spine.io/lychee.toml`. Keep its exclude
  list unchanged.

### 3. Add CI workflow

- [ ] Create `.github/workflows/check-links.yml`, adapted from
  `/Users/sanders/Projects/Spine/spine.io/.github/workflows/check-links.yml`.
- [ ] Diffs vs. source: `working-directory` → `docs`/`docs/_preview`;
  Lychee glob `'docs/public/**/*.html'`; `paths:` filter on `pull_request`
  for `['docs/**', '.github/workflows/check-links.yml', 'lychee.toml']`.
- [ ] Preserve pinned versions: `HUGO_VERSION=0.161.1`,
  `LYCHEE_VERSION_TAG=v0.15.1`. Preserve both caches.

### 4. Create `check-links` skill

- [ ] New file: `.agents/skills/check-links/SKILL.md`.
- [ ] Procedure: scope check → preflight binaries (Lychee prefer-installed,
  fallback to download) → install deps → Hugo build → Hugo serve on port
  1414 → Lychee against `docs/public/**/*.html` → tear down → report grouped
  by source `.md` → sentinel `.git/check-links.ok` on PASS.

### 5. Wire into `pre-pr` skill

- [ ] Edit `.agents/skills/pre-pr/SKILL.md`: add `docs/**` slice in the
  classifier; invoke `check-links` in parallel with `review-docs` when
  `docs/**` changed; include in verdict union; append to sentinel
  `reviewers=` field; add row to verdict table.

### 6. Update `.gitignore`

- [ ] Add `.agents/skills/check-links/.cache/` to `.gitignore` (Lychee binary cache).

### 7. Verification (end-to-end)

- [ ] **Local skill smoke test**: temporarily revert the link fix on a scratch
  commit; run `/check-links`; expect non-zero exit naming the broken URL
  and the source page. Re-apply the fix; rerun; expect clean exit.
- [ ] **CI smoke test**: push the branch; observe the new `check-links` job
  appear on the PR; it must be green with the fix applied.
- [ ] **pre-pr integration**: run `/pre-pr` on the branch; confirm the
  reviewers table now lists `check-links` and that the sentinel records
  it.

## Critical files

- `docs/content/docs/validation/developer/runtime-library.md` (link fix)
- `docs/content/docs/validation/developer/architecture.md` (link fix)
- `lychee.toml` (new, repo root)
- `.github/workflows/check-links.yml` (new)
- `.agents/skills/check-links/SKILL.md` (new)
- `.agents/skills/pre-pr/SKILL.md` (edit)
- `.gitignore` (edit — add `.agents/skills/check-links/.cache/`)

## Log
- 2026-05-22 — drafted, presented to user, approved with all three open
  questions resolved (Lychee fallback download / pre-pr wiring yes / port 1414).
- 2026-05-22 — execution started.
- 2026-05-22 — Hugo actually publishes to `docs/_preview/public/**`, not
  `docs/public/**` (the latter is a stale dir from an earlier config). Lychee
  glob updated everywhere.
- 2026-05-22 — User-supplied canonical URL is
  `SpineEventEngine/base-libraries`, not `SpineEventEngine/base`. Both docs
  updated; WebFetch confirmed HTTP 200.
- 2026-05-22 — Lychee v0.15.1 ships no arm64 macOS binary, so the skill's
  fallback download path can't work on Apple Silicon. Bumped the pin to
  `lychee-v0.24.2` in both `check-links.yml` and the skill. Asset names in
  that release drop the version-in-filename (e.g.
  `lychee-aarch64-apple-darwin.tar.gz`); release tag is `lychee-v0.24.2`.
  Extract with `--strip-components=1` so the binary lands at
  `.agents/skills/check-links/.cache/lychee/lychee`.
- 2026-05-22 — Verified end-to-end on macOS arm64: downloaded Lychee 0.24.2
  → `hugo` (extended 0.161.1) build → `hugo server --port 1414` → Lychee
  against `docs/_preview/public/**/*.html`. **616 links checked, 0 errors.**
- 2026-05-22 — Lychee 0.24.2 deprecated `--base`; switched to `--base-url`
  in both the workflow and the skill.
- 2026-05-22 — Self-review pass on SKILL + workflow + lychee.toml. Fixes:
  - SKILL: softened the "green here means green there" claim and listed the
    two deliberate differences (port 1414 vs 1313, local sentinel).
  - SKILL: aligned the build/serve commands to use `-e development` /
    `--environment development` so the skill renders exactly what CI renders.
    Replaced `docs/_script/hugo-build` with explicit `hugo -e development`
    (the helper script defaults to `production`).
  - SKILL: added Linux aarch64 row to the platform map; unsupported platforms
    (Windows / FreeBSD / 32-bit) now stop with a Must-fix asking the user to
    install Lychee manually instead of falling through silently.
  - SKILL: install the kill-trap **before** backgrounding the Hugo server so
    a failure between the subshell exit and the original trap line cannot
    leak a process holding port 1414. Noted the trap-doesn't-survive-Bash
    rule explicitly.
  - Workflow: added `cancel-in-progress: true` to `concurrency` so each PR
    push cancels the previous run instead of queueing.
  - Workflow: bumped `actions/setup-node@v3` → `@v4` to match the other
    actions which are already on `@v4`.
  - Workflow: rekeyed Lychee results cache to
    `cache-lychee-${{ runner.os }}-${{ hashFiles('lychee.toml') }}` with a
    `restore-keys:` fallback, so exclude-list edits invalidate cached `200`s
    deterministically. Included `LYCHEE_VERSION_TAG` in the binary cache key
    for the same reason.
  - Workflow: added `--strip-components=1` (and `-z`) to the tar extract so
    the binary lands at `lychee/lychee` instead of
    `lychee/lychee-<triple>/lychee` — matching the skill and what the next
    "Check links" step expects on a cold cache.
  - `lychee.toml`: removed the broad
    `raw.githubusercontent.com/SpineEventEngine/*` exclude (it would mask
    the very class of bug this skill exists to catch). Existing
    `max_retries=3` / `retry_wait_time=2` should absorb transient 429s; a
    comment in the file calls out the deliberate omission.
- 2026-05-22 — Second self-review pass. Fixes:
  - SKILL: the "same Hugo version, same Lychee version" claim used to be
    aspirational — neither value was pinned anywhere in this file. Added a
    "Pinned versions" subsection saying `check-links.yml` is the single
    source of truth, and rewrote step 2 so the auto-download path **reads
    `LYCHEE_VERSION_TAG` out of the workflow at runtime** via `grep|sed`.
    Hugo similarly read from the workflow's `HUGO_VERSION` for a "your
    Hugo is older than CI" warning. Drift between skill and CI is now
    impossible for the version pins.
  - SKILL: the previous trap-based teardown had a fatal bug — `trap … EXIT`
    fires when *this* shell exits, and Claude Code's Bash tool gives each
    invocation its own shell. So running step 5 (start server) and step 6
    (run Lychee) in separate Bash calls killed Hugo before Lychee could
    query it. Dropped the trap; switched to `nohup`-only (which detaches
    the server from the spawning shell). Step 5 now also `pkill -F`s any
    leftover server from a previous crashed run before launching. Step 8
    does the explicit kill with the same `pkill -F` (always runs, even on
    Lychee failure, so port 1414 isn't leaked).
  - Workflow: added `restore-keys: ${{ runner.os }}-${{ env.LYCHEE_VERSION_TAG }}-`
    fallback for the Lychee binary cache so a release-filename tweak can
    reuse the existing cached binary for the same version-tag instead of
    paying for a fresh download.
  - Workflow: made the Hugo server's `--port 1313` explicit so its coupling
    with the next step's `--base-url http://localhost:1313/` is visible at
    the call site. Comment in the workflow explains the lock-step.
  - `lychee.toml`: fixed `no_progress = false` → `true` (the comment said
    "don't show progress bar" but the value asked to show it — Lychee's
    `no_progress` semantics invert what one might expect).
  - `lychee.toml`: documented the `429` acceptance — it's a deliberate
    rate-limit tradeoff, not a typo, and the comment explains the rare
    downside (a genuinely-broken URL that also returns `429` would pass).
- 2026-05-22 — Third self-review pass. Fixes:
  - SKILL: dropped the concrete `HUGO_VERSION=0.161.1` /
    `LYCHEE_VERSION_TAG=lychee-v0.24.2` values from the preamble. They
    contradicted the "don't edit here" warning — duplicating pins is the
    very thing the "single source of truth" framing exists to prevent.
    Preamble now just points to `check-links.yml`'s `env:` block.
  - SKILL: step 3 replaced `docs/_script/install-dependencies` with inline
    `( cd docs/_preview && npm install )`. The helper script does a
    relative `cd _preview` so it only works from `docs/`; the skill's
    default CWD is the repo root, where the helper would fail with
    "No such file or directory: _preview". Annotated step 3 with the
    reason.
  - SKILL: step 5 captures `$!` to a pid file and then verifies the PID
    via `pgrep -F` before relying on it. Without this check a silent Hugo
    startup failure (port already bound, missing module, etc.) becomes a
    confusing "Lychee fetches an empty port" failure 30s later. Now it's
    a clear error with a log tail at the actual failure point.
  - SKILL: step 8's sentinel-consumer wording now describes the planned
    `pre-pr` short-circuit precisely — `head=` match + `status=PASS`
    skips re-dispatch and records APPROVE with note "cached from
    `.git/check-links.ok`". Avoids the "orphaned sentinel" issue
    where the skill claimed integration that didn't exist.
  - `pre-pr/SKILL.md`: implemented the sentinel short-circuit in step 4
    to make the SKILL.md claim true. Explicitly notes that other
    reviewers do not use this pattern (only `check-links` does,
    because its rebuild+serve cycle is slow and the result is
    deterministic for a given HEAD).
  - Workflow: removed the noisy "Check if the cache file exists"
    diagnostic step. It only existed as a debugging breadcrumb during
    initial bring-up and added log noise without any actionable signal —
    `actions/cache@v4` already reports cache-hit status.
  - `lychee.toml`: header comment now references the canonical
    `SpineEventEngine/SpineEventEngine.github.io` repo, not the bare
    `spine.io` shorthand.
- 2026-05-22 — Fourth self-review pass. Six fixes:
  - SKILL step 2 (Lychee auto-download): added an explicit
    `mkdir -p .agents/skills/check-links/.cache/lychee/` substep
    before the tar extract. On a fresh clone the cache dir does not
    exist (it is git-ignored), and `tar -xzf <asset> -C <dir>` would
    fail with "no such file or directory" — the previous
    end-to-end success worked only because the dir was already
    present locally. Renumbered the surrounding substeps.
  - SKILL step 3 (install Hugo deps): switched
    `( cd docs/_preview && npm install )` to `npm ci` to match the
    `Install Dependencies` step in `check-links.yml`. Rationale (in
    the SKILL): `npm install` may mutate the lockfile and resolve
    different transitive versions than CI; `npm ci` pins to
    `package-lock.json` and fails fast on lockfile drift instead of
    silently healing it.
  - Workflow: removed the standalone `Cache dependencies` step and
    folded its job into `actions/setup-node@v4` via
    `cache: 'npm'` + `cache-dependency-path:
    docs/_preview/package-lock.json`. One source of truth for the
    npm cache key, no drift between the two layers.
  - Workflow: the Hugo modules cache step pointed at
    `path: /tmp/hugo_cache`, but Hugo's default cacheDir on Linux
    is `~/.cache/hugo_cache` (or `$TMPDIR/hugo_cache_$USER`) —
    nothing ever landed in `/tmp/hugo_cache` and the cache was a
    silent no-op every run. Fixed by adding
    `HUGO_CACHEDIR: /tmp/hugo_cache` to the job `env:` block so
    Hugo actually writes where the cache step restores from. Also
    narrowed the cache key from `hashFiles('**/go.sum')` to
    `hashFiles('docs/**/go.sum')` since those are the only `go.sum`
    files in the repo and the broader glob was misleading.
  - Workflow: the Lychee `Download` and `Extract` steps gated on
    `steps.cache-lychee.outputs.cache-hit != 'true'`, which is only
    `'true'` on an EXACT key match. A restore via `restore-keys`
    reports `cache-hit == 'false'` per `actions/cache` docs, so the
    download would re-run even when the binary was present —
    defeating the fallback that was added in round 1. Switched the
    `if:` guards to `hashFiles('lychee/lychee') == ''`, which only
    triggers the download when the binary is actually absent.
    Comment in the workflow explains the rationale.
  - `lychee.toml`: the exclude entries are Rust regexes (per
    Lychee docs), not shell globs. The previous entries used
    unescaped dots and bare `*` (e.g. `fonts.googleapis.com/*`),
    which means "any char between `fonts` and `googleapis`,
    zero-or-more slashes at the end" — silent over-match that
    could mask real broken links to URLs of the same shape (e.g.
    `fontsXgoogleapisYcomZ/...`). Rewrote every pattern with
    escaped dots and `/.*` for the path suffix. Switched the TOML
    strings from double-quoted to single-quoted (literal strings)
    so the backslashes stay literal — TOML basic strings treat
    `\.` as an unrecognized escape. Added a header block above the
    `exclude = [` line documenting the regex semantics.
    **Empirically verified** with `lychee --dump`:
    `fonts.googleapis.com/css?family=Roboto` and `x.com/notify` are
    correctly excluded by the new config, `github.com/owner/repo`
    passes through. Without `--config`, all three pass — confirming
    the exclusion is firing from our patterns, not from Lychee
    built-ins.
- 2026-05-22 — Re-verified end-to-end after round-4 edits:
  `( cd docs/_preview && hugo -e development )` →
  `hugo server --environment development --port 1414` (PID survived
  across separate Bash calls; `pgrep -F` verification passed) →
  `lychee --config lychee.toml --base-url http://localhost:1414/
  'docs/_preview/public/**/*.html'`. **616 links, 0 errors, exit 0.**
- 2026-05-22 — Fifth self-review pass. Four fixes:
  - SKILL "Tooling" section (lines 67–72): removed the literal
    `v0.24.2` reference. It contradicted the "Pinned versions"
    subsection above (`check-links.yml` is the single source of
    truth) and would drift the moment the workflow pin is bumped —
    exactly the failure mode the dynamic `grep|sed` read in step 2
    was introduced to prevent. The Tooling sentence now points at
    `LYCHEE_VERSION_TAG` in the workflow.
  - SKILL "Notes" bullet about outside-`.git/` files: the previous
    claim ("only files outside `.git/` are `.cache/` and `/tmp/`")
    was wrong. The skill also creates `docs/_preview/node_modules/`
    (`npm ci`), `docs/_preview/public/` + `docs/_preview/resources/`
    (`hugo -e development`), and `.lycheecache` at the repo root
    (Lychee). All git-ignored (verified against `.gitignore` and
    `docs/.gitignore`), but listing them prevents future readers
    from mistaking a build artifact for an unexpected side-effect.
    Reworded "does not modify project sources" → "does not modify
    tracked sources" to be technically precise.
  - Workflow: the `Start Hugo server` step had no readiness check —
    only `nohup … & sleep 5`. A silent startup failure (port bind,
    missing Hugo module, build error after `nohup` returns 0)
    would surface 60 s later as "every URL unreachable" Lychee
    errors. Added a `curl -sf http://localhost:1313/` probe after
    the sleep that exits non-zero with a `cat nohup.out / nohup.err`
    dump if Hugo isn't actually serving. Mirrors the spirit of the
    skill's `pgrep -F` guard (strictly stronger, since `curl`
    catches "process alive but not serving" too).
  - `lychee.toml`: two exclude entries — `clients4\.google\.com/`
    and `ssl\.gstatic\.com/` — didn't have the trailing `/.*` that
    the header comment documents as the convention and that every
    other entry follows. They still matched via substring regex,
    but the inconsistency was a footgun for future edits. Aligned
    to `clients4\.google\.com/.*` and `ssl\.gstatic\.com/.*`.
    Empirically verified via `lychee --dump` (running from `/tmp`
    to bypass CWD config auto-discovery): both URLs are now
    excluded by the config, `github.com/owner/repo` still passes.
- 2026-05-22 — Re-verified end-to-end after round-5 edits:
  build → `hugo server --port 1414` → `pgrep -F` + `curl -sf
  http://localhost:1414/` both pass → Lychee 616/0/exit 0. The
  `--dump` probe confirms the two re-aligned exclude entries still
  filter their domains.
- 2026-05-22 — Rename pass: workflow `proof-links.yml` →
  `check-links.yml` (file + `name:` field `Proof Links` →
  `Check Links` + job key `proof-links:` → `check-links:`); skill
  `link-check-docs` → `check-links` (directory, frontmatter `name:`,
  all internal references); sentinel `.git/link-check-docs.ok` →
  `.git/check-links.ok` (skill + `pre-pr/SKILL.md` consumer);
  `.gitignore` comments + cache path; this task file's slug, body,
  and prior log entries.
- 2026-05-22 — Dropped all references to the future KDoc/Javadoc
  link-check skill from `SKILL.md` (description preamble + the
  "Related skills" bullet) and from this task file's Context
  section. Rationale: that skill doesn't exist yet, naming it now
  pre-commits the design before its own planning round.
