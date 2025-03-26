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

package io.spine.validation.range

import io.spine.core.External
import io.spine.core.Where
import io.spine.option.RangeOption
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
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.ref
import io.spine.protodata.ast.unpack
import io.spine.protodata.check
import io.spine.protodata.plugin.Policy
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.validation.OPTION_NAME
import io.spine.validation.RANGE
import io.spine.validation.defaultMessage
import io.spine.validation.event.RangeFieldDiscovered
import io.spine.validation.event.rangeFieldDiscovered

/**
 * Controls whether a field should be validated with the `(range)` option.
 *
 * Whenever a field marked with `(range)` option is discovered, emits
 * [RangeFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. Either `..` or ` .. ` is used as a range delimiter.
 * 3. Either `()` for exclusive or `[]` for inclusive bounds is used.
 * 4. The provided number has `.` for floating-point fields, and does not have `.`
 *    for integer fields.
 * 5. The provided bounds fit into the range of the target field type.
 * 6. The lower bound is strictly less than the upper one.
 *
 * Any violation of the above conditions leads to a compilation error.
 */
internal class RangePolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = RANGE)
        event: FieldOptionDiscovered
    ): Just<RangeFieldDiscovered> {
        val field = event.subject
        val file = event.file
        val primitiveType = checkFieldType(field, file, RANGE)

        val option = event.option.unpack<RangeOption>()
        val context = RangeContext(option.value, primitiveType, field, file)
        val delimiter = context.checkDelimiter()

        val (left, right) = context.range.split(delimiter)
        val (lowerExclusive, upperExclusive) = context.checkBoundTypes(left, right)
        val lower = context.checkNumericBound(left.substring(1), lowerExclusive)
        val upper = context.checkNumericBound(right.dropLast(1), upperExclusive)
        context.checkRelation(lower, upper)

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return rangeFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            this.range = context.range
            lowerBound = lower.toProto()
            upperBound = upper.toProto()
            this.file = file
        }.just()
    }
}

/**
 * Checks if the number-constraining option is applied to a field of numeric type.
 *
 * @param [field] The field to check.
 * @param [file] The file where the field is declared.
 * @param [option] The name of number-constraint option.
 */
internal fun checkFieldType(field: Field, file: File, option: String): PrimitiveType {
    val primitive = field.type.extractPrimitive()
    Compilation.check(primitive in supportedPrimitives, file, field.span) {
        "The field type `${field.type.name}` of `${field.qualifiedName}` is not supported by" +
                " the `($option)` option. Supported field types: numbers and repeated" +
                " of numbers."
    }
    return primitive!!
}

private fun RangeContext.checkDelimiter(): String =
    DELIMITER.find(range)?.value
        ?: Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The lower and upper bounds should be" +
                    " separated either with `..` or ` .. ` delimiter. Examples of the correct " +
                    " ranges: `(0..10]`, `[0 .. 10)`."
        }

private fun RangeContext.checkBoundTypes(lower: String, upper: String): Pair<Boolean, Boolean> {
    val lowerExclusive = when (lower.first()) {
        '(' -> true
        '[' -> false
        else -> Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The lower bound should begin either" +
                    " with `(` for exclusive or `[` for inclusive values. Examples of" +
                    " the correct ranges: `(0..10]`, `(0..10)`, `[5..100]`."
        }
    }
    val upperExclusive = when (upper.last()) {
        ')' -> true
        ']' -> false
        else -> Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The upper bound should end either" +
                    " with `)` for exclusive or `]` for inclusive values. Examples of" +
                    " the correct ranges: `(0..10]`, `(0..10)`, `[5..100]`."
        }
    }
    return lowerExclusive to upperExclusive
}

private fun RangeContext.checkRelation(lower: KotlinNumericBound, upper: KotlinNumericBound) {
    Compilation.check(lower <= upper, file, field.span) {
        "The `($RANGE)` option could not parse the range value `$range` specified for" +
                " `${field.qualifiedName}` field. The lower bound `${lower.value}` should be" +
                " less than the upper `${upper.value}`."
    }
}

/**
 * Extracts a primitive type if this [FieldType] is singular or repeated field.
 *
 * The option does not support maps, so we cannot use a similar extension from ProtoData.
 */
private fun FieldType.extractPrimitive(): PrimitiveType? = when {
    isPrimitive -> primitive
    isList -> list.primitive
    else -> null
}

private val DELIMITER = Regex("""(?<=\d)\s?\.\.\s?(?=[\d-])""")

private val supportedPrimitives = listOf(
    TYPE_FLOAT, TYPE_DOUBLE,
    TYPE_INT32, TYPE_INT64,
    TYPE_UINT32, TYPE_UINT64,
    TYPE_SINT32, TYPE_SINT64,
    TYPE_FIXED32, TYPE_FIXED64,
    TYPE_SFIXED32, TYPE_SFIXED64,
)
