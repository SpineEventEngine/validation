/*
 * Copyright 2026, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.gradle.testing

import io.spine.dependency.test.Jacoco
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.withType

/**
 * Configures the `launch*SpineCompiler` tasks of this project so that the JaCoCo
 * agent is attached to the forked JVM that runs the Spine Compiler.
 *
 * The Spine Compiler executes plugin code — renderers, option generators, and
 * ProtoData plugins such as `JavaValidationPlugin` — in a **separate JVM** spawned
 * by the `launch[<SourceSet>]SpineCompiler`
 * [JavaExec][org.gradle.api.tasks.JavaExec] tasks. Kover (and JaCoCo) instrument
 * only the `test` JVM, so this out-of-process execution is otherwise not credited
 * to coverage, even though the module's `.proto` fixtures exercise it on every build.
 *
 * For every such launch task, this method:
 *
 *  1. Resolves the standalone JaCoCo agent JAR pinned to [Jacoco.version] through a
 *     dedicated [AGENT_CONFIGURATION] configuration.
 *  2. Attaches
 *     `-javaagent:<agent>=destfile=build/`[COMPILER_COVERAGE_DIR]`/<task>.exec,append=true`
 *     to the forked JVM. A per-task destination file keeps the launch variants
 *     (main, test, test-fixtures) from clobbering one another.
 *  3. Wipes the exec directory at most once per build invocation, from the `doFirst`
 *     of the first launch task that actually executes, so stale coverage from a
 *     previous run does not accumulate. A `doFirst` action (rather than a `dependsOn`
 *     clean task) runs only when the task truly executes: an `UP-TO-DATE` launch keeps
 *     the previous `.exec`, which the report then still credits.
 *  4. Marks the launch tasks non-cacheable. The agent's `.exec` is flushed
 *     out-of-process on JVM shutdown, *after* the task action completes, so it cannot
 *     be a declared task output. Were the task left cacheable, a cache hit would
 *     restore the generated sources but no `.exec`, dropping the coverage.
 *
 * The agent is attached and the destination file computed from a `doFirst` action —
 * the same approach [enableTestKitCoverage] uses for TestKit workers — rather than a
 * `jvmArgumentProviders` entry, so the two coverage helpers stay structurally
 * identical and neither contributes untracked task inputs.
 *
 * The produced `.exec` files are merged into the **root** Kover report by
 * [io.spine.gradle.report.coverage.KoverConfig]. Only classes the root aggregation
 * owns (the `java` and `context` modules' renderers/generators) are credited from
 * them; a Kover report is scoped to its own classes, and the compiler loads those
 * classes as their original, un-relocated artifacts, so JaCoCo's class IDs match.
 * The agent emits binary execution data because Kover merges binary data at the
 * probe level — see `KoverConfig` for why binary, not XML.
 *
 * The method is idempotent and may be called on every subproject; it is a no-op for
 * modules that declare no `launch*SpineCompiler` task.
 */
fun Project.enableSpineCompilerCoverage() {
    val agent = configurations.maybeCreate(AGENT_CONFIGURATION).apply {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies.add(agent.name, "org.jacoco:org.jacoco.agent:${Jacoco.version}:runtime")

    val agentPath = agent.elements.map { it.single().asFile.absolutePath }
    val execDir = layout.buildDirectory.dir(COMPILER_COVERAGE_DIR)

    // Wiped at most once per build invocation, by the first launch task that actually
    // executes — see the KDoc above for why this is a guarded `doFirst` wipe rather
    // than a `dependsOn` clean task.
    val cleaned = AtomicBoolean(false)

    val launchTasks = tasks.withType<JavaExec>().matching { it.isSpineCompilerLaunchTask() }

    launchTasks.configureEach {
        inputs.files(agent).withPropertyName(AGENT_CONFIGURATION)
        outputs.cacheIf(
            "The Spine Compiler coverage exec is produced out-of-process and cannot " +
                    "be a declared task output; a cache hit would drop it."
        ) { false }
        doFirst {
            val dir = execDir.get().asFile
            if (cleaned.compareAndSet(false, true)) {
                dir.deleteRecursively()
            }
            dir.mkdirs()
            val execFile = dir.resolve("$name.exec")
            jvmArgs(
                "-javaagent:${agentPath.get()}=destfile=${execFile.absolutePath}," +
                        "append=true,output=file"
            )
        }
    }

    // The root Kover report/verification tasks read these exec files as
    // `additionalBinaryReports`, but do not otherwise depend on the (non-Kover)
    // test modules that produce them. Order them explicitly after the launch
    // tasks — otherwise the report may run before the exec is written.
    rootProject.tasks
        .matching { it.isCoverageReportTask() }
        .configureEach { dependsOn(launchTasks) }
}

/**
 * Tells whether this is one of the `launch[<SourceSet>]SpineCompiler` tasks that
 * fork the Spine Compiler — matched by name to avoid a compile-time dependency on
 * the compiler Gradle plugin's task types.
 */
private fun Task.isSpineCompilerLaunchTask(): Boolean =
    name.startsWith(LAUNCH_TASK_PREFIX) && name.endsWith(LAUNCH_TASK_SUFFIX)

/**
 * Tells whether this is a Kover report or verification task — one that reads the
 * binary exec files and must therefore run only after the launch tasks that write
 * them. Matches `koverXmlReport`, `koverHtmlReport`, `koverVerify`, and their
 * `Cached*` companions.
 */
private fun Task.isCoverageReportTask(): Boolean =
    name.startsWith(KOVER_TASK_PREFIX) &&
            (name.endsWith(REPORT_TASK_SUFFIX) || name.endsWith(VERIFY_TASK_SUFFIX))

/**
 * The name of the directory under a module's `build` directory where the coverage
 * of forked Spine Compiler JVMs is collected.
 *
 * The directory holds JaCoCo execution-data (`.exec`) files — one per launch task —
 * written by the JaCoCo agent attached to the compiler fork. `KoverConfig` picks
 * these files up and feeds them into the root Kover report as additional binary
 * reports.
 *
 * @see io.spine.gradle.report.coverage.KoverConfig
 */
internal const val COMPILER_COVERAGE_DIR: String = "jacoco-compiler"

/**
 * The name of the dedicated, resolvable configuration that holds the standalone
 * JaCoCo agent JAR (`org.jacoco:org.jacoco.agent:<version>:runtime`) attached to
 * the forked Spine Compiler JVMs.
 *
 * The configuration is hidden and non-consumable; it exists only to resolve the
 * agent JAR and to register it as an input of the launch tasks.
 */
private const val AGENT_CONFIGURATION: String = "spineCompilerJacocoAgent"

private const val LAUNCH_TASK_PREFIX: String = "launch"
private const val LAUNCH_TASK_SUFFIX: String = "SpineCompiler"

private const val KOVER_TASK_PREFIX: String = "kover"
private const val REPORT_TASK_SUFFIX: String = "Report"
private const val VERIFY_TASK_SUFFIX: String = "Verify"
