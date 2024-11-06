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

import io.spine.testing.UtilityClassTest
import io.spine.validate.NumberConversion.check
import java.math.BigDecimal
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`NumberConversion` utility should")
internal class NumberConversionSpec :
    UtilityClassTest<NumberConversion>(NumberConversion::class.java) {

    @Nested inner class
    `tell that it is possible to convert to` {

        @Test
        fun byte() =
            assertTrue(check("1".toByte(), "2".toByte()))

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#shorts")
        fun short(shortNumber: Number) =
            assertTrue(check("1".toShort(), shortNumber))

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#integers")
        fun integer(integerNumber: Number) =
            assertTrue(check(1, integerNumber))

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#longs")
        fun long(longNumber: Number) =
            assertTrue(check(1L, longNumber))

        @Test
        fun float() =
            assertTrue(check(1.0f, 3.14f))

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#doubles")
        fun double(doubleNumber: Number) =
            assertTrue(check(1.0, doubleNumber))
    }

    @Nested inner class
    `tell that it is not possible to convert` {

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#nonBytes")
        fun `non-'byte' to 'byte'`(nonByte: Number) =
            assertFalse(check("1".toByte(), nonByte))

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#nonShorts")
        fun `non-'short' to 'short'`(nonShort: Number) {
            assertFalse(check("1".toShort(), nonShort))
        }

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#nonIntegers")
        fun `non-'integer' to 'integer'`(nonInteger: Number) =
            assertFalse(check(1, nonInteger))

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#nonLongs")
        fun `non-'long' to 'long'`(nonLong: Number) =
            assertFalse(check(1L, nonLong))

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#nonFloats")
        fun `non-'float' to 'float'`(nonFloat: Number) =
            assertFalse(check(1.0f, nonFloat))

        @ParameterizedTest
        @MethodSource("io.spine.validate.NumberConversionSpec#nonDoubles")
        fun `non-'double' to 'double'`(nonDouble: Number) {
            assertFalse(check(1.0, nonDouble))
        }
    }

    @Test
    fun `tell that 'ComparableNumber' instances are automatically unwrapped`() {
        val number = ComparableNumber(3)
        assertTrue(check(number, number))
    }

    @Test
    fun `tell that 'BigDecimal's are not supported`() =
        assertFalse(check(BigDecimal.valueOf(1), 1L))

    @Suppress("unused") /* Serves as a source for argument values. */
    companion object {

        @JvmStatic
        fun nonBytes(): Stream<Number> = Stream.concat(Stream.of("1".toShort()), nonShorts())

        @JvmStatic
        fun nonShorts(): Stream<Number> = Stream.concat(Stream.of(2), nonIntegers())

        @JvmStatic
        fun nonIntegers(): Stream<Number> = Stream.concat(Stream.of(3L), nonLongs())

        @JvmStatic
        fun nonLongs(): Stream<Number> = Stream.of(4.0, 5.1f)

        @JvmStatic
        fun nonFloats(): Stream<Number> = Stream.concat(nonDoubles(), Stream.of(4.0))

        @JvmStatic
        fun nonDoubles(): Stream<Number> = Stream.of("1".toByte(), "1".toShort(), 2, 3L)

        @JvmStatic
        fun shorts(): Stream<Number> = Stream.of("1".toByte(), "2".toShort())

        @JvmStatic
        fun integers(): Stream<Number> = Stream.concat(shorts(), Stream.of(2))

        @JvmStatic
        fun longs(): Stream<Number> = Stream.concat(integers(), Stream.of(3L))

        @JvmStatic
        fun doubles(): Stream<Number> = Stream.of(3.14f, 8.19)
    }
}
