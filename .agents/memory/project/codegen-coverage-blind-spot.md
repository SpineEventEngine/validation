---
name: codegen-coverage-blind-spot
description: Why the `java`/`context` codegen modules report ~0% coverage — they run out-of-process in the forked Spine Compiler JVM, which JaCoCo/Kover do not instrument.
metadata:
  type: project
  since: 2026-07-06
---

The `java` and `context` modules are Spine Compiler plugins/renderers (`JavaValidationPlugin`,
the `*Generator` classes, the `expression/*` builders, the ProtoData plugin). They execute in the
**forked `launch*SpineCompiler` (`JavaExec`) process** during the build, not in the `test` JVM.
Kover/JaCoCo instrument only the test JVM, so this code reports **~0% line coverage** even though
the `tests/*` and `context-tests` `.proto` fixtures exercise it on every build. The lone generator
with real coverage — `UnsignedIntegerWarnings` (~82%) — has it because one spec instantiates it
**in-process**. This is the main reason the repo total sits around ~12%.

**Why:** So a future session seeing an alarmingly low total, or a codegen module at 0%, does not
misread it as neglected/untested code or a quality regression and waste time re-deriving the cause.
It is a measurement blind spot inherent to out-of-process code generation, not missing tests.

**How to apply:** When coverage looks structurally low here, do **not** reach for sibling
attribution (`SiblingCoverage.creditTestCoverageFrom`) or expect root Kover aggregation to fix it —
those re-attribute execution data that already exists in-process, and for compiler-executed code no
such data exists anywhere. Closing the gap requires **instrumenting the forked compiler JVM**:
attach the standalone JaCoCo agent to the `launch*SpineCompiler` tasks and merge the resulting
`.exec` via `KoverConfig`'s `additionalBinaryReports` — the same pattern
`io.spine.gradle.testing.enableTestKitCoverage()` already uses for Gradle TestKit workers. The
JVM-args/agent hook is best owned by the `SpineEventEngine/compiler` repo (the launch tasks are
`JavaExec`, so `jvmArgs` is the natural injection point; the `compiler-fat-cli` does **not**
relocate plugin classes, so JaCoCo class IDs match and `additionalBinaryReports` credits them
correctly). Complement with in-process unit tests for logic-heavy
generators, and report codegen vs. runtime under separate Codecov flags/components.
