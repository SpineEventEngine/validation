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

import com.google.protobuf.ByteString.copyFromUtf8
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps
import io.spine.test.tools.validate.BytesCompanion
import io.spine.test.tools.validate.EnumCompanion
import io.spine.test.tools.validate.EnumForGoes.EFG_ITEM1
import io.spine.test.tools.validate.MapCompanion
import io.spine.test.tools.validate.MessageCompanion
import io.spine.test.tools.validate.RepeatedCompanion
import io.spine.test.tools.validate.StringCompanion
import io.spine.test.tools.validate.bytesCompanion
import io.spine.test.tools.validate.enumCompanion
import io.spine.test.tools.validate.mapCompanion
import io.spine.test.tools.validate.messageCompanion
import io.spine.test.tools.validate.repeatedCompanion
import io.spine.test.tools.validate.stringCompanion
import io.spine.validate.ValidationException
import kotlin.reflect.KClass
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource


@DisplayName("`(goes)` constraint should")
internal class GoesITest {

    private object FieldValues {
        val message = Timestamps.now()
        val enum: EnumValueDescriptor = EFG_ITEM1.valueDescriptor
        val string = "some companion text"
        val bytes = copyFromUtf8("some companion data")
        val repeated = listOf(1L, 2L, 3L)
        val map = mapOf("key" to 32)
    }

    @MethodSource("fieldsWithCompanion")
    @ParameterizedTest(name = "make a companion `message` field required when it is requested by `{0}` field")
    fun makeMessageCompanionFieldsRequired(fieldName: String, fieldValue: Any) =
        assertCompanionIsRequired(MessageCompanion::class, fieldName, fieldValue, FieldValues.message)

    @MethodSource("fieldsWithCompanion")
    @ParameterizedTest(name = "make a companion `enum` field required when it is requested by `{0}` field")
    fun makeEnumCompanionFieldsRequired(fieldName: String, fieldValue: Any) =
        assertCompanionIsRequired(EnumCompanion::class, fieldName, fieldValue, FieldValues.enum)

    @MethodSource("fieldsWithCompanion")
    @ParameterizedTest(name = "make a companion `string` field required when it is requested by `{0}` field")
    fun makeStringCompanionFieldsRequired(fieldName: String, fieldValue: Any) =
        assertCompanionIsRequired(StringCompanion::class, fieldName, fieldValue, FieldValues.string)

    @MethodSource("fieldsWithCompanion")
    @ParameterizedTest(name = "make a companion `bytes` field required when it is requested by `{0}` field")
    fun makeBytesCompanionFieldsRequired(fieldName: String, fieldValue: Any) =
        assertCompanionIsRequired(BytesCompanion::class, fieldName, fieldValue, FieldValues.bytes)

    @MethodSource("fieldsWithCompanion")
    @ParameterizedTest(name = "make a companion `repeated` field required when it is requested by `{0}` field")
    fun makeRepeatedCompanionFieldsRequired(fieldName: String, fieldValue: Any) =
        assertCompanionIsRequired(RepeatedCompanion::class, fieldName, fieldValue, FieldValues.repeated)

    @MethodSource("fieldsWithCompanion")
    @ParameterizedTest(name = "make a companion `map` field required when it is requested by `{0}` field")
    fun makeMapCompanionFieldsRequired(fieldName: String, fieldValue: Any) =
        assertCompanionIsRequired(MapCompanion::class, fieldName, fieldValue, FieldValues.map)

    @Nested inner class
    `do nothing if a companion field is set when it is` {

        @Test
        fun `a message field`() {
            assertDoesNotThrow {
                messageCompanion {
                    companion = FieldValues.message
                }
            }
        }

        @Test
        fun `an enum field`() {
            assertDoesNotThrow {
                enumCompanion {
                    companionValue = FieldValues.enum.number
                }
            }
        }

        @Test
        fun `a string field`() {
            assertDoesNotThrow {
                stringCompanion {
                    companion = FieldValues.string
                }
            }
        }

        @Test
        fun `a bytes field`() {
            assertDoesNotThrow {
                bytesCompanion {
                    companion = FieldValues.bytes
                }
            }
        }

        @Test
        fun `a repeated field`() {
            assertDoesNotThrow {
                repeatedCompanion {
                    companion.addAll(FieldValues.repeated)
                }
            }
        }

        @Test
        fun `a map field`() {
            assertDoesNotThrow {
                mapCompanion {
                    companion.putAll(FieldValues.map)
                }
            }
        }
    }

    private companion object {

        @JvmStatic
        fun fieldsWithCompanion() = listOf(
            arguments(named("message", "message_field"), FieldValues.message),
            arguments(named("enum", "enum_field"), FieldValues.enum),
            arguments(named("string", "string_field"), FieldValues.string),
            arguments(named("bytes", "bytes_field"), FieldValues.bytes),
            arguments(named("repeated", "repeated_field"), FieldValues.repeated),
            arguments(named("map", "map_field"), FieldValues.map),
        )
    }
}

private fun <M : Message> assertCompanionIsRequired(
    message: KClass<M>,
    targetField: String,
    fieldValue: Any,
    companionValue: Any
) {
    val companionClass = message.java
    val descriptor = companionClass.getDeclaredMethod("getDescriptor")
        .invoke(null) as Descriptor
    val field = descriptor.findFieldByName(targetField)!!
    val builder = companionClass.getDeclaredMethod("newBuilder")
        .invoke(null) as Message.Builder
    val safeFieldValue = if (fieldValue is Map<*, *>) protoField(descriptor, fieldValue) else fieldValue
    val safeCompanionValue = if (companionValue is Map<*, *>) protoCompanion(descriptor, companionValue) else companionValue
    assertThrows<ValidationException> {
        builder.setField(field, safeFieldValue)
            .build()
    }
    assertDoesNotThrow {
        builder
            .setField(descriptor.findFieldByName("companion")!!, safeCompanionValue)
            .setField(field, safeFieldValue)
            .build()
    }
}

private fun protoCompanion(companion: Descriptor, map: Map<*, *>): Any =
    protoMap("companion", companion, map)

private fun protoField(companion: Descriptor, map: Map<*, *>): Any =
    protoMap("map_field", companion, map)

private fun protoMap(fieldName: String, companion: Descriptor, map: Map<*, *>): Any {
    val mapField = companion.findFieldByName(fieldName)!!
    val mapEntryDescriptor = mapField.messageType
    val protoEntries = mutableListOf<DynamicMessage>()
    map.forEach { entry ->
        protoEntries.add(
            DynamicMessage.newBuilder(mapEntryDescriptor)
                .setField(mapEntryDescriptor.findFieldByName("key"), entry.key)
                .setField(mapEntryDescriptor.findFieldByName("value"), entry.value)
                .build()
        )
    }
    return protoEntries
}


