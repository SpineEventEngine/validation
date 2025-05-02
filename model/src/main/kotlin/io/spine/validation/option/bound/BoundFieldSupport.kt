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

package io.spine.validation.option.bound

import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.File
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_INT64
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT64
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT64
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.check

/**
 * Determines whether the field type can be validated with a range-constraining option.
 */
internal object BoundFieldSupport {

    /**
     * Ensures the range-constraining option is applied to a field of a numeric type.
     *
     * If not, the method reports a compilation error.
     *
     * @param [field] The field to check.
     * @param [file] The file where the field is declared.
     * @param [option] The name of the option initiated the check.
     */
    internal fun checkFieldType(field: Field, file: File, option: String): PrimitiveType {
        val primitive = field.type.extractPrimitive()
        Compilation.check(primitive in numericPrimitives, file, field.span) {
            "The field type `${field.type.name}` of `${field.qualifiedName}` is not supported by" +
                    " the `($option)` option. Supported field types: numbers and repeated" +
                    " of numbers."
        }
        return primitive!!
    }
}

/**
 * Extracts a primitive type if this [FieldType] is singular or repeated field.
 *
 * Note that the option does not support maps, so we cannot use a similar extension
 * from ProtoData.
 */
private fun FieldType.extractPrimitive(): PrimitiveType? = when {
    isPrimitive -> primitive
    isList -> list.primitive
    else -> null
}

private val numericPrimitives = listOf(
    TYPE_FLOAT, TYPE_DOUBLE,
    TYPE_INT32, TYPE_INT64,
    TYPE_UINT32, TYPE_UINT64,
    TYPE_SINT32, TYPE_SINT64,
    TYPE_FIXED32, TYPE_FIXED64,
    TYPE_SFIXED32, TYPE_SFIXED64,
)
