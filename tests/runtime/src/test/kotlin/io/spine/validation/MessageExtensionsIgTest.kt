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

package io.spine.validation

import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.spine.test.validate.Meal
import io.spine.test.validate.Meat
import io.spine.test.validate.meal
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * This is a small set of integration tests for extension functions
 * working on compiled Protobuf messages.
 *
 * There is a "sister" test suite under `jvm-runtime` module,
 * which tests the same extensions but on stubs.
 */
@DisplayName("Validation extensions for `Message` should")
internal class MessageExtensionsIgTest {

    @Nested
    inner class `Check if the message is valid` {

        @Test
        fun `returning 'this' if so`() {
            val meal = meal { meat = Meat.getDefaultInstance() }
            meal.checkValid() shouldBeSameInstanceAs meal
        }

        @Test
        fun `throwing 'ValidationException' if not`() {
            assertThrows<ValidationException> {
                Meal.getDefaultInstance().checkValid()
            }
        }
    }
}
