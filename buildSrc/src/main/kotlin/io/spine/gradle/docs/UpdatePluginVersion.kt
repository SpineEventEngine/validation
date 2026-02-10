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

package io.spine.gradle.docs

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 * Updates the version of a Gradle plugin in `build.gradle.kts` files.
 *
 * The task searches for plugin declarations in the format
 * `id("plugin-id") version "version-number"` and replaces
 * the version number with the one found in the version script file.
 *
 * @property directory
 *         The directory to scan recursively for `build.gradle.kts` files.
 * @property versionScriptFile
 *         The path to the `version.gradle.kts` file containing the plugin version.
 * @property pluginId
 *         The ID of the plugin whose version should be updated.
 */
abstract class UpdatePluginVersion : DefaultTask() {

    @get:InputDirectory
    abstract val directory: DirectoryProperty

    @get:InputFile
    abstract val versionScriptFile: RegularFileProperty

    @get:Input
    abstract val pluginId: Property<String>

    /**
     * Updates plugin versions in build files within the path in the [directory].
     */
    @TaskAction
    fun update() {
        val version = extractVersion()
        val id = pluginId.get()
        val rootDir = directory.get().asFile

        rootDir.walkTopDown()
            .filter { it.name == "build.gradle.kts" }
            .forEach { file ->
                updateFile(file, id, version)
            }
    }

    private fun extractVersion(): String {
        val scriptFile = versionScriptFile.get().asFile
        val content = scriptFile.readText()
        // Regex to match: val ... by extra("version")
        val regex = """extra\("([^"]+)"\)""".toRegex()
        val matchResult = regex.find(content)
        return matchResult?.groupValues?.get(1)
            ?: error("Could not find version in ${scriptFile.absolutePath}")
    }

    private fun updateFile(file: File, id: String, version: String) {
        val content = file.readText()
        // Regex to match: id("plugin-id") version "version-number"
        val regex = """id\("$id"\)\s+version\s+"([^"]+)"""".toRegex()
        
        if (regex.containsMatchIn(content)) {
            val updatedContent = regex.replace(content) { match ->
                "id(\"$id\") version \"$version\""
            }
            if (content != updatedContent) {
                file.writeText(updatedContent)
                logger.info("Updated version in `${file.absolutePath}`.")
            }
        }
    }
}
