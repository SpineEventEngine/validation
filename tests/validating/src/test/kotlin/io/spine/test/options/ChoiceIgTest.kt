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

package io.spine.test.options

import com.google.protobuf.Message
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.base.Identifier
import io.spine.test.tools.validate.Fish
import io.spine.test.tools.validate.Meal
import io.spine.test.tools.validate.Sauce
import io.spine.test.tools.validate.fish
import io.spine.testing.TestValues.randomString
import io.spine.validation.Validate.violationsOf
import io.spine.string.format
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(choice)` option should be compiled so that")
internal class ChoiceIgTest {

    @Test
    fun `it causes violation if required field group is not set`() {
        val message = Meal.newBuilder()
            .setCheese(Sauce.getDefaultInstance())
            .buildPartial()

        val violations = violationsOf(message)

        violations.size shouldBe 1
        violations[0] shouldNotBe null
        violations[0].message.format() shouldContain "choice"
    }

    @Test
    fun `there is no violation if required field group is set`() {
        val fish = fish {
            description = randomString()
        }
        val message = Meal.newBuilder()
            .setCheese(Sauce.getDefaultInstance())
            .setFish(fish)
            .buildPartial()
        assertValid(message)
    }

    @Test
    fun `non-required field groups are ignored`() {
        val fish = Fish.newBuilder()
            .setDescription(Identifier.newUuid())
            .build()
        val message = Meal.newBuilder()
            .setFish(fish)
            .buildPartial()
        assertValid(message)
    }
}

private fun assertValid(message: Message) {
    violationsOf(message).shouldBeEmpty()
}
