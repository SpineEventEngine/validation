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

package io.spine.validation

import io.spine.option.OptionsProto
import io.spine.protodata.ast.filePattern
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.settings.defaultConsumerId
import io.spine.protodata.testing.AbstractCompilationErrorTest
import io.spine.protodata.util.Format

/**
 * An abstract base for compilation error tests of [ValidationPlugin].
 */
internal abstract class CompilationErrorTest : AbstractCompilationErrorTest() {

    override fun plugins(): List<Plugin> = listOf(
        object : ValidationPlugin() {}
    )

    override fun writeSettings(settings: SettingsDirectory) {
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
