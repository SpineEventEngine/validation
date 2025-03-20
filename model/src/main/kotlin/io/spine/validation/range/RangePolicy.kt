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

internal class RangePolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External
        @Where(field = OPTION_NAME, equals = RANGE)
        event: FieldOptionDiscovered
    ): Just<RangeFieldDiscovered> {
        val field = event.subject
        val file = event.file
        val primitiveType = checkFieldType(field, file)

        val option = event.option.unpack<RangeOption>()
        val range = option.value
        val context = RangeContext(range, primitiveType, field, file)
        val delimiter = context.checkDelimiter()

        val (left, right) = range.split(delimiter)
        val (minInclusive, maxInclusive) = context.checkBoundTypes(left, right)

        val lower = context.numericBound(left.substring(1), minInclusive)
        val upper = context.numericBound(right.dropLast(1), maxInclusive)
        context.checkRelation(lower, upper)

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return rangeFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            this.range = range
            lowerBound = lower.toProto()
            upperBound = upper.toProto()
        }.just()
    }
}

private fun checkFieldType(field: Field, file: File): PrimitiveType {
    val primitive = field.type.extractPrimitive()
    Compilation.check(primitive in supportedPrimitives, file, field.span) {
        "The field type `${field.type.name}` of `${field.qualifiedName}` is not supported by" +
                " the `($RANGE)` option. Supported field types: numbers and repeated" +
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

private fun RangeContext.checkBoundTypes(left: String, right: String): Pair<Boolean, Boolean> {
    val leftInclusive = when {
        left.startsWith("(") -> false
        left.startsWith("[") -> true
        else -> Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The lower bound should begin either" +
                    " with `(` for exclusive or `[` for inclusive values. Examples of" +
                    " the correct ranges: `(0..10]`, `(0..10)`, `[5..100]`."
        }
    }
    val rightInclusive = when {
        right.endsWith(")") -> false
        right.endsWith("]") -> true
        else -> Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The upper bound should end either" +
                    " with `)` for exclusive or `]` for inclusive values. Examples of" +
                    " the correct ranges: `(0..10]`, `(0..10)`, `[5..100]`."
        }
    }
    return leftInclusive to rightInclusive
}

private fun RangeContext.checkRelation(lower: NumericBound, upper: NumericBound) {
    Compilation.check(lower < upper, file, field.span) {
        "The `($RANGE)` option could not parse the range value `$range` specified for" +
                " `${field.qualifiedName}` field. The lower bound `${lower.value}` should be" +
                " less than the upper `${upper.value}`."
    }
}

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
