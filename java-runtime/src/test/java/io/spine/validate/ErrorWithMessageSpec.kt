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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName(
    VALIDATION_SHOULD + "propagate proper error message if custom message set and required"
)
internal class ErrorWithMessageSpec : ValidationOfConstraintTest() {

    @Test
    fun `Message field is NOT set`() = assertErrorMessage(
        CustomMessageRequiredMsgFieldValue.getDefaultInstance()
    )

    @Test
    fun `String field is NOT set`() = assertErrorMessage(
        CustomMessageRequiredStringFieldValue.getDefaultInstance()
    )

    @Test
    @DisplayName("ByteString field is NOT set")
    fun bytesNotSet() {
        val invalidMsg = CustomMessageRequiredByteStringFieldValue.getDefaultInstance()
        assertErrorMessage(invalidMsg)
    }

    @Test
    @DisplayName("repeated field is NOT set")
    fun repeatedNotSet() {
        val invalidMsg = CustomMessageRequiredRepeatedMsgFieldValue.getDefaultInstance()
        assertErrorMessage(invalidMsg)
    }

    @Test
    @DisplayName("Enum field is NOT set")
    fun enumNotSet() {
        val invalidMsg = CustomMessageRequiredEnumFieldValue.getDefaultInstance()
        assertErrorMessage(invalidMsg)
    }

    private fun assertErrorMessage(message: Message) {
        assertNotValid(message)
        val expectedErrorMessage = customErrorMessageFrom(message.descriptorForType)
        checkErrorMessage(expectedErrorMessage)
    }

    private fun checkErrorMessage(expectedMessage: String) {
        val constraintViolation = firstViolation()
        constraintViolation.msgFormat shouldBe expectedMessage
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

