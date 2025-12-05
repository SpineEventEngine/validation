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

package io.spine.validation.jvm

import com.google.protobuf.ByteString
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_LIST
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_MAP
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.ast.PrimitiveType.PT_UNKNOWN
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BYTES
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.PrimitiveType.UNRECOGNIZED
import io.spine.tools.compiler.ast.Type
import io.spine.tools.compiler.ast.Type.KindCase.ENUMERATION
import io.spine.tools.compiler.ast.Type.KindCase.MESSAGE
import io.spine.tools.compiler.ast.Type.KindCase.PRIMITIVE
import io.spine.tools.compiler.ast.cardinality
import io.spine.tools.compiler.ast.toType
import io.spine.tools.compiler.value.EnumValue
import io.spine.tools.compiler.value.ListValue
import io.spine.tools.compiler.value.MapValue
import io.spine.tools.compiler.value.MessageValue
import io.spine.tools.compiler.value.Value
import io.spine.tools.compiler.value.value
import io.spine.string.shortly

/**
 * A factory of [Value]s representing default states of Protobuf message fields.
 *
 * This class does not instantiate default values of Protobuf messages. It merely creates
 * instances of [Value] which represent values of Protobuf message fields, which are not set.
 */
// TODO:2025-03-12:yevhenii.nadtochii: `EmptyFieldCheck` should not need this object.
//  See issue: https://github.com/SpineEventEngine/validation/issues/199
public object UnsetValue {

    /**
     * Obtains an unset value for the given field.
     *
     * If a field is a number or a `bool`, it is impossible to tell if it's set or not.
     * In the binary representation, the `0` and `false` values may either be explicitly
     * set or just be the default values.
     * For these cases, and only for these cases, the method returns `null`.
     *
     * @return a default field value or `null`, if the field type does not assume a not-set value.
     */
    public fun forField(field: Field): Value? =
        when (val cardinality = field.type.cardinality) {
            CARDINALITY_LIST -> value { listValue = ListValue.getDefaultInstance() }
            CARDINALITY_MAP ->  value { mapValue = MapValue.getDefaultInstance() }
            CARDINALITY_SINGLE ->  singular(field.toType())
            else -> error(
                "Cannot create unset `Value` for the field `${field.shortly()}`." +
                        " Unexpected cardinality encountered: `$cardinality`."
            )
        }

    /**
     * Obtains the default value for the type of the given field.
     *
     * Behaves similarly to [forField], but never returns an empty list or an empty map.
     *
     * @return a default field value or `null`, if the field type does not assume a not-set value.
     */
    public fun singular(type: Type): Value? {
        val kind = type.kindCase
        return when (type.kindCase) {
            MESSAGE -> messageValue(type)
            ENUMERATION -> enumValue(type)
            PRIMITIVE -> primitiveValue(type)
            else -> error("Cannot create `Value` for the type of kind `$kind`.")
        }
    }
}

@Suppress("ReturnCount")
private fun primitiveValue(type: Type): Value? {
    val primitiveType: PrimitiveType = type.primitive
    if (primitiveType == PT_UNKNOWN || primitiveType == UNRECOGNIZED) {
        error("Unknown primitive type `$primitiveType`.")
    }
    if (primitiveType == TYPE_STRING) {
        return value { stringValue = "" }
    }
    if (primitiveType == TYPE_BYTES) {
        return value { bytesValue = ByteString.EMPTY }
    }
    return null
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
