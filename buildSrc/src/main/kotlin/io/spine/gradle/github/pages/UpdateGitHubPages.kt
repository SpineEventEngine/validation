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

package io.spine.gradle.github.pages

import dokkaHtmlTask
import io.spine.gradle.fs.LazyTempPath
import io.spine.gradle.github.pages.TaskName.copyDokka
import io.spine.gradle.github.pages.TaskName.copyJavadoc
import io.spine.gradle.github.pages.TaskName.updateGitHubPages
import io.spine.gradle.isSnapshot
import io.spine.gradle.javadoc.ExcludeInternalDoclet
import io.spine.gradle.javadoc.javadocTask
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

/**
 * Registers the `updateGitHubPages` task which performs the update of the GitHub
 * Pages with the documentation generated by Javadoc and Dokka for a particular
 * Gradle project. The generated documentation is appended to the `spine.io` site
 * via GitHub pages by pushing commits to the `gh-pages` branch.
 *
 * Please note that the update is only performed for the projects which are
 * NOT snapshots.
 *
 * Users may supply [allowInternalJavadoc][UpdateGitHubPagesExtension.allowInternalJavadoc]
 * to configure documentation generated by Javadoc. The documentation for the code
 * marked `@Internal` is included when the option is set to `true`. By default, this
 * option is `false`.
 *
 * Usage:
 * ```
 *      updateGitHubPages {
 *
 *          // Include `@Internal`-annotated code.
 *          allowInternalJavadoc.set(true)
 *
 *          // Propagate the full path to the local folder of the repository root.
 *          rootFolder.set(rootDir.absolutePath)
 *      }
 * ```
 *
 * In order to work, the script needs a `deploy_key_rsa` private RSA key file in the
 * repository root. It is recommended to encrypt it in the repository and then decrypt
 * it on CI upon publication. Also, the script uses the `FORMAL_GIT_HUB_PAGES_AUTHOR`
 * environment variable to set the author email for the commits. The `gh-pages`
 * branch itself should exist before the plugin is run.
 *
 * NOTE: when changing the value of "FORMAL_GIT_HUB_PAGES_AUTHOR", one also must change
 * the SSH private (encrypted `deploy_key_rsa`) and the public
 * ("GitHub Pages publisher" on GitHub) keys.
 *
 * Another requirement is an environment variable `REPO_SLUG`, which is set by the CI
 * environment, such as `Publish` GitHub Actions workflow. It points to the repository
 * for which the update is executed. E.g.:
 *
 * ```
 *      REPO_SLUG: SpineEventEngine/base
 * ```
 *
 * @see UpdateGitHubPagesExtension for the extension which is used to configure
 *      this plugin
 */
class UpdateGitHubPages : Plugin<Project> {

    /**
     * Root folder of the repository, to which this `Project` belongs.
     */
    internal lateinit var rootFolder: File

    /**
     * The external inputs to include into the publishing.
     *
     * The inputs are evaluated according to [Copy.from] specification.
     */
    private lateinit var includedInputs: Set<Any>

    /**
     * Path to the temp folder used to gather the Javadoc output before submitting it
     * to the GitHub Pages update.
     */
    internal val javadocOutputFolder = LazyTempPath("javadoc")

    /**
     * Path to the temp folder used to gather the documentation generated by Dokka
     * before submitting it to the GitHub Pages update.
     */
    internal val dokkaOutputFolder = LazyTempPath("dokka")

    /**
     * Applies the plugin to the specified [project].
     *
     * If the project version says it is a snapshot, the plugin registers a no-op task.
     *
     * Even in such a case, the extension object is still created in the given project
     * to allow customization of the parameters in its build script for later usage
     * when the project version changes to non-snapshot.
     */
    override fun apply(project: Project) {
        val extension = UpdateGitHubPagesExtension.createIn(project)
        project.afterEvaluate {
            val projectVersion = project.version.toString()
            if (projectVersion.isSnapshot()) {
                registerNoOpTask()
            } else {
                registerTasks(extension)
            }
        }
    }

    /**
     * Registers `updateGitHubPages` task which performs no actual update, but prints
     * the message telling the update is skipped, since the project is in
     * its `SNAPSHOT` version.
     */
    private fun Project.registerNoOpTask() {
        tasks.register(updateGitHubPages) {
            doLast {
                val project = this@registerNoOpTask
                println(
                    "GitHub Pages update will be skipped since this project is a snapshot: " +
                            "`${project.name}-${project.version}`."
                )
            }
        }
    }

    private fun Project.registerTasks(extension: UpdateGitHubPagesExtension) {
        val allowInternalJavadoc = extension.allowInternalJavadoc()
        rootFolder = extension.rootFolder()
        includedInputs = extension.includedInputs()

        if (!allowInternalJavadoc) {
            val doclet = ExcludeInternalDoclet(extension.excludeInternalDocletVersion)
            doclet.registerTaskIn(this)
        }

        tasks.registerCopyJavadoc(allowInternalJavadoc)
        tasks.registerCopyDokka()

        val updatePagesTask = tasks.registerUpdateTask()
        updatePagesTask.configure {
            dependsOn(copyJavadoc)
            dependsOn(copyDokka)
        }
    }

    private fun TaskContainer.registerCopyJavadoc(allowInternalJavadoc: Boolean) {
        val inputs = composeJavadocInputs(allowInternalJavadoc)

        register(copyJavadoc, Copy::class.java) {
            inputs.forEach { from(it) }
            into(javadocOutputFolder)
        }
    }

    private fun TaskContainer.composeJavadocInputs(allowInternalJavadoc: Boolean): List<Any> {
        val inputs = mutableListOf<Any>()
        if (allowInternalJavadoc) {
            inputs.add(javadocTask())
        } else {
            inputs.add(javadocTask(ExcludeInternalDoclet.taskName))
        }
        inputs.addAll(includedInputs)
        return inputs
    }

    private fun TaskContainer.registerCopyDokka() {
        val inputs = composeDokkaInputs()

        register(copyDokka, Copy::class.java) {
            inputs.forEach { from(it) }
            into(dokkaOutputFolder)
        }
    }

    private fun TaskContainer.composeDokkaInputs(): List<Any> {
        val inputs = mutableListOf<Any>()

        dokkaHtmlTask()?.let {
            inputs.add(it)
        }
        inputs.addAll(includedInputs)

        return inputs
    }

    private fun TaskContainer.registerUpdateTask(): TaskProvider<Task> {
        return register(updateGitHubPages) {
            doLast {
                try {
                    updateGhPages(project)
                } finally {
                    cleanup()
                }
            }
        }
    }

    private fun cleanup() {
        val folders = listOf(dokkaOutputFolder, javadocOutputFolder)
        folders.forEach {
            it.toFile().deleteRecursively()
        }
    }
}
