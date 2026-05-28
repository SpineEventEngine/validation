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

package io.spine.tools.validation

import com.google.protobuf.Descriptors.Descriptor
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.spine.format.Format
import io.spine.logging.testing.ConsoleTap
import io.spine.logging.testing.tapConsole
import io.spine.testing.compiler.PipelineSetup
import io.spine.testing.compiler.acceptingOnly
import io.spine.testing.compiler.pipelineParams
import io.spine.testing.compiler.withRequestFile
import io.spine.testing.compiler.withSettingsDir
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.compiler.settings.SettingsDirectory
import io.spine.tools.validation.given.UnsignedWithRange
import io.spine.tools.validation.java.JavaValidationPlugin
import io.spine.tools.validation.settings.JavaValidationRendererSettings
import io.spine.tools.validation.settings.SuppressWarnings
import io.spine.type.toJson
import java.nio.file.Path
import kotlin.io.path.createDirectories
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Verifies that the "unsigned integer types are not supported in Java"
 * warning, emitted by `JavaValidationRenderer` -> `BoundedFieldGenerator`
 * during code generation, is correctly toggled by the
 * `JavaValidationRendererSettings.suppress_warnings.unsigned_fields` setting
 * written by the Validation Gradle plugin from the
 * `validation.java.suppressWarnings.unsignedFields` DSL property.
 *
 * The test drives the real Spine Compiler pipeline (via [PipelineSetup])
 * against a proto fixture with a `uint32` field constrained by `(range)`
 * — the shape that triggers the warning — and captures everything written
 * to stdout while the pipeline runs.
 */
@DisplayName("Unsigned-integer warning emission should")
internal class UnsignedFieldWarningSpec {

    @TempDir
    lateinit var workingDir: Path

    @Test
    fun `emit the warning when no settings are written`() {
        val output = compile(UnsignedWithRange.getDescriptor()) { /* no settings */ }

        output shouldContain WARNING_TEXT
    }

    @Test
    fun `emit the warning when 'unsignedFields' suppression is 'false'`() {
        val output = compile(UnsignedWithRange.getDescriptor()) {
            writeSettings(suppressUnsignedFields = false)
        }

        output shouldContain WARNING_TEXT
    }

    @Test
    fun `not emit the warning when 'unsignedFields' suppression is 'true'`() {
        val output = compile(UnsignedWithRange.getDescriptor()) {
            writeSettings(suppressUnsignedFields = true)
        }

        output shouldNotContain WARNING_TEXT
    }

    /**
     * Runs the Spine Compiler pipeline with the Java validation plugin
     * against the given [descriptor], invoking [writeSettings] to populate
     * the settings directory before the pipeline starts. Returns
     * everything the pipeline (including the renderer) prints to stdout.
     */
    private fun compile(
        descriptor: Descriptor,
        writeSettings: SettingsDirectory.() -> Unit
    ): String {
        val wd = WorkingDirectory(workingDir)
        val outputDir = workingDir.resolve("output")
        outputDir.createDirectories()
        val params = pipelineParams {
            withRequestFile(wd.requestDirectory.file(SourceSetName("testFixtures")))
            withSettingsDir(wd.settingsDirectory.path)
        }
        val setup = PipelineSetup.byResources(
            params,
            plugins = listOf(JavaValidationPlugin()),
            outputRoot = outputDir,
            descriptorFilter = acceptingOnly(descriptor),
            writeSettings = writeSettings
        )
        val pipeline = setup.createPipeline()
        return tapConsole {
            pipeline()
        }
    }

    /**
     * Writes a [JavaValidationRendererSettings] settings file for the
     * `JavaValidationRenderer` consumer, mirroring exactly what the
     * Validation Gradle plugin does when the user configures
     * `validation.java.suppressWarnings.unsignedFields`.
     */
    private fun SettingsDirectory.writeSettings(suppressUnsignedFields: Boolean) {
        val message = JavaValidationRendererSettings.newBuilder()
            .setSuppressWarnings(
                SuppressWarnings.newBuilder()
                    .setUnsignedFields(suppressUnsignedFields)
                    .build()
            )
            .build()
        write(
            JAVA_VALIDATION_RENDERER_CONSUMER_ID,
            Format.ProtoJson,
            message.toJson()
        )
    }

    companion object {

        /**
         * Canonical class name of the renderer that reads
         * [JavaValidationRendererSettings] — the Spine Compiler
         * `LoadsSettings` consumer ID.
         */
        private const val JAVA_VALIDATION_RENDERER_CONSUMER_ID: String =
            "io.spine.tools.validation.java.JavaValidationRenderer"

        /**
         * The first sentence of the unsigned-integer warning emitted by
         * `BoundedFieldGenerator`. Matching only the first sentence keeps
         * the assertion stable across JDK feature versions, which are
         * baked into the warning's link to the Javadoc.
         */
        private const val WARNING_TEXT: String =
            "Unsigned integer types are not supported in Java."

        @JvmStatic
        @BeforeAll
        fun installConsoleTap() {
            ConsoleTap.install()
        }
    }
}
