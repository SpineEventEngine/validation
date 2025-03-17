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
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.ref
import io.spine.protodata.ast.unpack
import io.spine.protodata.check
import io.spine.protodata.plugin.Policy
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.validation.NumberType.FLOATING_POINT
import io.spine.validation.NumberType.INTEGER
import io.spine.validation.RangeSyntax.RANGE_DELIMITER
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
        val numberType = checkFieldType(field, file)

        val option = event.option.unpack<RangeOption>()
        val rangeValue = option.value
        checkRangeSyntax(rangeValue, numberType, field, file)

        val (left, right) = rangeValue.split(RANGE_DELIMITER)
        val (min, max) = numberType.toMinMax(left, right)
        numberType.checkMinLessMax(min, max, rangeValue, field, file)

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return rangeFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            range = rangeValue
            minValue = min
            maxValue = max
        }.just()
    }

    private fun NumberType.toMinMax(left: String, right: String): Pair<MinValue, MaxValue> =
        when (this) {
            INTEGER -> {
                val min = min(left.trim().substring(1).toLong(), left.contains("("))
                val max = max(right.trim().dropLast(1).toLong(), right.contains(")"))
                min to max
            }
            FLOATING_POINT -> {
                val min = min(left.trim().substring(1).toDouble(), left.contains("("))
                val max = max(right.trim().dropLast(1).toDouble(), right.contains(")"))
                min to max
            }
        }
}

private fun checkFieldType(field: Field, file: File): NumberType =
    when (val type = field.type.extractPrimitive()) {
        in integerPrimitives -> INTEGER
        in floatingPrimitives -> FLOATING_POINT
        else -> Compilation.error(file, field.span) {
            "The field type `${field.type}` of `${field.qualifiedName}` is not supported by" +
                    " the `($RANGE)` option. Supported field types: numbers and repeated" +
                    " of numbers."
        }
    }

private fun checkRangeSyntax(range: String, type: NumberType, field: Field, file: File) =
    Compilation.check(RangeSyntax.check(range, type), file, field.span) {
        "The passed range has an incorrect syntax: `$range`. Examples of the correct ranges:" +
                " `(0..10)`, `[-10..+32)`, `[0.5..2.5]`. Please see docs to `($RANGE)` for" +
                " the full syntax description."
    }

private fun NumberType.checkMinLessMax(
    min: MinValue,
    max: MaxValue,
    range: String,
    field: Field,
    file: File
) {
    val minLessMax = when (this) {
        INTEGER -> min.bound.int64Value < max.bound.int64Value
        FLOATING_POINT -> min.bound.doubleValue < max.bound.doubleValue
    }
    Compilation.check(minLessMax, file, field.span) {
        "The passed range `$range` has its lower bound equal or more than the upper one." +
                " The `($RANGE)` option requires the lower bound be strictly less than" +
                " the upper one."
    }
}

private fun FieldType.extractPrimitive(): PrimitiveType? = when {
    isPrimitive -> primitive
    isList -> list.primitive
    else -> null
}

private enum class NumberType {
    FLOATING_POINT, INTEGER;
}

private object RangeSyntax {

    private val IntegerRange = Regex("""[\[(][+-]?\d+\s?\.\.\s?[+-]?\d+[\])]""")
    private val FloatingRange = Regex("""[\[(][+-]?\d+(\.\d+)?\s?\.\.\s?[+-]?\d+(\.\d+)?[\])]""")

    const val RANGE_DELIMITER = ".."

    fun check(range: String, numberType: NumberType): Boolean =
        when(numberType) {
            INTEGER -> IntegerRange.matches(range)
            FLOATING_POINT -> FloatingRange.matches(range)
        }
}

private val floatingPrimitives = listOf(
    TYPE_DOUBLE,
    TYPE_FLOAT,
)

private val integerPrimitives = listOf(
    TYPE_INT32, TYPE_INT64,
    TYPE_UINT32, TYPE_UINT64,
    TYPE_SINT32, TYPE_SINT64,
    TYPE_FIXED32, TYPE_FIXED64,
    TYPE_SFIXED32, TYPE_SFIXED64,
)
