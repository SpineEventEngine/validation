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
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`NumberText` numbers should")
internal class NumberTextSpec {

    @Test
    fun `have a correct equality relationship`() {
        EqualsTester()
            .addEqualityGroup(NumberText("0.0"), NumberText("0.0"))
            .addEqualityGroup(NumberText("0.1"), NumberText("0.10"))
            .testEquals()
    }

    @Test
    fun `recognize that two numbers have different types`() {
        val plain = NumberText("1")
        val withDecimal = NumberText("1.0")

        plain.isOfSameType(withDecimal) shouldBe true
    }

    @Test
    fun `recognize that two numbers are of the same types`() {
        val fitsIntoByte = NumberText("4")
        val maxInt = NumberText(Int.MAX_VALUE.toString())

        fitsIntoByte.isOfSameType(maxInt) shouldBe true
    }

    @Test
    fun `compare values`() {
        val smaller = NumberText("0.1")
        val larger = NumberText("15")
        val comparison = smaller.toNumber().compareTo(larger.toNumber())

        (comparison < 0) shouldBe true
    }

    @Test
    fun `store numbers that do not fit into 'int'`() {
        val longMax = NumberText(Long.MAX_VALUE.toString())
        val lessThanLongMax = NumberText((Long.MAX_VALUE - 1).toString())

        longMax.toNumber().compareTo(lessThanLongMax.toNumber()) shouldBe 1
    }

    @ParameterizedTest
    @MethodSource("textNumbers")
    fun `stringify values`(input: Number, expected: String) {
        val text = NumberText(input)
        text.toString() shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("malformedNumbers")
    fun `throw on malformed numbers`(malformed: String) {
        assertThrows<NumberFormatException> {
            NumberText(malformed)
        }
    }

    @DisplayName("throw on a number with too many decimal separators")
    @Test
    fun throwOnTooManySeparators() {
        assertThrows<IllegalStateException> {
            NumberText("1.0.0")
        }
    }

    @Suppress("unused") // invoked via `@MethodSource`.
    companion object {

        @JvmStatic
        fun textNumbers(): Stream<Arguments> = Stream.of(
            Arguments.of(0.0, "0.0"),
            Arguments.of(0, "0"),
            Arguments.of(-1.0, "-1.0"),
            Arguments.of(-1, "-1"),
            Arguments.of(-1.23456789, "-1.23456789"),
            Arguments.of(-3L, "-3"),
            Arguments.of(-2.23456f, "-2.23456")
        )

        @JvmStatic
        fun malformedNumbers(): Stream<Arguments> = Stream.of(
            Arguments.of("1,0,0"),
            /* Even though the expressions below technically evaluate to a number,
               they are not allowed because we do not want to build and support
               a calculator on Protobuf options. */
            Arguments.of("1,0"),
            Arguments.of("2!"),
            Arguments.of("2/2"),
            Arguments.of("2+2"),
            Arguments.of("2-2")
        )
    }
}
