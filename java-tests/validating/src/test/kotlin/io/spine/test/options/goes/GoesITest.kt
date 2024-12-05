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

package io.spine.test.options.goes

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`(goes)` constraint should")
internal class GoesITest {

    @MethodSource("io.spine.test.options.goes.TestData#onlyTargetFields")
    @ParameterizedTest(name = "throw if only the target `{0}` field is set")
    fun throwIfOnlyTargetFieldSet(message: Class<Message>, fieldName: String, fieldValue: Any) {
        val descriptor = message.protoDescriptor()
        val field = descriptor.findFieldByName(fieldName)!!
        val protoValue = protoValue(field, fieldValue)
        assertThrows<ValidationException> {
            message.newBuilder()
                .setField(field, protoValue)
                .build()
        }
    }

    @MethodSource("io.spine.test.options.goes.TestData#onlyCompanionFields")
    @ParameterizedTest(name = "not throw if only the companion `{0}` field is set")
    fun notThrowIfOnlyCompanionFieldSet(
        message: Class<out Message>,
        fieldName: String,
        fieldValue: Any
    ) {
        val descriptor = message.protoDescriptor()
        val companionField = descriptor.findFieldByName(fieldName)!!
        val companionProtoValue = protoValue(companionField, fieldValue)
        assertDoesNotThrow {
            message.newBuilder()
                .setField(companionField, companionProtoValue)
                .build()
        }
    }

    @MethodSource("io.spine.test.options.goes.TestData#bothTargetAndCompanionFields")
    @ParameterizedTest(name = "not throw if both the target `{0}` and its companion `{3}` fields are set")
    fun notThrowIfBothTargetAndCompanionFieldsSet(
        message: Class<out Message>,
        companionName: String,
        companionValue: Any,
        fieldName: String,
        fieldValue: Any
    ) {
        val descriptor = message.protoDescriptor()
        val field = descriptor.findFieldByName(fieldName)!!
        val protoValue = protoValue(field, fieldValue)
        val companionField = descriptor.findFieldByName(companionName)!!
        val companionProtoValue = protoValue(companionField, companionValue)
        assertDoesNotThrow {
            message.newBuilder()
                .setField(field, protoValue)
                .setField(companionField, companionProtoValue)
                .build()
        }
    }
}

private fun Class<out Message>.protoDescriptor() =
    getDeclaredMethod("getDescriptor").invoke(null) as Descriptor

private fun Class<out Message>.newBuilder() =
    getDeclaredMethod("newBuilder").invoke(null) as Message.Builder

/**
 * Converts the given [value] to Protobuf equivalent, if needed.
 *
 * The method makes the given value compatible with [Message.Builder.setField] method.
 *
 * Kotlin's [Map] is converted to Protobuf map entries, which are message-specific.
 * Other values are passed "as is".
 */
private fun protoValue(field: FieldDescriptor, value: Any): Any =
    if (value is Map<*, *>) {
        val descriptor = field.messageType
        value.map { descriptor.newMapEntry(it.key, it.value) }
    } else {
        value
    }

private fun Descriptor.newMapEntry(key: Any?, value: Any?) =
    DynamicMessage.newBuilder(this)
        .setField(this.findFieldByName("key"), key)
        .setField(this.findFieldByName("value"), value)
        .build()
