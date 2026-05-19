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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * Registers a `patchGeneratedTemplateString` task in the project, which replaces
 * references to the legacy `io.spine.validation.TemplateString` class (and its
 * proto FQN `.spine.validation.TemplateString`) with `io.spine.string.TemplateString`
 * in the `generated/` sources.
 *
 * The task is wired to run after [upstreamTask] (the task that produces the sources
 * to be patched) and before `compileJava` and `kspKotlin`.
 *
 * @param upstreamTask the name of the task whose output should be patched
 *   (e.g., `generateProto` or `launchSpineCompiler`).
 */
fun Project.patchGeneratedTemplateString(upstreamTask: String): TaskProvider<Task> {
    val patchTask = tasks.register("patchGeneratedTemplateString") {
        dependsOn(upstreamTask)

        val generatedDir = layout.projectDirectory.dir("generated")
        inputs.dir(generatedDir).withPropertyName("generatedSources")
        outputs.dir(generatedDir).withPropertyName("patchedSources")

        doLast {
            val oldClassRef = "io.spine.validation.TemplateString"
            val newClassRef = "io.spine.string.TemplateString"
            val oldProtoRef = ".spine.validation.TemplateString"
            val newProtoRef = ".spine.string.TemplateString"
            generatedDir.asFile.walkTopDown()
                .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
                .forEach { file ->
                    val original = file.readText()
                    if (original.contains(oldClassRef) || original.contains(oldProtoRef)) {
                        val patched = original
                            .replace(oldClassRef, newClassRef)
                            .replace(oldProtoRef, newProtoRef)
                        file.writeText(patched)
                    }
                }
        }
    }

    tasks.named("compileJava") {
        dependsOn(patchTask)
    }

    afterEvaluate {
        tasks.named("kspKotlin") {
            dependsOn(patchTask)
        }
    }

    return patchTask
}
