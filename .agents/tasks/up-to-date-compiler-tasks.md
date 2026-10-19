---
slug: up-to-date-compiler-tasks
branch: write-settings-before-launch (Changes 1–2)
owner: claude
status: in-progress
started: 2026-10-19
---

## Goal

`LaunchSpineCompiler` stays `UP-TO-DATE` when nothing it depends on has changed,
also in a fresh Gradle daemon and after `clean`. It reruns when a settings DSL
value changes. Two runs over the same inputs produce byte-identical sources.

## Context

Follow-ups from SpineEventEngine/compiler#94 ("Stop the integration-test build
from recompiling the root modules").

Findings (verified 2026-10-19 on Gradle 9.6.1):

- `LaunchSpineCompiler.settingsDirectory` (`build/spine/compiler/settings`) is an
  `@InputFiles` property shared by all source sets of a project. The `compiler`
  Gradle API has no way for other plugins to contribute settings; they write
  files there directly.
- Validation writes `JavaValidationRenderer.pb.json` in a `doFirst` of every
  `LaunchSpineCompiler`, i.e. after Gradle has fingerprinted the inputs.
  In `core-jvm-compiler :annotation`:
  - same daemon: next run `UP-TO-DATE` (the daemon's VFS keeps the snapshot
    taken before `doFirst`, which hides the problem);
  - fresh daemon: reruns with "file …JavaValidationRenderer.pb.json has been added";
  - after `clean`: the cache key holds the 7 CoreJvm files only; `FROM-CACHE`
    skips `doFirst`, so the renderer settings never affect the cache key.
  - Changing the DSL without `clean` leaves the task `UP-TO-DATE` with stale
    settings (minimal repro).
- CoreJvm's `writeSpineCompilerPluginsSettings` declares the whole settings
  directory as `@OutputDirectory` and its DSL options as `@Internal`. With no
  inputs it stays `UP-TO-DATE` when the DSL changes (minimal repro).
- Generated sources are non-deterministic for two reasons:
  1. `mangled()` appends `SecureRandomString` suffixes
     (`PatternGenerator`, `RequireOptionGenerator`).
  2. Generators iterate `select<View>().all()`. `core-jvm`'s `TenantRecords`
     stores records in a `HashMap` keyed by proto IDs, whose `hashCode()` mixes
     in the descriptor's identity hash. In `compiler :api`, 35 of 39 differing
     files differ only by the order of constraint blocks.
- `Pipeline.generateId()` (compiler) also uses `SecureRandomString`, but the ID
  never reaches the generated output. Out of scope.

## Plan

### Change 1 — `validation/gradle-plugin`: write settings before fingerprinting
- [x] Add a `WriteValidationSettings` task (`writeValidationSettings`):
  - `@Input Property<Boolean>` lazily wired to
    `validation.java.suppressWarnings.unsignedFields`;
  - `@OutputFile` = `<compilerWorkingDir>/settings/io.spine.tools.validation.java.JavaValidationRenderer.pb.json`;
  - builds `JavaValidationRendererSettings` and serializes it in `@TaskAction`
    (the JSON printer needs the plugin context class loader);
  - `@DisableCachingByDefault`; `mustRunAfter(clean)`.
- [x] Replace the `doFirst` with `dependsOn(writer)` on every `LaunchSpineCompiler`
  (still only when `validation.enabled`).
- [x] Update the KDoc that explains the `doFirst` choice, and the
  `ValidationGradlePluginSpec` class KDoc.
- [x] `ProjectBuilder` specs: the task is registered, wired to the launch tasks,
  and its input reflects the DSL value.
- [x] Version bump.

### Change 2 — `validation/java`: deterministic generated code
- [x] `mangled()`: derive the suffix from SHA-256 of the identifier (JDK
  `MessageDigest`, base64url, Java-identifier chars only, same length: `length`
  counts digest bytes, as it counted random bytes before). Keep the
  signature; KDoc states that the same identifier yields the same name.
- [x] Sort the views each generator selects by declaration order
  (`Field.orderOfDeclaration`; oneof order for `(choice)`), including
  `SetOnceRenderer`.
  - `(choice)`: the `OneofGroup` in `ChoiceOneof` has no fields (the
    `OneofOptionDiscovered` event carries only the group header), so the order
    comes from `MessageType.oneof_group` of the declaring type.
- [x] Specs for `mangled()` (deterministic, valid identifier, distinct inputs →
  distinct names) and for the declaration order of generated constraints
  (`JavaIdentifiersSpec`; `DeclarationOrderIgTest` in `tests/validating`).
- [x] Drop the `SecureRandomString` import from Validation.
- [x] Version bump: shares `.466` with Change 1 (same branch).

### Change 3 — `core-jvm-compiler/gradle-plugin`: settings writer picks up DSL changes
(after Validation with Changes 1–2 is published)
- [ ] `WriteCompilerPluginsSettings`: declare the exact `@OutputFiles` instead of
  the whole directory, so it no longer overlaps Validation's file.
