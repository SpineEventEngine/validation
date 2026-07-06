# Team memory index

One line per memory. Scan at the start of every session.
See [README.md](README.md) for the format and routing rules.

## Feedback (validated patterns & corrections)

- [copilot-review-request](feedback/copilot-review-request.md) — GraphQL `requestReviews` with `botIds: ["BOT_kgDOCnlnWA"]`; REST endpoint silently no-ops on re-requests.

## Project (durable context & rationale)

- [codegen-coverage-blind-spot](project/codegen-coverage-blind-spot.md) — Why the `java`/`context` codegen modules report ~0% coverage — they run out-of-process in the forked Spine Compiler JVM, which JaCoCo/Kover do not instrument.

## Reference (external systems)

- [cache-warm-window](reference/cache-warm-window.md) — How prompt cache entries are shared between sibling-repo sessions and how to maximise overlap.
- [anthropic-api-caching](reference/anthropic-api-caching.md) — Pattern and pricing for adding prompt caching to any direct Anthropic API call.
