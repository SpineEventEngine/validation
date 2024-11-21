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
import io.spine.validation.assertions.assertValid
import io.spine.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a repeated number field should")
internal class RequiredRepeatedNumberITest {

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