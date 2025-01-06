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
import io.spine.protodata.protobuf.field
import io.spine.validation.ValidationTestFixture
import io.spine.validation.given.required.WithBoolField
import io.spine.validation.given.required.WithDoubleField
import io.spine.validation.given.required.WithIntField
import io.spine.validation.given.required.WithSignedInt
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

@DisplayName("`RequiredPolicy` should")
internal class RequiredPolicySpec {

    @Test
    fun `reject option on a boolean field`(@TempDir workingDir: Path) {
        val message = WithBoolField.getDescriptor()
        val error = compile(message, workingDir)

        val field = message.field("really")
        val expected = fieldDoesNotSupportRequired(field)
        error.message shouldContain expected
    }

    @Test
    fun `reject option on an integer field`(@TempDir workingDir: Path) {
        val message = WithIntField.getDescriptor()
        val error = compile(message, workingDir)

        val field = message.field("zero")
        val expected = fieldDoesNotSupportRequired(field)
        error.message shouldContain expected
    }

    @Test
    fun `reject option on a signed integer field`(@TempDir workingDir: Path) {
        val message = WithSignedInt.getDescriptor()
        val error = compile(message, workingDir)

        val field = message.field("signed")
        val expected = fieldDoesNotSupportRequired(field)
        error.message shouldContain expected
    }

    @Test
    fun `reject option on a double field`(@TempDir workingDir: Path) {
        val message = WithDoubleField.getDescriptor()
        val error = compile(message, workingDir)

        val field = message.field("temperature")
        val expected = fieldDoesNotSupportRequired(field)
        error.message shouldContain expected
    }

    /**
     * Creates and runs a pipeline which handles only the proto type with the given [descriptor].
     */
    private fun compile(
        descriptor: Descriptor,
        workingDir: Path
    ): Compilation.Error {
        val fixture = ValidationTestFixture(descriptor, workingDir)
        val pipeline = fixture.setup.createPipeline()
        val error = assertThrows<Compilation.Error> {
            // Redirect console output so that we don't print errors during the build.
            tapConsole {
                pipeline()
            }
        }
        return error
    }
}
