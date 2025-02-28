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

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.Durations2.hoursAndMinutes
import io.spine.protobuf.Durations2.minutes
import io.spine.test.tools.validate.UniqueMessageCollections
import io.spine.test.tools.validate.UniquePrimitiveCollections
import io.spine.test.tools.validate.uniqueMessageCollections
import io.spine.test.tools.validate.uniquePrimitiveCollections
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`(distinct)` option should be compiled, so that")
internal class DistinctITest {

    @Nested inner class
    `when handling 'list' field` {

        @Test
        fun `duplicated primitive entries result in a violation`() {
            val duplicate1 = 123
            val duplicate2 = 321

            val exception = assertThrows<ValidationException> {
                uniquePrimitiveCollections {
                    element.add(duplicate1)
                    element.add(1)
                    element.add(duplicate1)
                    element.add(2)
                    element.add(duplicate2)
                    element.add(3)
                    element.add(duplicate2)
                }
            }

            val violation = exception.constraintViolations.first()
            val duplicates = listOf(duplicate1, duplicate2)
            with(violation) {
                fieldPath shouldBe FieldPath("element")
                message.placeholderValueMap shouldContain ("field.duplicates" to "$duplicates")
            }
        }

        @Test
        fun `duplicated message entries result in a violation`() {
            val duplicate1 = hoursAndMinutes(2, 20)
            val duplicate2 = hoursAndMinutes(18, 40)

            val exception = assertThrows<ValidationException> {
                uniqueMessageCollections {
                    element.add(duplicate1)
                    element.add(minutes(15))
                    element.add(duplicate1)
                    element.add(minutes(30))
                    element.add(duplicate2)
                    element.add(minutes(45))
                    element.add(duplicate2)
                }
            }

            val violation = exception.constraintViolations.first()
            val duplicates = listOf(duplicate1, duplicate2)
            with(violation) {
                fieldPath shouldBe FieldPath("element")
                message.placeholderValueMap shouldContain ("field.duplicates" to "$duplicates")
            }
        }

        @Test
        fun `unique primitive elements do not result in a violation`() {
            assertDoesNotThrow {
                uniquePrimitiveCollections {
                    element.add(1)
                    element.add(2)
                    element.add(3)
                }
            }
        }

        @Test
        fun `unique message elements do not result in a violation`() {
            assertDoesNotThrow {
                uniqueMessageCollections {
                    element.add(minutes(1))
                    element.add(minutes(2))
                    element.add(minutes(3))
                }
            }
        }
    }

    @Nested inner class
    `when handling 'map' field` {

        @Test
        fun `duplicated primitive values result in a violation`() {
            val duplicate1 = 123
            val duplicate2 = 321

            val exception = assertThrows<ValidationException> {
                uniquePrimitiveCollections {
                    mapping.put("key1", duplicate1)
                    mapping.put("key2", 1)
                    mapping.put("key3", duplicate1)
                    mapping.put("key4", 2)
                    mapping.put("key5", duplicate2)
                    mapping.put("key6", 3)
                    mapping.put("key7", duplicate2)
                }
            }

            val violation = exception.constraintViolations.first()
            val duplicates = listOf(duplicate1, duplicate2)
            with(violation) {
                fieldPath shouldBe FieldPath("mapping")
                message.placeholderValueMap shouldContain ("field.duplicates" to "$duplicates")
            }
        }

        @Test
        fun `duplicated message values result in a violation`() {
            val duplicate1 = hoursAndMinutes(2, 20)
            val duplicate2 = hoursAndMinutes(18, 40)

            val exception = assertThrows<ValidationException> {
                uniqueMessageCollections {
                    mapping.put("key1", duplicate1)
                    mapping.put("key2", minutes(15))
                    mapping.put("key3", duplicate1)
                    mapping.put("key4", minutes(30))
                    mapping.put("key5", duplicate2)
                    mapping.put("key6", minutes(45))
                    mapping.put("key7", duplicate2)
                }
            }

            val violation = exception.constraintViolations.first()
            val duplicates = listOf(duplicate1, duplicate2)
            with(violation) {
                fieldPath shouldBe FieldPath("mapping")
                message.placeholderValueMap shouldContain ("field.duplicates" to "$duplicates")
            }
        }

        @Test
        fun `unique primitive values do not result in a violation`() {
            assertDoesNotThrow {
                uniquePrimitiveCollections {
                    mapping.put("key1", 1)
                    mapping.put("key2", 2)
                    mapping.put("key3", 3)
                }
            }
        }

        @Test
        fun `unique message values do not result in a violation`() {
            assertDoesNotThrow {
                uniqueMessageCollections {
                    mapping.put("key1", minutes(1))
                    mapping.put("key2", minutes(2))
                    mapping.put("key3", minutes(3))
                }
            }
        }
    }

    @Test
    fun `empty list and map do not result in a violation`() {
        assertDoesNotThrow {
            UniquePrimitiveCollections.newBuilder()
                .build()
            UniqueMessageCollections.newBuilder()
                .build()
        }
    }
}
