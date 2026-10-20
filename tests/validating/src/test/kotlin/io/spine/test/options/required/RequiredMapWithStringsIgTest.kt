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
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a map field with string values should")
internal class RequiredMapWithStringsIgTest {

    @Test
    fun `require at least one entry`() {
        val instance = Collections.newBuilder()
        assertViolation(instance, "contains_a_non_empty_string_value")
    }

    /**
     * An entry with an empty-string value is treated as missing — analogously
     * to how an empty string in a `(required)` `string` field is rejected.
     */
    @Test
    fun `prohibit an entry with the empty value`() {
        val empty = Collections.newBuilder()
            .putContainsANonEmptyStringValue("", "")
            .putNotEmptyMapOfInts(111, 314)
            .addAtLeastOnePieceOfMeat(UltimateChoice.FISH)
            .addNotEmptyListOfLongs(42L)
        assertViolation(empty, "contains_a_non_empty_string_value")
    }

    @Test
    fun `prohibit an entry with the empty value even if its key is non-empty`() {
        val instance = Collections.newBuilder()
            .addNotEmptyListOfLongs(42L)
            .putContainsANonEmptyStringValue(" ", "")
            .putNotEmptyMapOfInts(0, 0)
            .addAtLeastOnePieceOfMeat(UltimateChoice.CHICKEN)
        assertViolation(instance, "contains_a_non_empty_string_value")
    }

    @Test
    fun `prohibit a map that mixes a valid entry with an empty-value entry`() {
        val instance = Collections.newBuilder()
            .putContainsANonEmptyStringValue("foo", "bar")
            .putContainsANonEmptyStringValue("x", "")
            .putNotEmptyMapOfInts(111, 314)
            .addAtLeastOnePieceOfMeat(UltimateChoice.FISH)
            .addNotEmptyListOfLongs(42L)
        assertInvalid(instance)
    }

    @Test
    fun `allow entries with non-empty string`() {
        val nonEmpty = Collections.newBuilder()
            .putContainsANonEmptyStringValue("bar", "foo")
            .putContainsANonEmptyStringValue("foo", "bar")
            .putNotEmptyMapOfInts(111, 314)
            .addAtLeastOnePieceOfMeat(UltimateChoice.FISH)
            .addNotEmptyListOfLongs(42L)
        assertValid(nonEmpty)
    }

    @Test
    fun `allow an entry with the empty key as long as the value is non-empty`() {
        val instance = Collections.newBuilder()
            .addNotEmptyListOfLongs(42L)
            .putContainsANonEmptyStringValue("", "v")
            .putNotEmptyMapOfInts(0, 0)
            .addAtLeastOnePieceOfMeat(UltimateChoice.CHICKEN)
        assertValid(instance)
    }
}
