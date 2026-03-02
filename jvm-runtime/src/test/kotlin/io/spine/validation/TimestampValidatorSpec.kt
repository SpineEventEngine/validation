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

package io.spine.validation

import com.google.protobuf.timestamp
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`TimestampValidator` should")
internal class TimestampValidatorSpec {

    private val validator = TimestampValidator()

    @Test
    fun `validate a valid timestamp`() {
        val timestamp = timestamp {
            seconds = 123456789
            nanos = 123
        }
        validator.validate(timestamp).shouldBeEmpty()
    }

    @Test
    fun `detect an invalid timestamp`() {
        val secondsValue = -62135596801L // One second before the minimum.
        val timestamp = timestamp {
            seconds = secondsValue
        }
        val violations = validator.validate(timestamp)
        violations shouldHaveSize 1
        violations[0].message.withPlaceholders shouldBe 
                "The timestamp is invalid: seconds: $secondsValue, nanos: 0."
    }

    @Test
    fun `detect an invalid timestamp with invalid nanos`() {
        val nanosValue = 1000000000
        val timestamp = timestamp {
            nanos = nanosValue
        }
        val violations = validator.validate(timestamp)
        violations shouldHaveSize 1
        violations[0].message.withPlaceholders shouldBe 
                "The timestamp is invalid: seconds: 0, nanos: $nanosValue."
    }
}
