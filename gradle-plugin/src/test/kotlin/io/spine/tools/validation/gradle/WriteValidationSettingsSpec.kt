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

import io.kotest.matchers.shouldBe
import io.spine.tools.validation.settings.javaValidationRendererSettings
import io.spine.tools.validation.settings.suppressWarnings
import io.spine.type.toJson
import java.io.File
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Verifies the action of [WriteValidationSettings].
 *
 * The registration of the task by [ValidationGradlePlugin] is verified by
 * [ValidationGradlePluginSpec].
 */
@DisplayName("`WriteValidationSettings` should")
internal class WriteValidationSettingsSpec {

    private lateinit var project: Project
    private lateinit var outputFile: File

    @BeforeEach
    fun setUp(@TempDir projectDir: File) {
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()
        outputFile = projectDir.resolve("settings").resolve(WriteValidationSettings.SETTINGS_FILE)
    }

    @Test
    fun `write the suppressed warnings`() {
        write(suppressUnsignedFieldsWarnings = true)

        outputFile.readText() shouldBe expectedJson(unsignedFields = true)
    }

    @Test
    fun `write the warnings that are not suppressed`() {
        write(suppressUnsignedFieldsWarnings = false)

        outputFile.readText() shouldBe expectedJson(unsignedFields = false)
    }

    @Test
    fun `overwrite the file written before`() {
        write(suppressUnsignedFieldsWarnings = true)
        write(suppressUnsignedFieldsWarnings = false)

        outputFile.readText() shouldBe expectedJson(unsignedFields = false)
    }

    /**
     * Runs the action of a newly registered task with the given input.
     *
     * Gradle creates the parent directories of a task's `@OutputFile` before
     * running the task. This function does the same because it calls the task
     * action directly.
     */
    private fun write(suppressUnsignedFieldsWarnings: Boolean) {
        val name = "write${project.tasks.size}"
        val task = project.tasks.register<WriteValidationSettings>(name) {
            this.suppressUnsignedFieldsWarnings.set(suppressUnsignedFieldsWarnings)
            settingsDirectory.set(outputFile.parentFile)
        }.get()
        outputFile.parentFile.mkdirs()
        task.write()
    }

    private fun expectedJson(unsignedFields: Boolean): String =
        javaValidationRendererSettings {
            suppressWarnings = suppressWarnings {
                this.unsignedFields = unsignedFields
            }
        }.toJson()
}
