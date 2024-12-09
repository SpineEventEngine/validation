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

package io.spine.test.options.goes.given

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message

/**
 * Returns a Protobuf descriptor for the given message class.
 */
internal fun Class<out Message>.protoDescriptor() =
    getDeclaredMethod("getDescriptor").invoke(null) as Descriptor

/**
 * Creates a new builder of the given message class.
 */
internal fun Class<out Message>.newBuilder() =
    getDeclaredMethod("newBuilder").invoke(null) as Message.Builder

/**
 * Converts the given [value] to Protobuf equivalent, if needed.
 *
 * The method makes the given value compatible with [Message.Builder.setField] method.
 *
 * Kotlin's [Map] is converted to Protobuf map entries, which are message-specific.
 * Other values are passed "as is".
 */
internal fun protoValue(field: FieldDescriptor, value: Any): Any =
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
