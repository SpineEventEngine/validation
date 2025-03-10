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

import com.google.protobuf.Any
import com.google.protobuf.util.Timestamps
import io.spine.protobuf.AnyPacker
import io.spine.test.tools.validate.PersonName
import io.spine.test.tools.validate.inDepthValidatedMaps
import io.spine.test.tools.validate.inDepthValidatedMessage
import io.spine.test.tools.validate.inDepthValidatedRepeated
import io.spine.test.tools.validate.personName
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`(validate)` constraint should be compiled so that")
internal class ValidateITest {

    @Nested internal inner class
    `on a singular message field` {

        @Test
        fun `reject an invalid message`() {
            assertThrows<ValidationException> {
                inDepthValidatedMessage {
                    validatable = invalidName
                }
            }
        }

        @Test
        fun `accept a valid message`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    validatable = validName
                }
            }
        }

        @Test
        fun `accept a non-validatable message`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    nonValidatable = timestamp
                }
            }
        }
    }

    @Nested internal inner class
    `on a singular 'Any' field` {

        @Test
        fun `reject an invalid enclosed message`() {
            assertThrows<ValidationException> {
                inDepthValidatedMessage {
                    any = invalidAny
                }
            }
        }

        @Test
        fun `accept a valid enclosed message`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    any = validAny
                }
            }
        }

        @Test
        fun `accept a non-validatable enclosed message`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    any = timestampAny
                }
            }
        }

        @Test
        fun `accept a default instance`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    any = defaultAny
                }
            }
        }
    }

    @Nested internal inner class
    `on a repeated message field` {

        @Test
        fun `reject invalid messages`() {
            assertThrows<ValidationException> {
                inDepthValidatedRepeated {
                    validatable.addAll(
                        listOf(validName, invalidName, validName)
                    )
                }
            }
        }

        @Test
        fun `accept if all messages are valid`() {
            assertDoesNotThrow {
                inDepthValidatedRepeated {
                    validatable.addAll(
                        listOf(validName, validName, validName)
                    )
                }
            }
        }

        @Test
        fun `accept non-validatable messages`() {
            assertDoesNotThrow {
                inDepthValidatedRepeated {
                    nonValidatable.addAll(
                        listOf(timestamp, timestamp, timestamp)
                    )
                }
            }
        }
    }

    @Nested internal inner class
    `on a repeated 'Any' field` {

        @Test
        fun `reject invalid enclosed messages`() {
            assertThrows<ValidationException> {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(validAny, invalidAny, validAny)
                    )
                }
            }
        }

        @Test
        fun `accept if all enclosed messages are valid`() {
            assertDoesNotThrow {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(validAny, validAny, validAny)
                    )
                }
            }
        }

        @Test
        fun `accept non-validatable enclosed messages`() {
            assertDoesNotThrow {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(timestampAny, timestampAny, timestampAny)
                    )
                }
            }
        }

        @Test
        fun `accept default instances`() {
            assertDoesNotThrow {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(defaultAny, defaultAny, defaultAny)
                    )
                }
            }
        }
    }

    @Nested internal inner class
    `on a map with message values` {

        @Test
        fun `reject invalid message values`() {
            assertThrows<ValidationException> {
                inDepthValidatedMaps {
                    validatable.putAll(
                        mapOf(
                            "k1" to validName,
                            "k2" to invalidName,
                            "k3" to validName,
                        )
                    )
                }
            }
        }

        @Test
        fun `accept if all message values are valid`() {
            assertDoesNotThrow {
                inDepthValidatedMaps {
                    validatable.putAll(
                        mapOf(
                            "k1" to validName,
                            "k2" to validName,
                            "k3" to validName,
                        )
                    )
                }
            }
        }

        @Test
        fun `accept non-validatable message values`() {
            assertDoesNotThrow {
                inDepthValidatedMaps {
                    nonValidatable.putAll(
                        mapOf(
                            "k1" to timestamp,
                            "k2" to timestamp,
                            "k3" to timestamp,
                        )
                    )
                }
            }
        }
    }

    @Nested internal inner class
    `on a map with 'Any' values` {

        @Test
        fun `reject invalid enclosed message values`() {
            assertThrows<ValidationException> {
                inDepthValidatedMaps {
                    any.putAll(
                        mapOf(
                            "k1" to validAny,
                            "k2" to invalidAny,
                            "k3" to validAny,
                        )
                    )
                }
            }
        }

        @Test
        fun `accept if all enclosed message values are valid`() {
            assertDoesNotThrow {
                inDepthValidatedMaps {
                    any.putAll(
                        mapOf(
                            "k1" to validAny,
                            "k2" to validAny,
                            "k3" to validAny,
                        )
                    )
                }
            }
        }

        @Test
        fun `accept non-validatable enclosed message values`() {
            assertDoesNotThrow {
                inDepthValidatedMaps {
                    any.putAll(
                        mapOf(
                            "k1" to timestampAny,
                            "k2" to timestampAny,
                            "k3" to timestampAny,
                        )
                    )
                }
            }
        }

        @Test
        fun `accept default instances`() {
            assertDoesNotThrow {
                inDepthValidatedMaps {
                    any.putAll(
                        mapOf(
                            "k1" to defaultAny,
                            "k2" to defaultAny,
                            "k3" to defaultAny,
                        )
                    )
                }
            }
        }
    }

    @Test
    fun `accept empty repeated and maps`() {
        assertDoesNotThrow {
            inDepthValidatedRepeated {}
        }
        assertDoesNotThrow {
            inDepthValidatedMaps {}
        }
    }
}

private val timestamp = Timestamps.now()
private val invalidName = PersonName.newBuilder()
    .setValue("Jack_Chan")
    .buildPartial()
private val validName = personName {
    value = "Jack Chan"
}

private val invalidAny = AnyPacker.pack(invalidName)
private val validAny = AnyPacker.pack(validName)
private val timestampAny = AnyPacker.pack(timestamp)
private val defaultAny = Any.getDefaultInstance()
