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

package io.spine.test.options.required

import io.spine.test.tools.validate.Collections
import io.spine.test.tools.validate.UltimateChoice
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a repeated enum field should")
internal class RequiredRepeatedEnumIgTest {

    private val field = "at_least_one_piece_of_meat"

    @Test
    fun `require at least one item`() {
        val instance = Collections.newBuilder()
        assertViolation(instance, field)
    }

    @Test
    fun `prohibit all items with zero-index enum item value`() {
        val allZero = Collections.newBuilder()
            .putNotEmptyMapOfInts(42, 314)
            .addAtLeastOnePieceOfMeat(UltimateChoice.VEGETABLE)
            .addAtLeastOnePieceOfMeat(UltimateChoice.VEGETABLE)
            .putContainsANonEmptyStringValue("  ", "   ")
            .addNotEmptyListOfLongs(42L)
        assertViolation(allZero, field)
    }

    @Test
    fun `prohibit even one zero-index enum item value`() {
        val instance = Collections.newBuilder()
            .putContainsANonEmptyStringValue("111", "222")
            .addNotEmptyListOfLongs(0L)
            .putNotEmptyMapOfInts(0, 0)
            .addAtLeastOnePieceOfMeat(UltimateChoice.FISH)
            .addAtLeastOnePieceOfMeat(UltimateChoice.CHICKEN)
            .addAtLeastOnePieceOfMeat(UltimateChoice.VEGETABLE)
        assertViolation(instance, field)
    }
}
