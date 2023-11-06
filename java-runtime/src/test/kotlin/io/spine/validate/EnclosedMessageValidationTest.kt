/*
 * Copyright 2023, TeamDev. All rights reserved.
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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.spine.test.validate.EnclosedMessageFieldValue
import io.spine.test.validate.EnclosedMessageFieldValueWithCustomInvalidMessage
import io.spine.test.validate.EnclosedMessageFieldValueWithoutAnnotationFieldValueWithCustomInvalidMessage
import io.spine.test.validate.EnclosedMessageWithRequiredString
import io.spine.test.validate.EnclosedMessageWithoutAnnotationFieldValue
import io.spine.test.validate.PatternStringFieldValue
import io.spine.validate.given.MessageValidatorTestEnv
import io.spine.validate.given.MessageValidatorTestEnv.assertFieldPathIs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName(ValidationOfConstraintTest.VALIDATION_SHOULD + "validate enclosed messages and")
internal class EnclosedMessageValidationTest : ValidationOfConstraintTest() {

    @Test
    fun `find out that enclosed message field is valid`() {
        val enclosedMsg = PatternStringFieldValue.newBuilder()
            .setEmail("valid.email@mail.com")
            .build()
        val msg = EnclosedMessageFieldValue.newBuilder()
            .setOuterMsgField(enclosedMsg)
            .build()
        assertValid(msg)
    }

    @Test
    fun `find out enclosed message field is NOT valid`() {
        val enclosedMsg = PatternStringFieldValue.newBuilder()
            .setEmail("invalid email")
            .buildPartial()
        val msg = EnclosedMessageFieldValue.newBuilder()
            .setOuterMsgField(enclosedMsg)
            .buildPartial()
        assertNotValid(msg)
    }

    @Test
    fun `consider field valid if no valid option is set`() {
        val enclosedMsg = PatternStringFieldValue.newBuilder()
            .setEmail("invalid email")
            .build()
        val msg = EnclosedMessageWithoutAnnotationFieldValue.newBuilder()
            .setOuterMsgField(enclosedMsg)
            .build()
        assertValid(msg)
    }

    @Test
    fun `consider field valid if it is not set`() {
        val msg = EnclosedMessageWithRequiredString.getDefaultInstance()
        assertValid(msg)
    }

    @Test
    fun `provide valid violations if enclosed message field is not valid`() {
        val enclosedMsg = PatternStringFieldValue.newBuilder()
            .setEmail("invalid email")
            .buildPartial()
        val msg = EnclosedMessageFieldValue.newBuilder()
            .setOuterMsgField(enclosedMsg)
            .buildPartial()
        validate(msg)

        val violation = singleViolation()
        assertEquals("The message must have valid properties.", violation.msgFormat)
        assertFieldPathIs(
            violation,
            MessageValidatorTestEnv.OUTER_MSG_FIELD
        )
        val innerViolations = violation.violationList
        assertEquals(1, innerViolations.size)

        val innerViolation = innerViolations[0]

        innerViolation.msgFormat shouldStartWith Diags.Regex.prefix

        assertFieldPathIs(
            innerViolation,
            MessageValidatorTestEnv.OUTER_MSG_FIELD,
            MessageValidatorTestEnv.EMAIL
        )

        innerViolation.violationList.shouldBeEmpty()
    }

    @Test
    fun `provide custom invalid field message if specified`() {
        val enclosedMsg: @NonValidated PatternStringFieldValue =
            PatternStringFieldValue.newBuilder()
                .setEmail("invalid email")
                .buildPartial()
        val msg: @NonValidated EnclosedMessageFieldValueWithCustomInvalidMessage =
            EnclosedMessageFieldValueWithCustomInvalidMessage.newBuilder()
                .setOuterMsgField(enclosedMsg)
                .buildPartial()

        validate(msg)

        singleViolation().msgFormat shouldBe "Custom error"
    }

    @Test
    fun `ignore custom invalid field message if validation is disabled`() {
        val msg = EnclosedMessageFieldValueWithoutAnnotationFieldValueWithCustomInvalidMessage
            .getDefaultInstance()
        assertValid(msg)
    }
}
