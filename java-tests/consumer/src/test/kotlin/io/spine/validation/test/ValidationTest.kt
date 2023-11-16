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
package io.spine.validation.test

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.base.Identifier
import io.spine.protobuf.AnyPacker
import io.spine.validate.ConstraintViolation
import io.spine.validate.ValidationException
import io.spine.validation.test.money.LocalTime
import io.spine.validation.test.money.Mru
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Generated validation code should")
internal class ValidationTest {

    companion object {
        const val ALLOW_VALID = "and allow valid values"
        const val PROHIBIT_INVALID = "and prohibit invalid values"
        const val IGNORE_UNKNOWN_INVALID = "and ignore invalid packed values if type is unknown"
    }

    @CanIgnoreReturnValue
    private fun assertValidationException(builder: Message.Builder): ConstraintViolation {
        val exception = assertThrows<ValidationException> {
            builder.build()
        }
        val error = exception.asMessage()
        error.constraintViolationList shouldHaveSize 1
        return error.constraintViolationList[0]
    }

    private fun assertNoException(builder: Message.Builder) {
        try {
            val result: Any = builder.build()
            result shouldNotBe null
        } catch (e: ValidationException) {
            fail<Any>("Unexpected constraint violation: " + e.constraintViolations, e)
        }
    }

    private fun validAuthor(): Author = author {
        name = "Vernon"
    }

    @Nested
    @DisplayName("reflect a rule with a less (`<`) sign and")
    internal inner class LessRule {

        @Test
        @DisplayName("throw `ValidationException` if actual value is greater than the threshold")
        fun throwOnMore() {
            assertValidationException(Mru.newBuilder().setKhoums(6))
        }

        @Test
        @DisplayName("throw `ValidationException` if actual value is equal to the threshold")
        fun throwOnEdge() {
            assertValidationException(Mru.newBuilder().setKhoums(5))
        }

        @Test
        @DisplayName("throw no exceptions if actual value is less than the threshold")
        fun notThrow() {
            assertNoException(Mru.newBuilder().setKhoums(4))
        }
    }

    @Nested
    @DisplayName("reflect a rule with a greater or equal (`>=`) sign and")
    internal inner class GreaterRule {
        @Test
        @DisplayName("throw `ValidationException` if actual value is less than the threshold")
        fun throwOnMore() {
            val violation = assertValidationException(
                LocalTime.newBuilder()
                    .setHours(-1)
            )
            assertThat(violation.msgFormat)
                .contains("cannot be negative")
        }

        @Test
        @DisplayName("throw no exceptions if actual value is equal to the threshold")
        fun notThrowOnEdge() {
            assertNoException(LocalTime.newBuilder().setMinutes(0))
        }

        @Test
        @DisplayName("throw no exceptions if actual value is greater than the threshold")
        fun notThrow() {
            assertNoException(LocalTime.newBuilder().setMinutes(1))
        }
    }

    @Nested
    @DisplayName("reflect a `(required)` rule and")
    internal inner class Required {

        @Test
        @DisplayName("check string field")
        fun throwForString() {
            val builder = Author.newBuilder()
            val violation = assertValidationException(builder)
            assertThat(violation.msgFormat)
                .contains("Author must have a name")
        }

        @Test
        @DisplayName("check message field")
        fun throwForMessage() {
            val builder = Book.newBuilder()
            val violation = assertValidationException(builder)
            assertThat(violation.msgFormat).contains("value must be set")
            assertThat(violation.fieldPath.getFieldName(0)).isEqualTo("author")
        }

        @Test
        @DisplayName("pass if a value is set")
        fun passIfSet() {
            val builder = Author.newBuilder().setName("Evans")
            assertNoException(builder)
        }

        @Test
        @DisplayName("pass if not required")
        fun passIfNotRequired() {
            val builder = Book.newBuilder().setAuthor(validAuthor())
            assertNoException(builder)
        }

        @Test
        @DisplayName("throw `ValidationException` if a list contains only default values")
        fun empty() {
            val builder = Blizzard.newBuilder()
                .addSnowflake(Snowflake.getDefaultInstance())
            assertValidationException(builder)
        }

        @Test
        @DisplayName("pass if a list contains all non-default values")
        fun nonDefaultList() {
            val builder = Blizzard.newBuilder()
                .addSnowflake(
                    Snowflake.newBuilder()
                        .setEdges(3)
                        .setVertices(3)
                )
            assertNoException(builder)
        }

        @Test
        @DisplayName("throw `ValidationException` if a list contains at least one default value")
        fun withDefault() {
            val builder = Blizzard.newBuilder()
                .addSnowflake(
                    Snowflake.newBuilder()
                        .setEdges(3)
                        .setVertices(3)
                )
                .addSnowflake(Snowflake.getDefaultInstance())
            assertValidationException(builder)
        }
    }

