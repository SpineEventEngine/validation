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

package io.spine.test.options

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import com.google.protobuf.Message.Builder
import com.google.protobuf.ProtocolMessageEnum

/**
 * This file contains fixtures used by tests that verify the created instances
 * of [ConstraintViolation][io.spine.validate.ConstraintViolation].
 */
@Suppress("unused")
val ABOUT = ""

/**
 * Sets the given [field] with a [value] adapted to the field's expected type.
 *
 * The provided [value] can be converted to ensure compatibility with
 * the field's [descriptor][FieldDescriptor]'s requirements.
 *
 * The following conversions take place:
 *
 * - [Map] becomes a list of [MapEntry][com.google.protobuf.MapEntry].
 * - Enum constant becomes [EnumValueDescriptor][com.google.protobuf.Descriptors.EnumValueDescriptor].
 *
 * Other values are passed "as is".
 *
 * @param field The descriptor of the field to be set.
 * @param value The value to assign for the field.
 *
 * @see adaptedValue
 */
fun Builder.set(field: FieldDescriptor, value: Any): Builder {
    val adapted = adaptedValue(field, value)
    return setField(field, adapted)
}

/**
 * Adapts the provided [value] to the [field] descriptor, so that
 * it is compatible with [Message.Builder.setField] method.
 *
 * To be used with the mentioned method, the value must be of the correct type
 * for this field, that is, the same type that [Message.getField] returns.
 */
private fun adaptedValue(field: FieldDescriptor, value: Any): Any =
    when (value) {
        is Map<*, *> -> value.map { field.messageType.newMapEntry(it.key, it.value) }
        is ProtocolMessageEnum -> value.valueDescriptor
        else -> value
    }

/**
 * Creates a map entry for the given message [Descriptor].
 *
 * Entries are created using a [DynamicMessage]. We cannot create such entries
 * directly because it is a private inner class within generated messages.
 */
private fun Descriptor.newMapEntry(key: Any?, value: Any?) =
    DynamicMessage.newBuilder(this)
        .setField(findFieldByName("key"), key)
        .setField(findFieldByName("value"), value)
        .build()
