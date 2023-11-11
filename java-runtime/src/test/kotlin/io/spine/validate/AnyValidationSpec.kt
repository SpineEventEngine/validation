/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.validate

import io.spine.protobuf.AnyPacker
import io.spine.test.validate.RequiredStringValue
import io.spine.test.validate.anyfields.AnyContainer
import io.spine.test.validate.anyfields.UncheckedAnyContainer
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import io.spine.validate.given.MessageValidatorTestEnv.newStringValue
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName(VALIDATION_SHOULD + "when validating `google.protobuf.Any`")
internal class AnyValidationSpec : ValidationOfConstraintTest() {

    /**
     * Since the declaration of `RequiredMsgFieldValue` contains validation constraint
     * for the `value` field, the default instance of this type in invalid.
     */
    private val invalidMessage: @NonValidated RequiredStringValue =
        RequiredStringValue.getDefaultInstance()

    @Test
    fun `consider 'Any' valid if content is valid`() {
        val value = RequiredStringValue.newBuilder()
            .setValue(newStringValue())
            .build()
        val content = AnyPacker.pack(value)
        val container = AnyContainer.newBuilder()
            .setAny(content)
            .build()
        assertValid(container)
    }

    @Test
    fun `consider 'Any' not valid if content is not valid`() {
        val content = AnyPacker.pack(invalidMessage)
        val builder = AnyContainer.newBuilder().setAny(content)

        assertThrows<ValidationException> {
            builder.build()
        }

        assertNotValid(builder.buildPartial())
    }

    @Test
    fun `consider 'Any' valid if validation is not required`() {
        val content = AnyPacker.pack(invalidMessage)

        val builder = UncheckedAnyContainer.newBuilder().setAny(content)

        assertDoesNotThrow { builder.build() }

        assertValid(builder.build())
    }

    @Test
    fun `validate recursive messages`() {
        val internalAny = AnyPacker.pack(invalidMessage)
        val internal: @NonValidated AnyContainer = AnyContainer.newBuilder()
            .setAny(internalAny)
            .buildPartial()

        val external = AnyPacker.pack(internal)
        val builder = AnyContainer.newBuilder()
            .setAny(external)

        assertNotValid(builder.buildPartial())

        assertThrows<ValidationException> { builder.build() }
    }
}
