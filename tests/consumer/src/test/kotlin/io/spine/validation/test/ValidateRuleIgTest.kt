/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.validation.test

import com.google.protobuf.Message
import io.kotest.matchers.string.shouldContain
import io.spine.protobuf.AnyPacker
import io.spine.protobuf.pack
import io.spine.string.formatUnsafe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`(validate)` rule should")
internal class ValidateRuleIgTest {

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
            checkInvalid(builder, BAD_RAIN_DROP)
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
            checkInvalid(builder, BAD_CLOUD)
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
            checkInvalid(builder, BAD_RAIN_DROP)
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
            checkInvalid(builder, BAD_CLOUD)
        }
    }
}

private const val BAD_CLOUD = "Bad cloud!"
private const val BAD_RAIN_DROP = "Bad rain drop!"

private fun checkInvalid(builder: Message.Builder, errorPart: String) =
    assertValidationException(builder).run {
        message.formatUnsafe() shouldContain errorPart
    }

private fun meteoStatsInEurope(): MeteoStatistics.Builder =
    MeteoStatistics.newBuilder()
        .putIncludedRegions("EU", someRegion())

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

