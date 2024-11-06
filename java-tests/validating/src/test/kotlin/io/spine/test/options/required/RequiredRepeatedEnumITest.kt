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

package io.spine.test.options.required

import io.spine.test.tools.validate.Collections
import io.spine.test.tools.validate.UltimateChoice
import io.spine.validation.assertions.assertViolation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a repeated enum field should")
internal class RequiredRepeatedEnumITest {

    private val field = "at_least_one_piece_of_meat"

    @Test
    fun `require at least one item`() {
        val instance = Collections.newBuilder()
        assertViolation(instance, field, "must not be empty")
    }

    @Test // https://github.com/SpineEventEngine/mc-java/issues/119
    @Disabled("Until we finalize the behavior of the `required` constraint on repeated enums")
    fun `cannot have all items with zero-index enum item value`() {
        val allZero = Collections.newBuilder()
            .putNotEmptyMapOfInts(42, 314)
            .addAtLeastOnePieceOfMeat(UltimateChoice.VEGETABLE)
            .addAtLeastOnePieceOfMeat(UltimateChoice.VEGETABLE)
            .putContainsANonEmptyStringValue("  ", "   ")
            .addNotEmptyListOfLongs(42L)
        assertViolation(allZero, field, "cannot contain default values")
    }

    @Test // https://github.com/SpineEventEngine/mc-java/issues/119
    @Disabled("Until we finalize the behavior of the `required` constraint on repeated enums")
    fun `must not have event one value with non-zero enum item value`() {
        val instance = Collections.newBuilder()
            .putContainsANonEmptyStringValue("111", "222")
            .addNotEmptyListOfLongs(0L)
            .putNotEmptyMapOfInts(0, 0)
            .addAtLeastOnePieceOfMeat(UltimateChoice.FISH)
            .addAtLeastOnePieceOfMeat(UltimateChoice.CHICKEN)
            .addAtLeastOnePieceOfMeat(UltimateChoice.VEGETABLE)
        assertViolation(instance, field, "default values")
    }
}