- [ ] Make the task always run (`outputs.upToDateWhen { false }`). Writing is
  cheap, and `LaunchSpineCompiler` fingerprints content, so it still stays
  `UP-TO-DATE` when the content is unchanged (verified in a minimal repro).
  Modelling every DSL option as `@Input` was rejected: proto-valued options would
  need a deterministic serialization at fingerprinting time.
- [ ] `mustRunAfter(clean)`.
- [ ] Bump the Validation dependency.
  - Released CoreJvm (`.097`) cannot run with Validation `.466` yet: the Compiler
    fat CLI it uses bundles the Protobuf `4.36.0` runtime, while Validation's
    gencode is `4.36.2` (`ProtobufRuntimeVersionException` on loading
    `JavaValidationRendererSettings`). Rebuilding CoreJvm against `.466` fixes it.
- [ ] TestKit regression in `CoreJvmPluginIgTest`: run `launchSpineCompiler`
  twice with `--no-watch-fs` (so the VFS cannot hide the problem) and expect
  `UP-TO-DATE`; change a CoreJvm and a Validation DSL value and expect a rerun
  with the new settings; `clean` + run + run → `UP-TO-DATE`.
- [ ] Version bump.

### Verification (each change)
- [ ] `./gradlew build` (and `dokkaGenerate` for code changes).
- [ ] In a consumer: `launchSpineCompiler`, `./gradlew --stop`,
  `launchSpineCompiler --info` → `UP-TO-DATE`.
- [ ] Determinism: `:api:launchSpineCompiler --rerun` twice in `compiler`,
  `diff -r` of `api/generated` → no differences.

### Follow-ups (not in this plan)
- A settings API in `compiler` (one writer, `@Input` map consumer ID → content),
  replacing per-plugin writers.
- Deterministic `select().all()` ordering in `compiler`/`core-jvm` for all
  Compiler plugins.
- Review the names of the integration tests under `tests/` (`*ITest`) against
  the `XIgTest` convention in `.agents/guidelines/testing.md`.

## Log
- 2026-10-19 — analysis done; scope chosen: Validation + CoreJvm writer; plan drafted, awaiting approval.
- 2026-10-19 — approved; Change 1 started on `write-settings-before-launch` (based on `origin/master`, version `.465`).
- 2026-10-19 — Change 1 implemented (uncommitted, except the `.466` bump commit). `./gradlew build` and `:gradle-plugin:dokkaGenerate` pass. A standalone consumer of the published `.466` plugin stays `UP-TO-DATE` across `--no-daemon` runs, reruns when the DSL value changes, and works after `clean`; on `.465` the second run reruns with "…JavaValidationRenderer.pb.json has been added". `:docs:embedCode` can't run here (x86-64 binary, no Rosetta); `build-and-release.md` updated by hand.
- 2026-10-19 — reviews (Kotlin, Spine rules, Gradle, docs) addressed: the task owns the
  consumer ID and file name, takes an `@Internal` settings directory (set lazily) and
  derives its `@OutputFile`; properties are `internal`; a spec pins the `@Input`/`@OutputFile`
  declarations. With CoreJvm `.097` + Validation `.466`, both writers coexist with
  `processResources`/`sourcesJar` (no overlap or implicit-dependency warnings, fresh rerun
  `UP-TO-DATE`, `clean` OK); the compiler run itself is blocked by the Protobuf mismatch above.
  The automated end-to-end (TestKit) check stays in Change 3.
- 2026-10-19 — session resumed; `./gradlew build` re-run green on the final sources. Change 1
  committed (code + specs, then reports + docs). The `config`, `docs/_time`, and `docs/_examples`
  submodule pointers drifted during the session and stay uncommitted: `docs/_examples` was
  rewound to its `master` tip, behind the recorded `align-protobuf-runtime` commit.
- 2026-10-19 — Change 2 implemented on the same branch (uncommitted). Determinism check over
  the 9 test-module Compiler tasks that have protos (`--rerun`, `diff -r` of
  `tests/*/generated`): 0 differing files (52 with the `java/src/main` changes reverted).
  `DeclarationOrderITest` fails on the old code and passes on the new one.
- 2026-10-19 — reviews (Kotlin, Spine rules, docs) addressed: a `(min)` case added to
  `DeclarationOrderITest`, `(choice)` positions looked up with `getValue`, `length` checked
  before hashing, KDoc wording. `clean build` and `dokkaGenerate` green; determinism
  re-check on the final code: 0 differing files. Kept the module's `ITest` suffix over
  `IgTest` for consistency with its 38 neighbours.
- 2026-10-19 — Change 2 committed (`58dea9b841`). The new suite renamed to
  `DeclarationOrderIgTest`: the testing guideline takes precedence over the legacy
  `ITest` names around it, which are to be reviewed separately (see Follow-ups).
