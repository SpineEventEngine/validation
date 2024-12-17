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

package io.spine.validation.required

import com.google.protobuf.Descriptors.Descriptor
import io.kotest.matchers.string.shouldContain
import io.spine.logging.testing.tapConsole
import io.spine.protodata.Compilation
import io.spine.testing.logging.mute.MuteLogging
import io.spine.validation.ValidationTestFixture
import io.spine.validation.given.required.WithBoolField
import io.spine.validation.given.required.WithIntField
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

@DisplayName("`RequiredPolicy` should")
internal class RequiredPolicySpec {

    private val doesNotSupportRequired = "does not support `(required)` validation."

    private lateinit var error: Compilation.Error

    @Test
    fun `reject option on a boolean field`(@TempDir outputDir: Path, @TempDir settingsDir: Path) {
        createAndRunPipeline(WithBoolField.getDescriptor(), outputDir, settingsDir)

        error.message.let {
            it shouldContain "The field `spine.validation.given.required.WithBoolField.really`"
            it shouldContain "of the type `bool`"
            it shouldContain doesNotSupportRequired
        }
    }

    @Test
    @MuteLogging
    fun `reject option on an integer field`(@TempDir outputDir: Path, @TempDir settingsDir: Path) {
        createAndRunPipeline(WithIntField.getDescriptor(), outputDir, settingsDir)

        error.message.let {
            it shouldContain "The field `spine.validation.given.required.WithIntField.zero`"
            it shouldContain "of the type `int32`"
            it shouldContain doesNotSupportRequired
        }
    }

    /**
     * Creates and runs a pipeline which handles only the proto type with the given [descriptor].
     */
    private fun createAndRunPipeline(
        descriptor: Descriptor,
        outputDir: Path,
        settingsDir: Path
    ) {
        val fixture = ValidationTestFixture(descriptor, outputDir, settingsDir)
        val pipeline = fixture.setup.createPipeline()
        error = assertThrows<Compilation.Error> {
            // Redirect console output so that we don't print errors during the build.
            tapConsole {
                pipeline()
            }
        }
    }
}

