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

import com.google.protobuf.gradle.ProtobufPlugin
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.spine.format.Format
import io.spine.tools.compiler.gradle.api.compilerWorkingDir
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.gradle.lib.spineExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin.CLEAN_TASK_NAME
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.spine.tools.compiler.gradle.plugin.Plugin as CompilerGradlePlugin

/**
 * Verifies the configuration performed by [ValidationGradlePlugin].
 *
 * When the plugin is applied to a project, it registers the `validation` extension,
 * applies the Protobuf and Spine Compiler Gradle plugins on the user's behalf, and
 * adds the validation runtime as an `implementation` dependency.
 *
 * When the project is evaluated with validation enabled, the plugin registers
 * the [WriteValidationSettings] task and makes the [LaunchSpineCompiler] tasks
 * depend on it. The tests evaluate the project explicitly because [ProjectBuilder]
 * does not run the `afterEvaluate` actions on its own. The tests do not run
 * the Spine Compiler.
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

    @Nested inner class
    `when the project is evaluated` {

        @Test
        fun `register the task writing the settings`() {
            evaluate()

            project.tasks.findByName(WriteValidationSettings.NAME)
                .shouldNotBeNull()
        }

        @Test
        fun `make every 'LaunchSpineCompiler' task depend on writing the settings`() {
            evaluate()

            val launchTasks = project.tasks.withType<LaunchSpineCompiler>()
            launchTasks.shouldNotBeEmpty()
            launchTasks.forEach { task ->
                task.dependencyNames() shouldContain WriteValidationSettings.NAME
            }
        }

        @Test
        fun `pass the DSL value to the task as input`() {
            evaluate()

            writeSettingsTask().suppressUnsignedFieldsWarnings.get() shouldBe false
            project.spineExtension<ValidationExtension>()
                .java.suppressWarnings.unsignedFields.set(true)
            writeSettingsTask().suppressUnsignedFieldsWarnings.get() shouldBe true
        }

        @Test
        fun `declare the DSL value as input and the settings file as output`() {
            evaluate()

            val task = writeSettingsTask()
            val inputs = task.inputs.properties.filterKeys {
                it.startsWith("suppressUnsignedFieldsWarnings")
            }
            inputs.values.shouldContainExactly(false)
            task.inputs.files.files.shouldBeEmpty()
            task.outputs.files.singleFile shouldBe task.settingsFile.get().asFile
        }

        @Test
        fun `point the settings file at the path read by the Spine Compiler`() {
            evaluate()
            val settingsDirectory =
                WorkingDirectory(project.compilerWorkingDir.asFile.toPath()).settingsDirectory
            settingsDirectory.write(RENDERER_CONSUMER_ID, Format.ProtoJson, "{}")

            writeSettingsTask().settingsFile.get().asFile.exists() shouldBe true
        }

        @Test
        fun `run after 'clean'`() {
            evaluate()

            val task = writeSettingsTask()
            val clean = project.tasks.getByName(CLEAN_TASK_NAME)
            task.mustRunAfter.getDependencies(task) shouldContain clean
        }

        @Test
        fun `not write the settings when validation is disabled`() {
            project.spineExtension<ValidationExtension>().enabled.set(false)
            evaluate()

            project.tasks.findByName(WriteValidationSettings.NAME).shouldBeNull()
            val launchTasks = project.tasks.withType<LaunchSpineCompiler>()
            launchTasks.shouldNotBeEmpty()
            launchTasks.forEach { task ->
                task.dependencyNames() shouldNotContain WriteValidationSettings.NAME
            }
        }

        private fun evaluate() {
            // The `DescriptorSetFilePlugin` applied by the Spine Compiler plugin
            // after evaluation requires a non-empty `group`.
            project.group = "io.spine.validation.test"
            (project as ProjectInternal).evaluate()
        }

        private fun writeSettingsTask(): WriteValidationSettings =
            project.tasks.getByName<WriteValidationSettings>(WriteValidationSettings.NAME)

        /**
         * Obtains the names of the tasks that this task depends on
         * via [TaskProvider]s.
         *
         * Inspects [Task.dependsOn] rather than resolving the task dependencies,
         * which would resolve the configurations of the Spine Compiler.
         */
        private fun Task.dependencyNames(): List<String> =
            dependsOn.filterIsInstance<TaskProvider<*>>().map { it.name }
    }

    private companion object {

        /**
         * The ID of the consumer of the settings written by [WriteValidationSettings].
         *
         * Repeats the canonical name of `JavaValidationRenderer`, which loads
         * the settings, to lock the name of the settings file.
         */
        const val RENDERER_CONSUMER_ID = "io.spine.tools.validation.java.JavaValidationRenderer"
    }
}
