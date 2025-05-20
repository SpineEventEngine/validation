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

package io.spine.validation.api.expression

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.isSingular
import io.spine.protodata.ast.name
import io.spine.protodata.ast.toFieldType
import io.spine.protodata.java.Expression
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.call
import io.spine.string.ti

/**
 * Returns an expression that converts the provided field [value] to a [String].
 *
 * See [FieldType.stringValueOf] for details upon how the value is converted.
 */
public fun Field.stringValueOf(value: Expression<*>): Expression<String> =
    type.stringValueOf(value)

/**
 * Returns an expression that converts the provided field [value] to a [String].
 *
 * How the value is converted to [String] is determined by this [FieldType].
 *
 * For most field types, an invocation of `toString()` is used because these
 * Protobuf types are represented as Java objects in the generated code.
 *
 * The following field types use `toString()`:
 *
 * 1. Messages and enums.
 * 2. Lists and maps.
 * 3. Bytes.
 *
 * Note that the `bytes` field type is represented with [com.google.protobuf.ByteString]
 * in the generated code. An invocation of `toString()` on this type is completely safe.
 * It prints a truncated, escaped version of its content along with its size.
 *
 * The remaining field types are handled as follows:
 *
 * 1. `string` remains unchanged.
 * 2. Scalar types (except `bytes`) are converted using the `String.valueOf` method.
 */
// TODO:2025-05-20:yevhenii.nadtochii: Update docs.
public fun FieldType.stringValueOf(value: Expression<*>): Expression<String> =
    when {
        isSingular -> stringifySingular(value)
        isList -> {
            val itemType = list.toFieldType()
            val stringifyItem = itemType.stringifySingular(ReadVar<Any?>("e"))
            Expression(
                """
                $value.stream()
                    .map(e -> $stringifyItem)
                    .collect($CollectorsClass.joining(",", "[", "]"))
                """.trimIndent()
            )
        }
        isMap -> {
            // We create a stringifying function only for values because keys in Protobuf
            // can only be represented with either integers or strings. So, these types
            // will be converted to string automatically during the string concatenation.
            val valueType = map.valueType.toFieldType()
            val stringifyValue = valueType.stringifySingular(ReadVar<Any?>("e.getValue()"))
            Expression(
                """
                $value.entrySet()
                    .stream()
                    .map(e -> "\"" + e.getKey() + "\":" + $stringifyValue)
                    .collect($CollectorsClass.joining(",", "{", "}"))
                """.trimIndent()
            )
        }
        else -> unsupportedFieldType(value)
    }

private fun FieldType.stringifySingular(value: Expression<*>) = when {
    isMessage -> SpineJson.call("toCompactJson", value)
    isEnum -> value.stringify()
    isPrimitive -> value.stringifyPrimitive(primitive)
    else -> unsupportedFieldType(value)
}

@Suppress("UNCHECKED_CAST") // Casting string field's value to `String` is safe.
private fun Expression<*>.stringifyPrimitive(primitive: PrimitiveType) =
    when (primitive) {
        TYPE_STRING -> this as Expression<String>
        TYPE_BYTES -> stringify()
        else -> StringClass.call("valueOf", this)
    }

private fun FieldType.unsupportedFieldType(value: Expression<*>): Nothing = error(
    """
    Cannot convert `$value` expression to `Expression<String>`.
    Unsupported field type: `${name}`.
    """.ti()
)
