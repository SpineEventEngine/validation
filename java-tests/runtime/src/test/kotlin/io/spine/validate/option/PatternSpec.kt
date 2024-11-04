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

package io.spine.validate.option

import com.google.protobuf.StringValue
import io.spine.test.validate.AllThePatterns
import io.spine.test.validate.PatternStringFieldValue
import io.spine.validate.Diags.Regex.errorMessage
import io.spine.validate.NonValidated
import io.spine.validate.ValidationOfConstraintTest
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import io.spine.validate.given.MessageValidatorTestEnv
import org.checkerframework.checker.regex.qual.Regex
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName(VALIDATION_SHOULD + "analyze `(pattern)` option and")
internal class PatternSpec : ValidationOfConstraintTest() {

    /** As defined in the stub message type [PatternStringFieldValue]. */
    private val regex: @Regex String =
        "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"

    @Test
    fun `find out that string matches to regex pattern`() =
        assertValid(patternStringFor("valid.email@mail.com"))

    @Test
    fun `find out that string does not match to regex pattern`() =
        assertNotValid(patternStringFor("invalid email"))

    @Test
    fun `consider field is valid if 'PatternOption' is not set`() =
        assertValid(StringValue.getDefaultInstance())

    @Test
    fun `provide one valid violation if string does not match the regex pattern`() {
        val msg = patternStringFor("invalid email")

        val expectedErrMsg = errorMessage(regex).replace("\\\\", "\\")

        assertSingleViolation(msg, expectedErrMsg, MessageValidatorTestEnv.EMAIL)
    }

    @Test
    fun `validate with 'case_insensitive' modifier`() {
        val message = AllThePatterns.newBuilder()
            .setLetters("AbC")
            .buildPartial()
        assertValid(message)

        val invalid = AllThePatterns.newBuilder()
            .setLetters("12345")
            .buildPartial()
        assertNotValid(invalid)
    }

    @Test
    fun `validate with 'multiline' modifier`() {
        val message = AllThePatterns.newBuilder()
            .setManyLines("text" + System.lineSeparator() + "more text")
            .buildPartial()
        assertValid(message)

        val invalid = AllThePatterns.newBuilder()
            .setManyLines("single line text")
            .buildPartial()
        assertNotValid(invalid)
    }

    @Test
    fun `validate with 'partial' modifier`() {
        val message = AllThePatterns.newBuilder()
            .setPartial("Hello World!")
            .buildPartial()
        assertValid(message)

        val invalid = AllThePatterns.newBuilder()
            .setPartial("123456")
            .buildPartial()
        assertNotValid(invalid)
    }

    @Test
    fun `validate with 'unicode' modifier`() {
        val message = AllThePatterns.newBuilder()
            .setUtf8("ґ")
            .buildPartial()
        assertValid(message)

        val invalid = AllThePatterns.newBuilder()
            .setUtf8("\\\\")
            .buildPartial()
        assertNotValid(invalid)
    }

    @Test
    fun `validate with 'dot_all' modifier`() {
        val message = AllThePatterns.newBuilder()
            .setDotAll("ab" + System.lineSeparator() + "cd")
            .buildPartial()
        assertValid(message)
    }

    private fun patternStringFor(email: String): @NonValidated PatternStringFieldValue {
        return PatternStringFieldValue.newBuilder()
            .setEmail(email)
            .buildPartial()
    }
}
