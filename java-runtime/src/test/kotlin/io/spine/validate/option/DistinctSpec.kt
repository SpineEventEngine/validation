/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.validate.option

import io.spine.test.validate.DistinctValues
import io.spine.test.validate.DistinctValues.Planet.EARTH
import io.spine.test.validate.DistinctValues.Planet.JUPITER
import io.spine.test.validate.DistinctValues.Planet.MARS
import io.spine.test.validate.distinctValues
import io.spine.validate.ValidationOfConstraintTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName(ValidationOfConstraintTest.VALIDATION_SHOULD + "analyze `(distinct)` option and")
internal class DistinctSpec : ValidationOfConstraintTest() {

    @Test
    fun `find out that empty message does not violate the contract`() {
        assertValid(DistinctValues.getDefaultInstance())
    }

    @Nested
    @DisplayName("find out that no duplicates do not violate contract")
    internal inner class NotViolates {

        @Test
        fun enums() = assertValid {
            distinctValues {
                enums.add(EARTH)
                enums.add(MARS)
                enums.add(JUPITER)
            }
        }

        @Test
        fun ints() = assertValid {
            distinctValues {
                ints.add(1)
                ints.add(2)
                ints.add(3)
            }
        }

        @Test
        fun strings() = assertValid {
            distinctValues {
                strings.add("First")
                strings.add("Second")
                strings.add("Third")
            }
        }

        @Test
        fun messages() = assertValid {
            distinctValues {
                messages.add(customMessageOf(1))
                messages.add(customMessageOf(2))
                messages.add(customMessageOf(3))
            }
        }
    }

    @Nested
    @DisplayName("find out that duplicate value violates contract")
    @Disabled("Until 'skipValidation()` is turned off.")
    internal inner class DuplicateViolates {

        @Test
        fun enums() = assertDoesNotBuild {
            distinctValues {
                enums.add(EARTH)
                enums.add(JUPITER)
                enums.add(EARTH)
            }
        }

        @Test
        fun ints() = assertDoesNotBuild {
            distinctValues {
                ints.add(1)
                ints.add(2)
                ints.add(1)
            }
        }

        @Test
        fun strings() = assertDoesNotBuild {
            distinctValues {
                strings.add("First")
                strings.add("Second")
                strings.add("First")
            }
        }

        @Test
        fun messages() = assertDoesNotBuild {
            distinctValues {
                messages.add(customMessageOf(1))
                messages.add(customMessageOf(2))
                messages.add(customMessageOf(1))
            }
        }
    }

    companion object {
        private fun customMessageOf(value: Long): DistinctValues.CustomMessage {
            return DistinctValues.CustomMessage.newBuilder()
                .setValue(value)
                .build()
        }
    }
}
