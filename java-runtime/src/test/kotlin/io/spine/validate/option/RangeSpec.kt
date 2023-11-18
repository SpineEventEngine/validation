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

package io.spine.validate.option

import io.spine.test.validate.Hours
import io.spine.test.validate.NumRanges
import io.spine.test.validate.RangesHolder
import io.spine.test.validate.hours
import io.spine.test.validate.rangesHolder
import io.spine.validate.ValidationOfConstraintTest
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import kotlin.streams.toList
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("unused") // methods are invoked via `@MethodSource`.
@DisplayName(VALIDATION_SHOULD + "analyze `(range)` option and find out that")
internal class RangeSpec : ValidationOfConstraintTest() {

    @Nested
    internal inner class Integers {

        @ParameterizedTest
        @MethodSource("io.spine.validate.option.RangeSpec#validHours")
        fun `fit into the defined range`(hour: Int) {
            val msg = hourRange(hour)
            assertValid(msg)
        }

        @ParameterizedTest
        @MethodSource("io.spine.validate.option.RangeSpec#invalidHours")
        @Disabled("Until 'skipValidation()` is turned off.")
        fun `do not fit into the defined range`(hour: Int) = assertDoesNotBuild {
            hourRange(hour)
        }

        private fun hourRange(hour: Int): NumRanges = validRange().setHour(hour).build()
    }

    @Nested
    internal inner class Longs {

        @ParameterizedTest
        @MethodSource("io.spine.validate.option.RangeSpec#validMinutes")
        fun `fit into the defined range`(minute: Long) = assertValid {
            minuteRange(minute)
        }

        @ParameterizedTest
        @MethodSource("io.spine.validate.option.RangeSpec#invalidMinutes")
        @Disabled("Until 'skipValidation()` is turned off.")
        fun `do not fit into the defined range`(minute: Long) = assertDoesNotBuild {
            minuteRange(minute)
        }

        private fun minuteRange(minute: Long): NumRanges =
            validRange().setMinute(minute).build()
    }

    @Nested
    internal inner class Floats {

        @ParameterizedTest
        @MethodSource("io.spine.validate.option.RangeSpec#validDegrees")
        fun `fit into the defined range`(degree: Float) = assertValid {
            floatRange(degree)
        }

        @ParameterizedTest
        @MethodSource("io.spine.validate.option.RangeSpec#invalidDegrees")
        @Disabled("Until 'skipValidation()` is turned off.")
        fun `do not fit into the defined range`(degree: Float) = assertDoesNotBuild {
            floatRange(degree)
        }

        private fun floatRange(degree: Float): NumRanges =
            validRange().setDegree(degree).build()
    }

    @Nested
    internal inner class Doubles {

        @ParameterizedTest
        @MethodSource("io.spine.validate.option.RangeSpec#validAngles")
        fun `fit into the defined range`(angle: Double) = assertValid {
            doubleRange(angle)
        }

        @ParameterizedTest
        @MethodSource("io.spine.validate.option.RangeSpec#invalidAngles")
        @Disabled("Until 'skipValidation()` is turned off.")
        fun `do not fit into the defined range`(angle: Double) = assertDoesNotBuild {
            doubleRange(angle)
        }

        private fun doubleRange(angle: Double): NumRanges =
            validRange().setAngle(angle).build()
    }

    @Nested
    @DisplayName("repeated option is")
    internal inner class RepeatedRange {

        @Test
        fun valid() = assertValid {
            hours {
                hour.addAll(validHours().toList())
            }
        }

        @Test
        @Disabled("Until 'skipValidation()` is turned off.")
        fun invalid() = assertDoesNotBuild {
            Hours.newBuilder()
                .addAllHour(invalidHours().toList())
                .build()
        }
    }

    companion object {

        @JvmStatic
        fun holderOf(ranges: NumRanges): RangesHolder = rangesHolder {
            this.ranges = ranges
        }

        @JvmStatic
        fun validRange(): NumRanges.Builder =
            NumRanges.newBuilder()
                .setHour(1)
                .setAngle(1.0)
                .setDegree(1f)
                .setMinute(1)

        @JvmStatic
        fun invalidHalfDayHours(): Stream<Int> =
            IntStream.of(-1, 13, 23, 24, Int.MAX_VALUE, Int.MIN_VALUE).boxed()

        @JvmStatic
        fun invalidHours(): Stream<Int> =
            IntStream.of(-1, 24, Int.MAX_VALUE, Int.MIN_VALUE).boxed()

        @JvmStatic
        fun validHours(): Stream<Int> = IntStream.range(0, 23).boxed()

        @JvmStatic
        fun validHalfDayHours(): Stream<Int> = IntStream.range(0, 12).boxed()

        @JvmStatic
        fun invalidHalfHourMinutes(): Stream<Long> =
            LongStream.of(-1, 31, 59, 60, Long.MAX_VALUE, Long.MIN_VALUE).boxed()

        @JvmStatic
        fun invalidMinutes(): Stream<Long> =
            LongStream.of(-1, 60, 61, Long.MAX_VALUE, Long.MIN_VALUE).boxed()

        @JvmStatic
        fun validMinutes(): Stream<Long> = LongStream.range(0, 59).boxed()

        @JvmStatic
        fun validHalfHourMinutes(): Stream<Long> = LongStream.range(0, 29).boxed()

        @JvmStatic
        fun invalidHalfRangeDegrees(): Stream<Float> = DoubleStream.of(
            -1.0,
            180.0,
            180.1,
            Float.MAX_VALUE.toDouble(),
            (-Float.MAX_VALUE).toDouble()
        )
            .boxed()
            .map { obj: Double -> obj.toFloat() }

        @JvmStatic
        fun invalidDegrees(): Stream<Float> = DoubleStream.of(
            -1.0,
            360.0,
            360.1,
            Float.MAX_VALUE.toDouble(),
            (-Float.MAX_VALUE).toDouble()
        )
            .boxed()
            .map { obj: Double -> obj.toFloat() }

        @JvmStatic
        fun validDegrees(): Stream<Float> =
            DoubleStream.of(0.0, 0.54, 1.23, 31.3, 40.0, 59.9, 180.1, 359.9).boxed()
                .map { obj: Double -> obj.toFloat() }

        @JvmStatic
        fun validHalfRangeDegrees(): Stream<Float> =
            DoubleStream.of(0.0, 0.54, 1.23, 31.3, 40.0, 59.9, 179.9).boxed()
                .map { obj: Double -> obj.toFloat() }

        @JvmStatic
        fun invalidHalfRangeAngles(): Stream<Double> =
            DoubleStream.of(-1.0, 0.0, 90.0, 90.1, Double.MAX_VALUE, -Double.MAX_VALUE).boxed()

        @JvmStatic
        fun invalidAngles(): Stream<Double> =
            DoubleStream.of(-1.0, 0.0, 180.0, 180.1, Double.MAX_VALUE, -Double.MAX_VALUE).boxed()

        @JvmStatic
        fun validAngles(): Stream<Double> =
            DoubleStream.of(0.01, 0.54, 1.23, 31.3, 40.0, 59.9, 179.9).boxed()

        @JvmStatic
        fun validHalfRangeAngles(): Stream<Double> =
            DoubleStream.of(0.01, 0.54, 1.23, 31.3, 40.0, 59.9, 89.9).boxed()
    }
}