    @Nested
    @DisplayName("reflect a `(distinct)` rule and")
    internal inner class DistinctRule {
        @Test
        @DisplayName("throw `ValidationException` if a list contains duplicate entries")
        fun duplicateInList() {
            val flake = Snowflake.newBuilder()
                .setEdges(6)
                .setVertices(6)
                .build()
            val builder = Blizzard.newBuilder()
                .addSnowflake(flake)
                .addSnowflake(flake)
            assertValidationException(builder)
        }

        @Test
        @DisplayName("throw `ValidationException` if a map contains duplicate entries")
        fun duplicateInMap() {
            val player = Player.newBuilder()
                .setShirtName("John Doe")
                .build()
            val builder = Team.newBuilder()
                .putPlayers(7, player)
                .putPlayers(10, player)
            assertValidationException(builder)
        }
    }

    @Nested
    @DisplayName("reflect the `(pattern)` rule")
    internal inner class PatternRule {

        @Test
        @DisplayName(ALLOW_VALID)
        fun pass() {
            val player: Message.Builder = Player.newBuilder()
                .setShirtName("Regina Falangee")
            assertNoException(player)
        }

        @Test
        @DisplayName(PROHIBIT_INVALID)
        fun fail() {
            val player = Player.newBuilder()
                .setShirtName("R")
            val violation = assertValidationException(player)
            assertThat(violation.msgFormat)
                .contains("Invalid T-Shirt name")
        }

        @Test
        @DisplayName("and allow partial matches")
        fun partial() {
            val msg = Book.newBuilder()
                .setAuthor(validAuthor())
                .setContent("Something Something Pride Something Something")
            assertNoException(msg)
        }

        @Test
        @DisplayName("and allow ignoring case")
        fun caseInsensitive() {
            val msg = Book.newBuilder()
                .setAuthor(validAuthor())
                .setContent("preJudice")
            assertNoException(msg)
        }

        @Test
        @DisplayName("and still fail even with loose rules")
        fun failWithLoose() {
            val msg = Book.newBuilder()
                .setAuthor(validAuthor())
                .setContent("something else")
            val violation = assertValidationException(msg)
            assertThat(
                violation.fieldPath
                    .getFieldName(0)
            )
                .isEqualTo("content")
        }

        @Test
        @DisplayName("and handle special characters in the pattern properly")
        fun allowDollarSigns() {
            val msg = Team.newBuilder().setName("Sch 04")
            assertNoException(msg)
        }
    }

