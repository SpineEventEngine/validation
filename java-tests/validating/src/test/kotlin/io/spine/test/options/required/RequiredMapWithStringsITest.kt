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
import io.spine.validation.assertions.assertInvalid
import io.spine.validation.assertions.assertValid
import io.spine.validation.assertions.assertViolation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a map field with string values should")
internal class RequiredMapWithStringsITest {

    @Test
    fun `require at least one entry`() {
        val instance = Collections.newBuilder()
        assertViolation(instance,
            "contains_a_non_empty_string_value",
            "must have a value"
        )
    }

    @Test
    @Disabled("temporarily until the issue with empty entries is finalised")
    fun `prohibit an entry with empty string`() {
        val empty = Collections.newBuilder()
            .putContainsANonEmptyStringValue("", "")
            .putNotEmptyMapOfInts(111, 314)
            .addAtLeastOnePieceOfMeat(UltimateChoice.FISH)
            .addNotEmptyListOfLongs(42L)
        assertInvalid(empty)
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
    fun `allow an entry with the empty key and non-empty value`() {
        val instance = Collections.newBuilder()
            .addNotEmptyListOfLongs(42L)
            .putContainsANonEmptyStringValue("", " ")
            .putNotEmptyMapOfInts(0, 0)
            .addAtLeastOnePieceOfMeat(UltimateChoice.CHICKEN)
        assertValid(instance)
    }
}
