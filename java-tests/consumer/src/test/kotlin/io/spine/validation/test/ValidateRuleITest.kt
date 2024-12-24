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

package io.spine.validation.test

import com.google.protobuf.Any
import com.google.protobuf.Message
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.spine.protobuf.AnyPacker
import io.spine.protobuf.pack
import io.spine.testing.logging.mute.MuteLogging
import io.spine.validate.formatUnsafe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`(validate)` rule should")
internal class ValidateRuleITest {

    @Nested internal inner class
    `on a singular field` {

        @Test
        fun `validate a field value`() = assertNoException(
            meteoStatsInEurope()
                .setAverageDrop(validRainDrop())
        )

        @Test
        fun `prohibit non-valid field values`() {
            val builder = meteoStatsInEurope()
                .setAverageDrop(invalidRainDrop())
            checkInvalid(builder)
        }
    }

    @Nested
    internal inner class
    `on a singular 'Any' field` {

        @Test
        fun `accept a valid enclosed message`() {
            val builder = meteoStatsInEurope()
                .setAverageDrop(validRainDrop())
                .setLastEvent(AnyPacker.pack(validRainDrop()))
            assertNoException(builder)
        }

        @Test
        fun `prohibit invalid enclosed message`() {
            val builder = meteoStatsInEurope()
                .setAverageDrop(validRainDrop())
                .setLastEvent(invalidCloud().pack())
            checkInvalid(builder)
        }

        @Test
        @MuteLogging
        fun `ignore unknown packed type`() {
            val builder = meteoStatsInEurope()
                .setAverageDrop(validRainDrop())
                .setLastEvent(packedUnknown())
            assertNoException(builder)
        }
    }

    @Nested
    internal inner class
    `on a 'repeated' field` {

        @Test
        fun `check each item`() {
            val builder = Rain.newBuilder()
                .addRainDrop(validRainDrop())
            assertNoException(builder)
        }

        @Test
        fun `reject an invalid item`() {
            val builder = Rain.newBuilder()
                .addRainDrop(invalidRainDrop())
            checkInvalid(builder, "Bad rain drop")
        }
    }

    @Nested
    internal inner class
    `on a repeated 'Any' field` {

        @Test
        fun `check each enclosed item`() {
            val packedValid = validRainDrop().pack()
            val builder = meteoStatsInEurope()
                .setAverageDrop(validRainDrop())
                .addPredictedEvent(packedValid)
                .addPredictedEvent(packedValid)
            assertNoException(builder)
        }

        @Test
        fun `reject an invalid enclosed item`() {
            val packedValid = validCloud().pack()
            val packedInvalid = invalidCloud().pack()
            val builder = meteoStatsInEurope()
                .setAverageDrop(validRainDrop())
                .addPredictedEvent(packedValid)
                .addPredictedEvent(packedInvalid)
            checkInvalid(builder)
        }

        @Test
        @MuteLogging
        fun `ignore unknown packed type`() {
            val packedValid = validCloud().pack()
            val packedInvalid = packedUnknown()
            val builder = meteoStatsInEurope()
                .setAverageDrop(validRainDrop())
                .addPredictedEvent(packedValid)
                .addPredictedEvent(packedInvalid)
            assertNoException(builder)
        }
    }
}

private fun checkInvalid(
    builder: Message.Builder,
    errorPart: String = "is invalid"
) {
    assertValidationException(builder).run {
        message.formatUnsafe() shouldContain errorPart
        violationList shouldHaveSize 1
    }
}

private fun meteoStatsInEurope(): MeteoStatistics.Builder =
    MeteoStatistics.newBuilder()
        .putIncludedRegions("EU", someRegion())

private fun packedUnknown(): Any =
    Any.newBuilder()
        .setTypeUrl("unknown.type/foo.bar")
        .build()

private fun invalidRainDrop(): RainDrop {
    return RainDrop.newBuilder()
        .setMassInGrams(-1)
        .buildPartial()
}

private fun validRainDrop(): RainDrop =
    RainDrop.newBuilder()
        .setMassInGrams(1)
        .buildPartial()

private fun validCloud(): Cloud = cloud {
    cubicMeters = 2
}

private fun invalidCloud(): Cloud =
    Cloud.newBuilder()
        .setCubicMeters(-2)
        .buildPartial()

private fun someRegion(): Region = region {
    name = "Europe"
}

