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

import io.spine.test.tools.validate.MapWithEnumValues
import io.spine.test.tools.validate.UltimateChoice
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a map field with enum values should")
internal class RequiredMapEnumIgTest {

    private val field = "meat"

    @Test
    fun `require at least one entry`() {
        val instance = MapWithEnumValues.newBuilder()
        assertViolation(instance, field)
    }

    /**
     * An entry whose value is the zero-index enum item is treated as missing —
     * analogously to how the zero-index enum item is rejected for a singular
     * `(required)` enum field.
     */
    @Test
    fun `prohibit all entries with zero-index enum item value`() {
        val allZero = MapWithEnumValues.newBuilder()
            .putMeat("a", UltimateChoice.VEGETABLE)
            .putMeat("b", UltimateChoice.VEGETABLE)
        assertViolation(allZero, field)
    }

    @Test
    fun `prohibit even one zero-index enum item value`() {
        val instance = MapWithEnumValues.newBuilder()
            .putMeat("a", UltimateChoice.FISH)
            .putMeat("b", UltimateChoice.CHICKEN)
            .putMeat("c", UltimateChoice.VEGETABLE)
        assertViolation(instance, field)
    }

    @Test
    fun `allow entries with non-zero enum values`() {
        val instance = MapWithEnumValues.newBuilder()
            .putMeat("a", UltimateChoice.FISH)
            .putMeat("b", UltimateChoice.CHICKEN)
        assertValid(instance)
    }

    @Test
    fun `allow an entry with the empty key as long as the value is non-zero`() {
        val instance = MapWithEnumValues.newBuilder()
            .putMeat("", UltimateChoice.FISH)
        assertValid(instance)
    }
}
