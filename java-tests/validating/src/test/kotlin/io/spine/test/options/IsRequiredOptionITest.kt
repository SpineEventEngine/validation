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
import io.spine.validate.Validate.violationsOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(is_required)` option should be compiled so that")
internal class IsRequiredOptionITest {

    @Test
    fun `throw if required field group is not set`() {
        val message = Meal.newBuilder()
            .setCheese(Sauce.getDefaultInstance())
            .buildPartial()

        val violations = violationsOf(message)

        violations.size shouldBe 1
        violations[0] shouldNotBe null
        violations[0]!!.msgFormat shouldContain "choice"
    }

    @Test
    fun `not throw if required field group is set`() {
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
    fun `ignore non-required field groups`() {
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
