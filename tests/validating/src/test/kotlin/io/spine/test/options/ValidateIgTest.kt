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

package io.spine.test.options

import com.google.protobuf.Any
import com.google.protobuf.any
import com.google.protobuf.util.Timestamps
import io.spine.protobuf.AnyPacker
import io.spine.test.tools.validate.PersonName
import io.spine.test.tools.validate.inDepthValidatedMaps
import io.spine.test.tools.validate.inDepthValidatedMessage
import io.spine.test.tools.validate.inDepthValidatedRepeated
import io.spine.test.tools.validate.personName
import io.spine.type.UnknownTypeException
import io.spine.validation.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`(validate)` constraint should be compiled so that")
internal class ValidateIgTest {

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
        fun `accept a default instance of message with '(required)' field`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    validatable = defaultName
                }
            }
        }

        @Test
        fun `accept any non-validatable message`() {
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
                    any = invalidAnyName
                }
            }
        }

        @Test
        fun `reject a default instance of enclosed message with '(required)' field`() {
            assertThrows<ValidationException> {
                inDepthValidatedMessage {
                    any = defaultAnyName
                }
            }
        }

        @Test
        fun `accept a valid enclosed message`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    any = validAnyName
                }
            }
        }

        @Test
        fun `accept a default instance of 'Any'`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    any = defaultAny
                }
            }
        }

        @Test
        fun `accept any non-validatable enclosed message`() {
            assertDoesNotThrow {
                inDepthValidatedMessage {
                    any = anyTimestamp
                }
            }
        }

        @Test
        fun `reject any unknown enclosed message`() {
            assertThrows<UnknownTypeException> {
                inDepthValidatedMessage {
                    any = unknownAny
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
        fun `reject default instances with '(required)' fields`() {
            assertThrows<ValidationException> {
                inDepthValidatedRepeated {
                    validatable.addAll(
                        listOf(validName, defaultName, validName)
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
                        listOf(validAnyName, invalidAnyName, validAnyName)
                    )
                }
            }
        }

        @Test
        fun `reject default instances of enclosed messages with '(required)' fields`() {
            assertThrows<ValidationException> {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(validAnyName, defaultAnyName, validAnyName)
                    )
                }
            }
        }

        @Test
        fun `accept if all enclosed messages are valid`() {
            assertDoesNotThrow {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(validAnyName, validAnyName, validAnyName)
                    )
                }
            }
        }

        @Test
        fun `accept non-validatable enclosed messages`() {
            assertDoesNotThrow {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(anyTimestamp, anyTimestamp, anyTimestamp)
                    )
                }
            }
        }

        @Test
        fun `accept default instances of 'Any'`() {
            assertDoesNotThrow {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(defaultAny, defaultAny, defaultAny)
                    )
                }
            }
        }

        @Test
        fun `reject unknown enclosed messages`() {
            assertThrows<UnknownTypeException> {
                inDepthValidatedRepeated {
                    any.addAll(
                        listOf(unknownAny, unknownAny, unknownAny)
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
        fun `reject default instances of message values with '(required)' fields`() {
            assertThrows<ValidationException> {
                inDepthValidatedMaps {
                    validatable.putAll(
                        mapOf(
                            "k1" to validName,
                            "k2" to defaultName,
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
                            "k1" to validAnyName,
                            "k2" to invalidAnyName,
                            "k3" to validAnyName,
                        )
                    )
                }
            }
        }

        @Test
        fun `reject default instances of enclosed message values with '(required)' fields`() {
            assertThrows<ValidationException> {
                inDepthValidatedMaps {
                    any.putAll(
                        mapOf(
                            "k1" to validAnyName,
                            "k2" to defaultAnyName,
                            "k3" to validAnyName,
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
                            "k1" to validAnyName,
                            "k2" to validAnyName,
                            "k3" to validAnyName,
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
                            "k1" to anyTimestamp,
                            "k2" to anyTimestamp,
                            "k3" to anyTimestamp,
                        )
                    )
                }
            }
        }

        @Test
        fun `accept default instances of 'Any'`() {
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

        @Test
        fun `accept unknown enclosed message values`() {
            assertThrows<UnknownTypeException> {
                inDepthValidatedMaps {
                    any.putAll(
                        mapOf(
                            "k1" to unknownAny,
                            "k2" to unknownAny,
                            "k3" to unknownAny,
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
private val defaultName = PersonName.getDefaultInstance()
private val invalidName = PersonName.newBuilder()
    .setValue("Jack_Chan")
    .buildPartial()
private val validName = personName {
    value = "Jack Chan"
}

private val invalidAnyName = AnyPacker.pack(invalidName)
private val defaultAnyName = AnyPacker.pack(defaultName)
private val validAnyName = AnyPacker.pack(validName)
private val anyTimestamp = AnyPacker.pack(timestamp)
private val defaultAny = Any.getDefaultInstance()
private val unknownAny = any {
    typeUrl = "foo/bar"
}
