/*
 * Copyright 2024, TeamDev. All rights reserved.
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
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.spine.test.validate.NotValidateWithCustomMessage
import io.spine.test.validate.PatternStringFieldValue
import io.spine.test.validate.ValidateEnclosed
import io.spine.test.validate.ValidateWithCustomMessage
import io.spine.test.validate.ValidateWithRequiredString
import io.spine.test.validate.notValidateEnclosed
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import io.spine.validate.given.MessageValidatorTestEnv.EMAIL
import io.spine.validate.given.MessageValidatorTestEnv.ENCLOSED_FIELD_NAME
import io.spine.validate.text.format
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@DisplayName(VALIDATION_SHOULD + "validate enclosed messages and")
internal class EnclosedMessageValidationSpec : ValidationOfConstraintTest() {

    @Test
    fun `find out that enclosed message field is valid`() {
        val enclosedMsg = PatternStringFieldValue.newBuilder()
            .setEmail("valid.email@mail.com")
            .build()
        val msg = ValidateEnclosed.newBuilder()
            .setEnclosed(enclosedMsg)
            .build()
        assertValid(msg)
    }

    @Test
    fun `find out enclosed message field is NOT valid`() {
        val enclosedMsg = PatternStringFieldValue.newBuilder()
            .setEmail("invalid email")
            .buildPartial()
        val msg = ValidateEnclosed.newBuilder()
            .setEnclosed(enclosedMsg)
            .buildPartial()
        assertNotValid(msg)
    }

    @Test
    fun `consider field valid if no valid option is set`() {
        val enclosedMsg: @NonValidated PatternStringFieldValue =
            PatternStringFieldValue.newBuilder()
                .setEmail("invalid email")
                .buildPartial()
        val msg = notValidateEnclosed {
            enclosed = enclosedMsg
        }
        assertValid(msg)
    }

    @Test
    fun `consider field valid if it is not set`() {
        val msg = ValidateWithRequiredString.getDefaultInstance()
        assertValid(msg)
    }

    @Test
    fun `provide valid violations if enclosed message field is not valid`() {
        val enclosedMsg = PatternStringFieldValue.newBuilder()
            .setEmail("invalid email")
            .buildPartial()
        val msg = ValidateEnclosed.newBuilder()
            .setEnclosed(enclosedMsg)
            .buildPartial()
        validate(msg)

        val violation = singleViolation()
        violation.message.withPlaceholders shouldContain "is invalid"
        assertFieldPathIs(
            violation,
            ENCLOSED_FIELD_NAME
        )

        val innerViolations = violation.violationList
        innerViolations shouldHaveSize  1
        val innerViolation = innerViolations[0]
        innerViolation.message.withPlaceholders shouldStartWith Diags.Regex.prefix

        assertFieldPathIs(
            innerViolation,
            EMAIL
        )

        innerViolation.violationList.shouldBeEmpty()
    }

    @Test
    fun `provide custom invalid field message if specified`() {
        val enclosedMsg: @NonValidated PatternStringFieldValue =
            PatternStringFieldValue.newBuilder()
                .setEmail("invalid email")
                .buildPartial()
        val msg: @NonValidated ValidateWithCustomMessage = ValidateWithCustomMessage.newBuilder()
                .setEnclosed(enclosedMsg)
                .buildPartial()

        validate(msg)

        singleViolation().message.format() shouldBe "Custom error"
    }

    /**
     * This method tests that setting only a custom message for a validation constraint
     * does not cause validation, neither runtime, nor on `build()`.
     */
    @Test
    fun `ignore custom invalid field message if validation is disabled`() {
        assertValid(NotValidateWithCustomMessage.getDefaultInstance())
        assertDoesNotThrow { NotValidateWithCustomMessage.newBuilder().build() }
    }
}
