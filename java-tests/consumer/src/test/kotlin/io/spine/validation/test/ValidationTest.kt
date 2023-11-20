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
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
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
            val result = builder.build()
            result shouldNotBe null
        } catch (e: ValidationException) {
            fail<Any>("Unexpected constraint violation: " + e.constraintViolations, e)
        }
    }

    private fun validAuthor(): Author = author {
        name = "Vernon"
    }

    @Nested internal inner class
    `reflect a rule with a less sign and` {

        @Test
        fun `throw 'ValidationException' if actual value is greater than the threshold`() {
            assertValidationException(Mru.newBuilder().setKhoums(6))
        }

        @Test
        fun `throw 'ValidationException' if actual value is equal to the threshold`() {
            assertValidationException(Mru.newBuilder().setKhoums(5))
        }

        @Test
        fun `throw no exceptions if actual value is less than the threshold`() {
            assertNoException(Mru.newBuilder().setKhoums(4))
        }
    }

    @Nested internal inner class
    `reflect a rule with a greater or equal sign and` {
        
        @Test
        fun `throw 'ValidationException' if actual value is less than the threshold`() {
            val violation = assertValidationException(
                LocalTime.newBuilder().setHours(-1)
            )
            violation.msgFormat shouldContain "cannot be negative"
        }

        @Test
        fun `throw no exceptions if actual value is equal to the threshold`() {
            assertNoException(LocalTime.newBuilder().setMinutes(0))
        }

        @Test
        fun `throw no exceptions if actual value is greater than the threshold`() {
            assertNoException(LocalTime.newBuilder().setMinutes(1))
        }
    }

    @Nested internal inner class
    `reflect a '(required)' rule and` {

        @Test
        fun `check string field`() {
            val builder = Author.newBuilder()
            val violation = assertValidationException(builder)
            violation.msgFormat shouldContain "Author must have a name"
        }

        @Test
        fun `check message field`() {
            val builder = Book.newBuilder()
            assertValidationException(builder).also {
                it.msgFormat shouldContain "value must be set"
                it.fieldPath.getFieldName(0) shouldBe "author"
            }
        }

        @Test
        fun `pass if a value is set`() = assertNoException(
            Author.newBuilder().setName("Evans")
        )

        @Test
        fun `pass if not required`() = assertNoException(
            Book.newBuilder().setAuthor(validAuthor())
        )

        @Test
        @Disabled("Until we propagate the `validate` option property to collection elements.")
        fun `throw 'ValidationException' if a list contains only default values`() {
            val builder = Blizzard.newBuilder()
                .addSnowflake(Snowflake.getDefaultInstance())
            assertValidationException(builder)
        }

        @Test
        fun `pass if a list contains all non-default values`() = assertNoException(
            Blizzard.newBuilder()
                .addSnowflake(
                    Snowflake.newBuilder()
                        .setEdges(3)
                        .setVertices(3)
                )
        )

        @Test
        @Disabled("Until we propagate the `validate` option property to collection elements.")
        fun `throw 'ValidationException' if a list contains at least one default value`() {
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

    @Nested internal inner class
    `reflect a '(distinct)' rule and` {

        @Test
        fun `throw 'ValidationException' if a list contains duplicate entries`() {
            val flake = snowflake {
                edges = 6
                vertices = 6
            }
            val builder = Blizzard.newBuilder()
                .addSnowflake(flake)
                .addSnowflake(flake)

            assertValidationException(builder)
        }

        @Test
        fun `throw 'ValidationException' if a map contains duplicate entries`() {
            val player = player {
                shirtName = "John Doe"
            }
            val builder = Team.newBuilder()
                .putPlayers(7, player)
                .putPlayers(10, player)
            assertValidationException(builder)
        }
    }

    @Nested internal inner class
    `reflect the '(pattern)' rule` {

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
        fun `and allow partial matches`() {
            val msg = Book.newBuilder()
                .setAuthor(validAuthor())
                .setContent("Something Something Pride Something Something")
            assertNoException(msg)
        }

        @Test
        fun `and allow ignoring case`() {
            val msg = Book.newBuilder()
                .setAuthor(validAuthor())
                .setContent("preJudice")
            assertNoException(msg)
        }

        @Test
        fun `and still fail even with loose rules`() {
            val msg = Book.newBuilder()
                .setAuthor(validAuthor())
                .setContent("something else")
            val violation = assertValidationException(msg)
            violation.fieldPath.getFieldName(0) shouldBe "content"
        }

        @Test
        fun `and handle special characters in the pattern properly`() =
            assertNoException(Team.newBuilder().setName("Sch 04"))
    }

    @Nested internal inner class
    `reflect the '(validate)' rule` {

        @Nested internal inner class
        `on a singular field` {

            @Test
            @DisplayName(ALLOW_VALID)
            fun pass() = assertNoException(
                meteoStatsInEurope()
                    .setAverageDrop(validRainDrop())
            )

            @Test
            @DisplayName(PROHIBIT_INVALID)
            fun fail() {
                val builder = meteoStatsInEurope()
                    .setAverageDrop(invalidRainDrop())
                checkInvalid(builder)
            }
        }

        @Nested internal inner class
        `on a singular 'Any' field` {

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

        @Nested internal inner class
        `on a 'repeated' field` {

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

        @Nested internal inner class
        `on a repeated 'Any' field` {

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


        private fun checkInvalid(
            builder: Message.Builder,
            errorPart: String = "message must have valid properties"
        ) {
            assertValidationException(builder).run {
                msgFormat shouldContain errorPart
                violationList shouldHaveSize 1
            }
        }
    }

    @Nested internal inner class
    `support '(is_required)' constraint for 'oneof' fields` {

        @Test
        fun `prohibit invalid`() {
            assertValidationException(Lunch.newBuilder())
        }

        @Test
        fun `allow valid`() = assertNoException(
            Lunch.newBuilder()
                .setHotSoup("Minestrone")
        )
    }

    @Nested internal inner class
    `reflect the '(when)' rule` {

        @Test
        fun `prohibit invalid`() {
            val startWhen = Timestamps.fromSeconds(4792687200L) // 15 Nov 2121
            val player = Player.newBuilder()
                .setStartedCareerIn(startWhen)
            assertValidationException(player)
        }

        @Test
        fun `allow valid`() {
            val timestamp = Timestamps.fromSeconds(59086800L) // 15 Nov 1971
            val player = Player.newBuilder()
                .setStartedCareerIn(timestamp)
            assertNoException(player)
        }
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

private fun validCloud(): Cloud = cloud {
    cubicMeters = 2
}

private fun invalidCloud(): Cloud {
    return Cloud.newBuilder()
        .setCubicMeters(-2)
        .buildPartial()
}

private fun someRegion(): Region = region {
    name = "Europe"
}
