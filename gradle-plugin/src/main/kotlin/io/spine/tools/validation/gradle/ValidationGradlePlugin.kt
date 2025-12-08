/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import io.spine.tools.compiler.gradle.api.compilerSettings
import io.spine.tools.compiler.gradle.api.addUserClasspathDependency
import io.spine.tools.gradle.DslSpec
import io.spine.tools.gradle.lib.LibraryPlugin
import io.spine.tools.gradle.lib.spineExtension
import org.gradle.api.Project

/**
 * Gradle plugin that configures the Spine Compiler to run the Validation Compiler in solo mode.
 *
 * The plugin applies the Spine Compiler Gradle plugin (id = `io.spine.compiler`) and then
 * registers the Validation compiler implementation using the Compiler's Gradle extension.
 *
 * We avoid hard dependencies on the Compiler Gradle API types and configure the extension
 * reflectively to stay compatible across minor API changes.
 */
public class ValidationGradlePlugin : LibraryPlugin<ValidationExtension>(
    DslSpec(ValidationExtension.NAME, ValidationExtension::class)
) {
    override fun apply(project: Project) {
        super.apply(project)
        val version = Meta.version
        val javaBundle = ValidationSdk.javaCodegenBundle(version)
        project.run {
            addUserClasspathDependency(javaBundle)
            afterEvaluate {
                // Add the Validation Java Compiler only if `spine/validation/enabled` is true.
                if (validationExtension.enabled.get()) {
                    compilerSettings.plugins(ValidationSdk.javaCompilerPlugin)
                }
            }
        }
    }
}

/**
 * The extension added by [ValidationGradlePlugin].
 */
private val Project.validationExtension: ValidationExtension
    get() = spineExtension<ValidationExtension>()