    @Nested
    @DisplayName("reflect the (validate) rule")
    internal inner class Validate {
        @Nested
        @DisplayName("on a singular field")
        internal inner class Singular {
            @Test
            @DisplayName(ALLOW_VALID)
            fun pass() {
                val builder = meteoStatsInEurope()
                    .setAverageDrop(validRainDrop())
                assertNoException(builder)
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            fun fail() {
                val builder = meteoStatsInEurope()
                    .setAverageDrop(invalidRainDrop())
                checkInvalid(builder)
            }
        }

        @Nested
        @DisplayName("on a singular `Any` field")
        internal inner class SingularAny {
            @Test
            @DisplayName(ALLOW_VALID)
            fun pass() {
                val builder = meteoStatsInEurope()
                    .setAverageDrop(validRainDrop())
                    .setLastEvent(AnyPacker.pack(validRainDrop()))
                assertNoException(builder)
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            fun fail() {
                val builder = meteoStatsInEurope()
                    .setAverageDrop(validRainDrop())
                    .setLastEvent(AnyPacker.pack(invalidCloud()))
                checkInvalid(builder)
            }

            @Test
            @DisplayName(IGNORE_UNKNOWN_INVALID)
            fun unknownType() {
                val builder = meteoStatsInEurope()
                    .setAverageDrop(validRainDrop())
                    .setLastEvent(AnyPacker.pack(invalidRainDrop()))
                assertNoException(builder)
            }
        }

        @Nested
        @DisplayName("on a repeated field")
        internal inner class Repeated {
            @Test
            @DisplayName(ALLOW_VALID)
            fun pass() {
                val builder = Rain.newBuilder()
                    .addRainDrop(validRainDrop())
                assertNoException(builder)
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            fun fail() {
                val builder = Rain.newBuilder()
                    .addRainDrop(invalidRainDrop())
                checkInvalid(builder, "Bad rain drop")
            }
        }

        @Nested
        @DisplayName("on a repeated `Any` field")
        internal inner class RepeatedAny {
            @Test
            @DisplayName(ALLOW_VALID)
            fun pass() {
                val packedValid = AnyPacker.pack(validRainDrop())
                val builder = meteoStatsInEurope()
                    .setAverageDrop(validRainDrop())
                    .addPredictedEvent(packedValid)
                    .addPredictedEvent(packedValid)
                assertNoException(builder)
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            fun fail() {
                val packedValid = AnyPacker.pack(validCloud())
                val packedInvalid = AnyPacker.pack(invalidCloud())
                val builder = meteoStatsInEurope()
                    .setAverageDrop(validRainDrop())
                    .addPredictedEvent(packedValid)
                    .addPredictedEvent(packedInvalid)
                checkInvalid(builder)
            }

            @Test
            @DisplayName(IGNORE_UNKNOWN_INVALID)
            fun unknownValue() {
                val packedValid = AnyPacker.pack(validCloud())
                val packedInvalid = AnyPacker.pack(invalidRainDrop())
                val builder = meteoStatsInEurope()
                    .setAverageDrop(validRainDrop())
                    .addPredictedEvent(packedValid)
                    .addPredictedEvent(packedInvalid)
                assertNoException(builder)
            }
        }

        private fun meteoStatsInEurope(): MeteoStatistics.Builder {
            return MeteoStatistics.newBuilder()
                .putIncludedRegions("EU", someRegion())
        }

        private fun invalidRainDrop(): RainDrop {
            return RainDrop.newBuilder()
                .setMassInGrams(-1)
                .buildPartial()
        }

        private fun validRainDrop(): RainDrop {
            return RainDrop.newBuilder()
                .setMassInGrams(1)
                .buildPartial()
        }

        private fun validCloud(): Cloud {
            return Cloud.newBuilder()
                .setCubicMeters(2)
                .build()
        }

        private fun invalidCloud(): Cloud {
            return Cloud.newBuilder()
                .setCubicMeters(-2)
                .buildPartial()
        }

        private fun someRegion(): Region {
            return Region.newBuilder()
                .setName("Europe")
                .build()
        }

        private fun checkInvalid(
            builder: Message.Builder,
            errorPart: String = "message must have valid properties"
        ) {
            val violation = assertValidationException(builder)
            assertThat(violation.msgFormat)
                .contains(errorPart)
            assertThat(violation.violationList)
                .hasSize(1)
        }
    }

    @Nested
    internal inner class IsRequired {
        @Test
        @DisplayName(PROHIBIT_INVALID)
        fun fail() {
            val builder = Lunch.newBuilder()
            assertValidationException(builder)
        }

        @Test
        @DisplayName(ALLOW_VALID)
        fun pass() {
            val builder = Lunch.newBuilder()
                .setHotSoup("Minestrone")
            assertNoException(builder)
        }
    }

    @Nested
    @DisplayName("reflect the `(when)` rule")
    internal inner class WhenRule {

        @Test
        @DisplayName(PROHIBIT_INVALID)
        fun fail() {
            val startWhen = Timestamps.fromSeconds(4792687200L) // 15 Nov 2121
            val player = Player.newBuilder()
                .setStartedCareerIn(startWhen)
            assertValidationException(player)
        }

        @Test
        @DisplayName(ALLOW_VALID)
        fun pass() {
            val timestamp = Timestamps.fromSeconds(59086800L) // 15 Nov 1971
            val player = Player.newBuilder()
                .setStartedCareerIn(timestamp)
            assertNoException(player)
        }
    }

    /**
     * Tests the rules which are currently generated by `mc-java`.
     *
     *
     * Note: these tests are now disabled since simultaneous application of ProtoData
     * and `mc-java` makes impossible to pass the configuration options
     * to the `LaunchProtoData` Gradle task. Effectively, inability to make such
     * a configuration makes these tests fail.
     *
     *
     * The tests are disabled until the code generation required by these tests
     * is fully performed by the Validation library itself without any dependencies
     * onto the code generated by `mc-java`. Once this is achieved, `mc-java` plugin
     * should be turned off for the corresponding test project.
     */
    @Nested
    @Disabled
    @DisplayName("make the entity and signal IDs required")
    internal inner class RequiredIdRule {
        @Test
        @DisplayName("allow valid entities")
        fun passEntity() {
            val entity = Fancy.newBuilder()
                .setId(FancyId.newBuilder().setUuid(Identifier.newUuid()))
            assertNoException(entity)
        }

        @Test
        @DisplayName("not allow invalid entities")
        fun failEntity() {
            val entity = Fancy.newBuilder()
            val violation = assertValidationException(entity)
            violation.fieldPath.getFieldName(0) shouldBe "id"
        }

        @Test
        @DisplayName("allow valid events")
        fun passEvent() {
            val event = PrefixEventRecognized.newBuilder()
                .setId("qwerty")
            assertNoException(event)
        }

        @Test
        @DisplayName("not allow invalid events")
        fun failEvent() {
            val event = PrefixEventRecognized.newBuilder()
            val violation = assertValidationException(event)
            violation.fieldPath.getFieldName(0) shouldBe "id"
        }

        @Test
        @DisplayName("allow valid commands")
        fun passCommand() {
            val cmd = RecognizeSuffixCommand.newBuilder().setId("42")
            assertNoException(cmd)
        }

        @Test
        @DisplayName("not allow invalid commands")
        fun failCommand() {
            val cmd = RecognizeSuffixCommand.newBuilder()
            val violation = assertValidationException(cmd)
            violation.fieldPath.getFieldName(0) shouldBe "id"
        }
    }
}
