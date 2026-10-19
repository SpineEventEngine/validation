/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
