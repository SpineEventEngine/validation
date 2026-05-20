/*
 * Copyright 2026, TeamDev. All rights reserved.
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

import io.spine.test.tools.validate.MapWithEnumValues
import io.spine.test.tools.validate.UltimateChoice
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a map field with enum values should")
internal class RequiredMapEnumITest {

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
