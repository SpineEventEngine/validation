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

package io.spine.tools.validation.gradle

import com.google.protobuf.gradle.ProtobufPlugin
import io.spine.tools.compiler.gradle.api.addUserClasspathDependency
import io.spine.tools.compiler.gradle.api.compilerSettings
import io.spine.tools.compiler.gradle.plugin.Extension
import io.spine.tools.compiler.gradle.plugin.Plugin as CompilerGradlePlugin
import io.spine.tools.gradle.DslSpec
import io.spine.tools.gradle.lib.LibraryPlugin
import io.spine.tools.gradle.lib.spineExtension
import io.spine.tools.meta.MavenArtifact
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.apply

/**
 * Gradle plugin that configures the Spine Compiler to run the Validation Compiler.
 */
public class ValidationGradlePlugin : LibraryPlugin<ValidationExtension>(
    DslSpec(ValidationExtension.NAME, ValidationExtension::class)
) {
    override fun apply(project: Project) {
        super.apply(project)
        // Add the Protobuf Gradle Plugin so that the user doesn't need to add it manually.
        // The version of the plugin is defined by our dependency but can be forced
        // to a newer one in the user's project.
        project.apply<ProtobufPlugin>()

        // Apply the Compiler Gradle Plugin so that we can manipulate the compiler settings.
        // We do not want the user to add it manually either.
        project.apply<CompilerGradlePlugin>()
        val javaBundle = ValidationSdk.javaCodegenBundle
        project.run {
            addUserClasspathDependency(javaBundle)
            afterEvaluate {
                // Add the Validation Java Compiler only if `spine/validation/enabled` is true.
                if (validationExtension.enabled.get()) {
                    (compilerSettings as Extension).run {
                        // Put the Validation Java Compiler first in the list of the plugins.
                        // Other plugins may rely on the validation code.
                        val ordered = listOf(ValidationSdk.javaCompilerPlugin) + plugins.get()
                        plugins.set(ordered)
                    }
                }
            }
            // We add the dependency on runtime anyway for the following reasons:
            //  1. We do not want users to change their Gradle build files when they turn on or off
            //     code generation for the validation code.
            //
            //  2. We have run-time validation rules that are going to be used in parallel with
            //     the generated code. This includes current and new implementation for validation
            //     rules for the already existing generated Protobuf code.
            //
            addDependency("implementation", ValidationSdk.jvmRuntime)
        }
    }
}

/**
 * The extension added by [ValidationGradlePlugin].
 */
private val Project.validationExtension: ValidationExtension
    get() = spineExtension<ValidationExtension>()

private fun Project.addDependency(configuration: String, artifact: MavenArtifact) {
    val dependency = findDependency(artifact) ?: artifact.coordinates
    dependencies.add(configuration, dependency)
}

private fun Project.findDependency(artifact: MavenArtifact): Dependency? {
    val dependencies = configurations.flatMap { c -> c.dependencies }
    val found = dependencies.firstOrNull { d ->
        artifact.group == d.group // `d.group` could be `null`.
                && artifact.name == d.name
    }
    return found
}
