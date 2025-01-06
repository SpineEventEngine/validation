/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.validation

import com.google.protobuf.Descriptors.GenericDescriptor
import io.spine.option.OptionsProto
import io.spine.protodata.ast.filePattern
import io.spine.protodata.backend.DescriptorFilter
import io.spine.protodata.params.WorkingDirectory
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.util.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.settings.defaultConsumerId
import io.spine.protodata.testing.PipelineSetup
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.protodata.testing.pipelineParams
import io.spine.protodata.testing.withRequestFile
import io.spine.protodata.testing.withSettingsDir
import io.spine.tools.code.SourceSetName
import java.nio.file.Path

/**
 * A test fixture for running a validation test for the Protobuf declaration specified
 * by the given descriptor.
 */
class ValidationTestFixture(
    descriptor: GenericDescriptor,
    workingDir: Path,
    plugin: Plugin = ValidationPlugin()
) {
    val setup: PipelineSetup

    init {
        val wd = WorkingDirectory(workingDir)
        val outputDir = workingDir.resolve("output")
        outputDir.toFile().mkdirs()
        val params = pipelineParams {
            withRequestFile(wd.requestDirectory.file(SourceSetName("testFixtures")))
            withSettingsDir(wd.settingsDirectory.path)
        }
        setup = byResources(
            params,
            listOf(plugin),
            outputDir,
            acceptingOnly(descriptor)) {
            writeSettings(it)
        }
    }


    private fun acceptingOnly(descriptor: GenericDescriptor): DescriptorFilter =
        { it.fullName == descriptor.fullName }

    /**
     * Writes default settings as created by McJava.
     */
    private fun writeSettings(settings: SettingsDirectory) {
        val config = validationConfig {
            messageMarkers = messageMarkers {
                commandPattern.add(filePattern { suffix = "commands.proto" })
                eventPattern.add(filePattern { suffix = "events.proto" })
                rejectionPattern.add(filePattern { suffix = "rejections.proto" })
                entityOptionName.add(OptionsProto.entity.descriptor.name)
            }
        }
        settings.write(
            ValidationPlugin::class.java.defaultConsumerId,
            Format.PROTO_JSON,
            config.toByteArray()
        )
    }
}
