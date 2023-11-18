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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.spine.option.OptionsProto
import io.spine.test.validate.CustomMessageRequiredByteStringFieldValue
import io.spine.test.validate.CustomMessageRequiredEnumFieldValue
import io.spine.test.validate.CustomMessageRequiredMsgFieldValue
import io.spine.test.validate.CustomMessageRequiredRepeatedMsgFieldValue
import io.spine.test.validate.CustomMessageRequiredStringFieldValue
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName(VALIDATION_SHOULD + "use `if_missing` field option")
@Disabled("Until 'skipValidation()` is turned off.")
internal class IfMissingErrorMsgSpec : ValidationOfConstraintTest() {

    @Nested inner class
    `to obtain custom error message when validating on runtime if` {

        @Test
        fun `'Message' field is NOT set`() = assertErrorMessage(
            CustomMessageRequiredMsgFieldValue.getDefaultInstance()
        )

        @Test
        fun `string field is NOT set`() = assertErrorMessage(
            CustomMessageRequiredStringFieldValue.getDefaultInstance()
        )

        @Test
        fun `'ByteString' field is NOT set`() = assertErrorMessage(
            CustomMessageRequiredByteStringFieldValue.getDefaultInstance()
        )

        @Test
        fun `'repeated' field is NOT set`() = assertErrorMessage(
            CustomMessageRequiredRepeatedMsgFieldValue.getDefaultInstance()
        )

        @Test
        fun `enum field is NOT set`() = assertErrorMessage(
            CustomMessageRequiredEnumFieldValue.getDefaultInstance()
        )

        private fun assertErrorMessage(message: Message) {
            assertNotValid(message)
            val expectedErrorMessage = customErrorMessageFrom(message.descriptorForType)
            checkErrorMessage(expectedErrorMessage)
        }

        private fun checkErrorMessage(expectedMessage: String) {
            val constraintViolation = firstViolation()
            constraintViolation.msgFormat shouldBe expectedMessage
        }
    }

    @Nested inner class
    `to obtain custom error message when validating on 'build' method` {

        @Test
        fun `'Message' field`() = assertErrorMessage(
            CustomMessageRequiredMsgFieldValue.newBuilder(),
            CustomMessageRequiredMsgFieldValue.getDefaultInstance()
        )

        @Test
        fun `string field`() = assertErrorMessage(
            CustomMessageRequiredStringFieldValue.newBuilder(),
            CustomMessageRequiredStringFieldValue.getDefaultInstance()
        )

        @Test
        fun `'ByteString'`() = assertErrorMessage(
            CustomMessageRequiredByteStringFieldValue.newBuilder(),
            CustomMessageRequiredByteStringFieldValue.getDefaultInstance()
        )

        @Test
        fun `'repeated' field`() = assertErrorMessage(
            CustomMessageRequiredRepeatedMsgFieldValue.newBuilder(),
            CustomMessageRequiredRepeatedMsgFieldValue.getDefaultInstance()
        )

        @Test
        fun `enum field`() = assertErrorMessage(
            CustomMessageRequiredEnumFieldValue.newBuilder(),
            CustomMessageRequiredEnumFieldValue.getDefaultInstance()
        )

        private fun assertErrorMessage(builder: Message.Builder, defaultInstance: Message) {
            val exception = assertThrows<ValidationException> {
                builder.build()
            }
            val violation = exception.constraintViolations.first()
            violation.msgFormat shouldBe customErrorMessageFrom(
                defaultInstance.descriptorForType)
        }
    }

    companion object {

        private fun customErrorMessageFrom(descriptor: Descriptor): String {
            val firstFieldDescriptor = descriptor.fields[0]
            return firstFieldDescriptor.options
                .getExtension(OptionsProto.ifMissing)
                .errorMsg
        }
    }
}

