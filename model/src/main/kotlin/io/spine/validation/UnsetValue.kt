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

package io.spine.validation

import com.google.protobuf.ByteString
import io.spine.protodata.ast.Cardinality.CARDINALITY_LIST
import io.spine.protodata.ast.Cardinality.CARDINALITY_MAP
import io.spine.protodata.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.PrimitiveType.UNRECOGNIZED
import io.spine.protodata.ast.Type
import io.spine.protodata.ast.Type.KindCase.ENUMERATION
import io.spine.protodata.ast.Type.KindCase.KIND_NOT_SET
import io.spine.protodata.ast.Type.KindCase.MESSAGE
import io.spine.protodata.ast.Type.KindCase.PRIMITIVE
import io.spine.protodata.ast.cardinality
import io.spine.protodata.ast.toType
import io.spine.protodata.value.EnumValue
import io.spine.protodata.value.ListValue
import io.spine.protodata.value.MapValue
import io.spine.protodata.value.MessageValue
import io.spine.protodata.value.Value
import io.spine.protodata.value.value
import io.spine.string.shortly
import java.util.*

/**
 * A factory of [Value]s representing default states of Protobuf message fields.
 *
 * This class does not instantiate default values of Protobuf messages. It merely creates
 * instances of [Value] which represent values of Protobuf message fields, which are not set.
 */
public object UnsetValue {

    /**
     * Obtains an unset value for the given field.
     *
     * If a field is a number or a `bool`, it is impossible to tell if it's set or not.
     * In the binary representation, the `0` and `false` values may either be explicitly
     * set or just be the default values. For these cases, and only for these cases, the method
     * returns `Optional.empty()`.
     *
     * @return a [Value] with the field's default value or `Optional.empty()` if
     *   the field does not have an easily distinguished not-set value
     */
    public fun forField(field: Field): Optional<Value> =
        when (val cardinality = field.type.cardinality) {
            CARDINALITY_LIST -> Optional.of(
                value { listValue = ListValue.getDefaultInstance() }
            )

            CARDINALITY_MAP -> Optional.of(
                value { mapValue = MapValue.getDefaultInstance() }
            )
            CARDINALITY_SINGLE -> {
                val type = field.toType()
                singular(type)
            }
            else -> error(
                "Cannot create `Value` for the field `${field.shortly()}`." +
                        " Unexpected cardinality encountered: `$cardinality`."
            )
        }

    /**
     * Obtains the default value for the type of the given field.
     *
     * Behaves in a similar way to [forField], but never returns an empty list or an empty map.
     *
     * @return a [Value] with the field's default value or `Optional.empty()` if
     *   the field does not have an easily distinguished not-set value
     */
    public fun singular(type: Type): Optional<Value> {
        val kind = type.kindCase
        return when (type.kindCase) {
            MESSAGE -> Optional.of(messageValue(type))
            ENUMERATION -> Optional.of(enumValue(type))
            PRIMITIVE -> primitiveValue(type)
            KIND_NOT_SET -> error("Cannot create `Value` for the type of kind `$kind`.")
            else -> error("Cannot create `Value` for the type of kind `$kind`.")
        }
    }
}

@Suppress("ReturnCount")
private fun primitiveValue(type: Type): Optional<Value> {
    val primitiveType: PrimitiveType = type.primitive
    if (primitiveType == PT_UNKNOWN || primitiveType == UNRECOGNIZED) {
        error("Unknown primitive type `$primitiveType`.")
    }
    if (primitiveType == TYPE_STRING) {
        return Optional.of(value { stringValue = "" })
    }
    if (primitiveType == TYPE_BYTES) {
        return Optional.of(value { bytesValue = ByteString.EMPTY })
    }
    return Optional.empty()
}

private fun enumValue(type: Type): Value = value {
    enumValue = EnumValue.newBuilder()
        .setType(type.enumeration)
        .buildPartial()
}

private fun messageValue(type: Type): Value = value {
    messageValue = MessageValue.newBuilder()
        .setType(type.message)
        .buildPartial()
}
