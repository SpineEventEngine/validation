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

package io.spine.validate;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.spine.testing.Assertions.assertIllegalState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`NumberText` numbers should")
class NumberTextTest {

    @Test
    @DisplayName("have a correct equality relationship")
    void testEquals() {
        var equalsTester = new EqualsTester();
        equalsTester.addEqualityGroup(new NumberText("0.0"), new NumberText("0.0"))
                    .addEqualityGroup(new NumberText("0.1"), new NumberText("0.10"))
                    .testEquals();
    }

    @Test
    @DisplayName("recognize that two numbers have different types")
    void differentTypeTest() {
        var plainNumber = "1";
        var numberWithDecimalPart = "1.0";
        var plain = new NumberText(plainNumber);
        var withDecimal = new NumberText(numberWithDecimalPart);
        assertFalse(plain.isOfSameType(withDecimal));
    }

    @Test
    @DisplayName("recognize that two numbers are of the same types")
    void sameTypeTest() {
        var fitsIntoByte = "4";
        var maxInteger = String.valueOf(Integer.MAX_VALUE);
        var small = new NumberText(fitsIntoByte);
        var large = new NumberText(maxInteger);
        assertTrue(small.isOfSameType(large));
    }

    @Test
    @DisplayName("compare values")
    void comparisonTest() {
        var smallerValue = "0.1";
        var largerValue = "15";
        var smaller = new NumberText(smallerValue);
        var larger = new NumberText(largerValue);
        var comparison = smaller.toNumber()
                                .compareTo(larger.toNumber());
        assertTrue(comparison < 0);
    }

    @Test
    @DisplayName("store numbers that do not fit into `int`")
    void testDoesNotFitIntoInt() {
        var longMaxValue = String.valueOf(Long.MAX_VALUE);
        var lessThanLongMaxValue = String.valueOf(Long.MAX_VALUE - 1);
        var larger = new NumberText(longMaxValue);
        var smaller = new NumberText(lessThanLongMaxValue);
        assertEquals(1, larger.toNumber()
                              .compareTo(smaller.toNumber()));
    }

    @ParameterizedTest
    @MethodSource("textNumbers")
    @DisplayName("stringify values")
    void toStringTest(Number input, String expected) {
        var text = new NumberText(input);
        assertEquals(expected, text.toString());
    }

    @SuppressWarnings("unused") // invoked via `@MethodSource`.
    private static Stream<Arguments> textNumbers() {
        return Stream.of(
                Arguments.of(0.0d, "0.0"),
                Arguments.of(0, "0"),
                Arguments.of(-1.0d, "-1.0"),
                Arguments.of(-1, "-1"),
                Arguments.of(-1.23456789d, "-1.23456789"),
                Arguments.of(-3L, "-3"),
                Arguments.of(-2.23456f, "-2.23456")
        );
    }

    @DisplayName("throw on malformed numbers")
    @ParameterizedTest
    @MethodSource("malformedNumbers")
    void throwOnMalformedNumbers(String malformed) {
        assertThrows(NumberFormatException.class, () -> new NumberText(malformed));
    }

    @DisplayName("throw on a number with too many decimal separators")
    @Test
    void throwOnTooManySeparators() {
        assertIllegalState(() -> new NumberText("1.0.0"));
    }

    @SuppressWarnings("unused") // invoked via `@MethodSource`.
    private static Stream<Arguments> malformedNumbers() {
        return Stream.of(
                Arguments.of("1,0,0"),
                Arguments.of("1,0"),
                // Even though those are technically expressions that evaluate to a number,
                // they are not allowed.
                Arguments.of("2!"),
                Arguments.of("2/2"),
                Arguments.of("2+2"),
                Arguments.of("2-2")
        );
    }
}
