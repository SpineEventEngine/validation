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

import io.kotest.matchers.string.shouldContain
import io.spine.string.format
import io.spine.tools.validation.test.time.LocalTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(min)` rule should")
internal class MinRuleIgTest {

    @Test
    fun `reject a value less than the threshold`() {
        val violation = assertValidationException(
            LocalTime.newBuilder().setHours(-1)
        )
        val message = violation.message.format()
        message shouldContain "cannot be negative"
    }

    @Test
    fun `accept a value equal to the threshold`() {
        assertNoException(LocalTime.newBuilder().setMinutes(0))
    }

    @Test
    fun `accept a value greater than the threshold`() {
        assertNoException(LocalTime.newBuilder().setMinutes(1))
    }
}
