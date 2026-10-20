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

/**
 * This test suite verifies that a custom message validator [TheOnlyTimeValid]
 * works when validating messages containing [Timestamp] fields.
 */
@DisplayName("A validator for an external message should")
internal class ExternalMessageValidatorIgTest {

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
