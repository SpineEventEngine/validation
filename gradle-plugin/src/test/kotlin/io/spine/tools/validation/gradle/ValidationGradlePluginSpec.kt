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
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.spine.tools.gradle.lib.spineExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.spine.tools.compiler.gradle.plugin.Plugin as CompilerGradlePlugin

/**
 * Verifies the eager configuration performed by [ValidationGradlePlugin] when
 * it is applied to a project: registering the `validation` extension, applying
 * the Protobuf and Spine Compiler Gradle plugins on the user's behalf, and
 * adding the validation runtime as an `implementation` dependency.
 *
 * The plugin's `afterEvaluate` and per-task `doFirst` actions run only during a
 * real build and are covered by the integration projects under `:tests`.
 */
@DisplayName("`ValidationGradlePlugin` should")
internal class ValidationGradlePluginSpec {

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        // A consumer project always applies a JVM plugin; the Spine Compiler
        // plugin (applied transitively below) requires the `JavaPluginExtension`.
        project.pluginManager.apply("java")
        project.pluginManager.apply(ValidationGradlePlugin::class.java)
    }

    @Test
    fun `register the 'validation' extension`() {
        project.spineExtension<ValidationExtension>().shouldNotBeNull()
    }

    @Test
    fun `apply the Protobuf Gradle plugin on the user's behalf`() {
        project.plugins.hasPlugin(ProtobufPlugin::class.java) shouldBe true
    }

    @Test
    fun `apply the Spine Compiler Gradle plugin on the user's behalf`() {
        project.plugins.hasPlugin(CompilerGradlePlugin::class.java) shouldBe true
    }

    @Test
    fun `add the validation runtime as an 'implementation' dependency`() {
        val implementation = project.configurations.getByName("implementation")
        val hasRuntime = implementation.dependencies.any {
            it.name == "spine-validation-jvm-runtime"
        }
        hasRuntime shouldBe true
    }
}
