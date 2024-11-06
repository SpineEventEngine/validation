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

package io.spine.validate

import com.google.common.testing.EqualsTester
import com.google.common.testing.NullPointerTester
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`ComparableNumber` should")
internal class ComparableNumberSpec {

    @Test
    fun `not accept nulls`() {
        val tester = NullPointerTester()
        tester.testAllPublicConstructors(ComparableNumber::class.java)
        tester.testAllPublicInstanceMethods(ComparableNumber(42))
    }

    @Nested inner class
    `have a consistent equality relationship` {

        @Test
        fun `between instances`() {
            val longMaxValue = Long.MAX_VALUE.toString()
            val doubleMinValue = Double.MIN_VALUE.toString()
            EqualsTester()
                .addEqualityGroup(NumberText(1L).toNumber(), NumberText("1").toNumber())
                .addEqualityGroup(
                    NumberText(longMaxValue).toNumber(),
                    NumberText(Long.MAX_VALUE).toNumber()
                )
                .addEqualityGroup(
                    NumberText(doubleMinValue).toNumber(),
                    NumberText(Double.MIN_VALUE).toNumber()
                )
                .testEquals()
        }

        @Test
        fun `between instances and primitives`() {
            val doubleValue = Double.MAX_VALUE
            val intValue = Int.MAX_VALUE
            val floatValue = Float.MAX_VALUE
            val longValue = Long.MAX_VALUE

            EqualsTester()
                .addEqualityGroup(doubleValue, ComparableNumber(doubleValue).toDouble())
                .addEqualityGroup(intValue, ComparableNumber(intValue).toInt())
                .addEqualityGroup(floatValue, ComparableNumber(floatValue).toFloat())
                .addEqualityGroup(longValue, ComparableNumber(longValue).toLong())
                .testEquals()
        }
    }
}
