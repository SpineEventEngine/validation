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
import io.kotest.matchers.string.shouldNotContain
import io.spine.logging.testing.ConsoleTap
import io.spine.logging.testing.tapConsole
import io.spine.testing.compiler.PipelineSetup
import io.spine.testing.compiler.acceptingOnly
import io.spine.testing.compiler.pipelineParams
import io.spine.testing.compiler.withRequestFile
import io.spine.testing.compiler.withSettingsDir
import io.spine.tools.code.SourceSetName
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.compiler.protobuf.field
import io.spine.tools.validation.given.EntityWithInt32Id
import java.nio.file.Path
import kotlin.io.path.createDirectories
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Assumed `(required)` entity IDs should")
internal class AssumedRequiredIdSpec {

    @TempDir
    lateinit var workingDir: Path

    @Test
    fun `not emit a warning for an int32 field`() {
        val descriptor = EntityWithInt32Id.getDescriptor()
        val output = compile(descriptor)
        val field = descriptor.field("id")

        output shouldNotContain field.fullName
        output shouldNotContain "should not be declared as `(required)`"
        output shouldNotContain "assumed to be required"
    }

    private fun compile(descriptor: Descriptor): String {
        val wd = WorkingDirectory(workingDir)
        val outputDir = workingDir.resolve("output")
        outputDir.createDirectories()
        val params = pipelineParams {
            withRequestFile(wd.requestDirectory.file(SourceSetName("testFixtures")))
            withSettingsDir(wd.settingsDirectory.path)
        }
        val setup = PipelineSetup.byResources(
            params,
            plugins = listOf(object : ValidationPlugin() {}),
            outputRoot = outputDir,
            descriptorFilter = acceptingOnly(descriptor)
        ) {}
        val pipeline = setup.createPipeline()
        return tapConsole {
            pipeline()
        }
    }

    companion object {

        @JvmStatic
        @BeforeAll
        fun installConsoleTap() {
            ConsoleTap.install()
        }
    }
}
