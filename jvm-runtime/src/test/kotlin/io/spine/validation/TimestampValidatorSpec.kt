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
    fun `detect an invalid timestamp with seconds out of range`() {
        val secondsValue = -62135596801L // One second before the minimum.
        val timestamp = timestamp {
            seconds = secondsValue
        }
        val violations = validator.validate(timestamp)
        violations shouldHaveSize 1
        val violation = violations[0] as FieldViolation
        violation.message.withPlaceholders shouldBe
                "The field.path value is out of range (range.value): $secondsValue."
        violation.message.placeholderValueMap shouldBe mapOf(
            "field.path" to "seconds",
            "range.value" to "-62135596800..253402300799"
        )
        violation.fieldPath!!.fieldNameList shouldBe listOf("seconds")
        violation.fieldValue shouldBe secondsValue
    }

    @Test
    fun `detect an invalid timestamp with nanos out of range`() {
        val nanosValue = 1_000_000_000
        val timestamp = timestamp {
            nanos = nanosValue
        }
        val violations = validator.validate(timestamp)
        violations shouldHaveSize 1
        val violation = violations[0] as FieldViolation
        violation.message.withPlaceholders shouldBe
                "The field.path value is out of range:  (range.value)$nanosValue."
        violation.message.placeholderValueMap shouldBe mapOf(
            "field.path" to "nanos",
            "range.value" to "0..999999999"
        )
        violation.fieldPath!!.fieldNameList shouldBe listOf("nanos")
        violation.fieldValue shouldBe nanosValue
    }

    @Test
    fun `detect both seconds and nanos out of range`() {
        val secondsValue = -62135596801L
        val nanosValue = 1_000_000_000
        val timestamp = timestamp {
            seconds = secondsValue
            nanos = nanosValue
        }
        val violations = validator.validate(timestamp)
        violations shouldHaveSize 2
        
        val secondsViolation = violations.find { 
            it.fieldPath?.fieldNameList == listOf("seconds")
        } as FieldViolation
        secondsViolation.message.withPlaceholders shouldBe
                "The field.path value is out of range (range.value): $secondsValue."
        secondsViolation.message.placeholderValueMap shouldBe mapOf(
            "field.path" to "seconds",
            "range.value" to "-62135596800..253402300799"
        )
        secondsViolation.fieldValue shouldBe secondsValue

        val nanosViolation = violations.find {
            it.fieldPath?.fieldNameList == listOf("nanos")
        } as FieldViolation
        nanosViolation.message.withPlaceholders shouldBe
                "The field.path value is out of range:  (range.value)$nanosValue."
        nanosViolation.message.placeholderValueMap shouldBe mapOf(
            "field.path" to "nanos",
            "range.value" to "0..999999999"
        )
        nanosViolation.fieldValue shouldBe nanosValue
    }
}
