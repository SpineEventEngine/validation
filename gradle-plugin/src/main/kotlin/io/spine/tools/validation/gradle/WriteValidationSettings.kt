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

import io.spine.format.Format
import io.spine.tools.validation.settings.JavaValidationRendererSettings
import io.spine.tools.validation.settings.javaValidationRendererSettings
import io.spine.tools.validation.settings.suppressWarnings
import io.spine.type.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes the [JavaValidationRendererSettings] built from the
 * `validation { java { ... } }` DSL block as a settings file for the
 * `io.spine.tools.validation.java.JavaValidationRenderer` consumer.
 *
 * The Validation Gradle plugin registers this task when validation is enabled
 * in the project, and makes every `LaunchSpineCompiler` task depend on it.
 * As a result, the settings file is in place when Gradle fingerprints
 * the settings directory, which is an input of `LaunchSpineCompiler`.
 * This keeps `LaunchSpineCompiler` up-to-date until the settings or its other
 * inputs change, and makes the settings a part of its build cache key.
 *
 * The DSL values are the inputs of this task. Changing a value makes the task
 * rewrite the file, which, in turn, makes `LaunchSpineCompiler` out of date.
 * When the file is deleted (for example, by `clean`), the task writes it again.
 *
 * The task declares only the file it writes as its output, not the whole settings
 * directory, because other plugins write their settings files into the same directory.
 */
@DisableCachingByDefault(because = "Writing a single small file is as fast as loading it.")
public abstract class WriteValidationSettings : DefaultTask() {

    /**
     * Tells if the "unsigned integer types are not supported in Java" warning
     * is suppressed.
     *
     * @see ValidationExtension.Java.SuppressWarnings.unsignedFields
     */
    @get:Input
    internal abstract val suppressUnsignedFieldsWarnings: Property<Boolean>

    /**
     * The settings directory of the Spine Compiler.
     *
     * The property is [Internal] because the task does not read the directory.
     * The task output is the [settingsFile] in this directory.
     */
    @get:Internal
    internal abstract val settingsDirectory: DirectoryProperty

    /**
     * The settings file to write.
     */
    @get:OutputFile
    internal val settingsFile: Provider<RegularFile> = settingsDirectory.file(SETTINGS_FILE)

    /**
     * Writes the settings file in the [ProtoJson][Format.ProtoJson] format.
     */
    @TaskAction
    public fun write() {
        val settings = javaValidationRendererSettings {
            suppressWarnings = suppressWarnings {
                unsignedFields = suppressUnsignedFieldsWarnings.get()
            }
        }
        settingsFile.get().asFile.writeText(settings.toJson())
    }

    public companion object {

        /**
         * The name of the task.
         */
        public const val NAME: String = "writeValidationSettings"

        /**
         * Canonical class name of the Spine Compiler renderer that reads
         * [JavaValidationRendererSettings] via `LoadsSettings`.
         *
         * Kept as a string literal because the Validation Gradle plugin does not
         * depend on the `:java` module that hosts the renderer class — only on the
         * `:java-settings` module that hosts the proto type.
         */
        internal const val CONSUMER_ID: String =
            "io.spine.tools.validation.java.JavaValidationRenderer"

        /**
         * The name of the settings file.
         *
         * Follows the naming of `SettingsDirectory.write()`: the [ID of the consumer][CONSUMER_ID],
         * a dot, and the primary extension of the [ProtoJson][Format.ProtoJson] format.
         */
        internal val SETTINGS_FILE: String = "$CONSUMER_ID.${Format.ProtoJson.extension}"
    }
}
