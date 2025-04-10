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

import com.google.protobuf.Message
import com.google.protobuf.doubleValue
import io.kotest.matchers.string.shouldContain
import io.spine.test.validate.MaxExclusive
import io.spine.test.validate.MaxInclusive
import io.spine.test.validate.MinExclusive
import io.spine.test.validate.MinInclusive
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName(VALIDATION_SHOULD + "analyze `(min)` and `(max)` options and")
internal class NumberRangeSpec : ValidationOfConstraintTest() {

    @Test
    fun `consider number field is valid if no number options set`() = assertValid {
        doubleValue { value = 5.0 }
    }

    @Test
    fun `find out that number is greater than min inclusive`() =
        minNumberTest(GREATER_THAN_MIN, inclusive = true, valid = true)

    @Test
    fun `find out that number is equal to min inclusive`() =
        minNumberTest(EQUAL_MIN, inclusive = true, valid = true)

    @Test
    fun `find out that number is less than min inclusive`() =
        minNumberTest(LESS_THAN_MIN, inclusive = true, valid = false)

    @Test
    fun `find out that number is grated than min exclusive`() =
        minNumberTest(GREATER_THAN_MIN, inclusive = false, valid = true)

    @Test
    fun `find out that number is equal to min exclusive`() =
        minNumberTest(EQUAL_MIN, inclusive = false, valid = false)

    @Test
    fun `find out that number is less than min exclusive`() =
        minNumberTest(LESS_THAN_MIN, inclusive = false, valid = false)

    @Test
    fun `provide one valid violation if number is less than min`() {
        minNumberTest(LESS_THAN_MIN, inclusive = true, valid = false)
        val errorMessage = firstViolation().message.format()
        errorMessage shouldContain "must be >= $EQUAL_MIN"
    }

    @Test
    fun `find out that number is greater than max inclusive`() =
        maxNumberTest(GREATER_THAN_MAX, inclusive = true, valid = false)

    @Test
    fun `find out that number is equal to max inclusive`() =
        maxNumberTest(EQUAL_MAX, inclusive = true, valid = true)

    @Test
    fun `find out that number is less than max inclusive`() =
        maxNumberTest(LESS_THAN_MAX, inclusive = true, valid = true)

    @Test
    fun `find out that number is less than max non-inclusive`() =
        maxNumberTest(GREATER_THAN_MAX, inclusive = false, valid = false)

    @Test
    fun `find out that number is equal to max exclusive`() =
        maxNumberTest(EQUAL_MAX, inclusive = false, valid = false)

    @Test
    fun `find out that number is less than max exclusive`() =
        maxNumberTest(LESS_THAN_MAX, inclusive = false, valid = true)

    @Test
    fun `provide one valid violation if number is greater than max`() {
        maxNumberTest(GREATER_THAN_MAX, inclusive = true, valid = false)
        val errorMessage = firstViolation().message.format()
        errorMessage shouldContain "must be <= $EQUAL_MAX"
    }

    private fun msgMin(value: Double, inclusive: Boolean): Message {
        val builder = if (inclusive)
            MinInclusive.newBuilder().setValue(value)
        else MinExclusive.newBuilder().setValue(value)
        return builder.buildPartial()
    }

    private fun minNumberTest(value: Double, inclusive: Boolean, valid: Boolean) {
        val msg = msgMin(value, inclusive)
        validate(msg)
        assertIsValid(valid)
    }

    private fun maxNumberTest(value: Double, inclusive: Boolean, valid: Boolean) {
        val msg = msgMax(inclusive, value)
        validate(msg)
        assertIsValid(valid)
    }

    private fun msgMax(inclusive: Boolean, value: Double): @NonValidated Message {
        val builder = if (inclusive)
            MaxInclusive.newBuilder().setValue(value)
        else MaxExclusive.newBuilder().setValue(value)
        return builder.buildPartial()
    }

    companion object {
        const val EQUAL_MIN: Double = 16.5
        const val GREATER_THAN_MIN: Double = EQUAL_MIN + 5
        const val LESS_THAN_MIN: Double = EQUAL_MIN - 5

        const val EQUAL_MAX: Double = 64.5
        const val GREATER_THAN_MAX: Double = EQUAL_MAX + 5

        const val LESS_THAN_MAX: Double = EQUAL_MAX - 5
    }
}
