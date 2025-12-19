/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.tools.validation.test

import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.tools.compiler.protobuf.descriptor
import io.spine.validation.ConstraintViolation
import io.spine.validation.ValidationException
import io.spine.tools.validation.test.TheOnlyTimeValid.Companion.ValidTimestamp
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`TimestampValidator` should")
class TimestampValidatorSpec {

    @Nested inner class
    `prohibit invalid instances` {

        @Test
        fun `of a singular field`() {
            val timestamp = Timestamps.now()
            val exception = assertThrows<ValidationException> {
                singularWellKnownMessage {
                    value = timestamp
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe 1

            val violation = violations.first()
            violation.assert<SingularWellKnownMessage>(timestamp)
        }

        @Test
        fun `of a repeated field`() {
            val timestamps = List(3) { Timestamps.now() }
            val exception = assertThrows<ValidationException> {
                repeatedWellKnownMessage {
                    value.addAll(timestamps)
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe timestamps.size

            violations.forEachIndexed { index, violation ->
                val timestamp = timestamps[index]
                violation.assert<RepeatedWellKnownMessage>(timestamp)
            }
        }

        @Test
        fun `of a map field`() {
            val timestamps = List(3) { "Timestamp #$it" }
                .associateWith { Timestamps.now() }
            val exception = assertThrows<ValidationException> {
                mappedWellKnownMessage {
                    value.putAll(timestamps)
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe timestamps.size

            violations.forEachIndexed { index, violation ->
                val timestamp = timestamps["Timestamp #$index"]!!
                violation.assert<MappedWellKnownMessage>(timestamp)
            }
        }
    }

    @Nested inner class
    `allow valid instances` {

        @Test
        fun `of a singular field`() {
            assertDoesNotThrow {
                singularWellKnownMessage {
                    value = ValidTimestamp
                }
            }
        }

        @Test
        fun `of a repeated field`() {
            val timestamps = List(3) { ValidTimestamp }
            assertDoesNotThrow {
                repeatedWellKnownMessage {
                    value.addAll(timestamps)
                }
            }
        }

        @Test
        fun `of a map field`() {
            val timestamps = List(3) { "Timestamp #$it" }
                .associateWith { ValidTimestamp }
            assertDoesNotThrow {
                mappedWellKnownMessage {
                    value.putAll(timestamps)
                }
            }
        }
    }
}

/**
 * Asserts that this [ConstraintViolation] has all required fields populated
 * in accordance to [TheOnlyTimeValid].
 */
private inline fun <reified T : Message> ConstraintViolation.assert(timestamp: Timestamp) {
    message shouldBe TheOnlyTimeValid.Violation.message
    fieldPath shouldBe expectedFieldPath
    typeName shouldBe T::class.descriptor.fullName
    fieldValue shouldBe toAny(timestamp.seconds)
}

private val expectedFieldPath = FieldPath("value").toBuilder()
    .mergeFrom(TheOnlyTimeValid.Violation.fieldPath)
    .build()
