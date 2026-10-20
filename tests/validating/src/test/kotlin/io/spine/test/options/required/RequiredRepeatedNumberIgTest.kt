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
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a repeated number field should")
internal class RequiredRepeatedNumberIgTest {

    @Test
    fun `require at lest one element`() {
        val instance = Collections.newBuilder()
        assertViolation(instance, "not_empty_list_of_longs")
    }

    @Test
    fun `allow any items, including zero`() {
        val instance = Collections.newBuilder()
            .addNotEmptyListOfLongs(0L)
            .putContainsANonEmptyStringValue("222", "111")
            .addAtLeastOnePieceOfMeat(UltimateChoice.CHICKEN)
            .putNotEmptyMapOfInts(42, 42)
        assertValid(instance)
    }
}
