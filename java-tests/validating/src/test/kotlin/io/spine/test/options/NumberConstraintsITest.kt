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

package io.spine.test.options

import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.test.tools.validate.InterestRate
import io.spine.test.tools.validate.Probability
import io.spine.test.tools.validate.Year
import io.spine.validate.format
import io.spine.validation.RangeFieldExtrema
import io.spine.validation.assertions.assertInvalid
import io.spine.validation.assertions.assertValid
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Number boundaries constraints should be compiled so that")
internal class NumberConstraintsITest {

    @Test
    fun `min value is checked`() {
        assertViolation(
            InterestRate.newBuilder().setPercent(-3f),
            "greater than 0.0"
        )
        assertValid(
            InterestRate.newBuilder()
                .setPercent(117.3f)
        )
    }

    @Test
    fun `min and max values are checked`() {
        assertViolation(
            Year.newBuilder()
                .setDayCount(42),
            "greater than or equal to 365"
        )
        assertViolation(
            Year.newBuilder()
                .setDayCount(420),
            "less than or equal to 366"
        )
        assertValid(
            Year.newBuilder()
                .setDayCount(365)
        )
        assertValid(
            Year.newBuilder()
                .setDayCount(366)
        )
    }

    @Test
    fun `numerical range is checked`() {
        assertViolation(
            Probability.newBuilder()
                .setValue(1.1),
            "1.1"
        )
        assertViolation(
            Probability.newBuilder()
                .setValue(-0.1),
            "-0.1"
        )
        assertValid(
            Probability.newBuilder()
                .setValue(0.0)
        )
        assertValid(
            Probability.newBuilder()
                .setValue(1.0)
        )
    }

    @Test
    fun `numerical range handles minimum values of the field type`() {
        val intMinValue = Int.MIN_VALUE
        val longMinValue = Long.MIN_VALUE
        val uintMinValue = UInt.MIN_VALUE.toInt()
        val ulongMinValue = ULong.MIN_VALUE.toLong()
        assertValid(
            RangeFieldExtrema.newBuilder()
                .setFloat(-Float.MAX_VALUE)
                .setDouble(-Double.MAX_VALUE)
                .setInt32(intMinValue)
                .setInt64(longMinValue)
                .setUint32(uintMinValue)
                .setUint64(ulongMinValue)
                .setSint32(intMinValue)
                .setSint64(longMinValue)
                .setFixed32(uintMinValue)
                .setFixed64(ulongMinValue)
                .setSfixed32(intMinValue)
                .setSfixed64(longMinValue)
        )
    }

    @Test
    fun `numerical range handles maximum values of the field type`() {
        val intMaxValue = Int.MAX_VALUE
        val longMaxValue = Long.MAX_VALUE
        val uintMaxValue = UInt.MAX_VALUE.toInt()
        val ulongMaxValue = ULong.MAX_VALUE.toLong()
        assertValid(
            RangeFieldExtrema.newBuilder()
                .setFloat(Float.MAX_VALUE)
                .setDouble(Double.MAX_VALUE)
                .setInt32(intMaxValue)
                .setInt64(longMaxValue)
                .setUint32(uintMaxValue)
                .setUint64(ulongMaxValue)
                .setSint32(intMaxValue)
                .setSint64(longMaxValue)
                .setFixed32(uintMaxValue)
                .setFixed64(ulongMaxValue)
                .setSfixed32(intMaxValue)
                .setSfixed64(longMaxValue)
        )
    }
}

private fun assertViolation(message: Message.Builder, error: String) {
    val violations = assertInvalid(message)
    violations.size shouldBe 1
    violations[0] shouldNotBe null
    violations[0].message.format() shouldContain error
}
