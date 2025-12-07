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

package io.spine.tools.validation

import com.google.protobuf.Message
import io.spine.option.OptionsProto
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.filePattern
import io.spine.tools.compiler.plugin.Plugin
import io.spine.tools.compiler.protobuf.descriptor
import io.spine.tools.compiler.protobuf.field
import io.spine.tools.compiler.settings.SettingsDirectory
import io.spine.tools.compiler.settings.defaultConsumerId
import io.spine.testing.compiler.AbstractCompilationErrorTest
import io.spine.format.Format
import io.spine.validation.ValidationPlugin
import io.spine.validation.messageMarkers
import io.spine.validation.validationConfig
import kotlin.reflect.KClass

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
            Format.ProtoJson,
            config.toByteArray()
        )
    }
}

/**
 * Asserts that the given [message] does not compile.
 *
 * @param message The class of the message to compile.
 * @param fieldName The name of the field to use for assertions.
 * @param errorMessageAssertions Assertions to run upon the returned error message.
 */
internal fun CompilationErrorTest.assertCompilationFails(
    message: KClass<out Message>,
    fieldName: String = "value",
    errorMessageAssertions: String.(Field) -> Unit
) {
    val descriptor = message.descriptor
    val error = assertCompilationFails(descriptor)
    val field = descriptor.field(fieldName)
    error.message!!.errorMessageAssertions(field)
}
