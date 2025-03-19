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

import io.spine.protodata.Compilation
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
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.check
import io.spine.validation.NumericBound
import io.spine.validation.RANGE

@Suppress("CyclomaticComplexMethod") // "You have no choice, you have to choose".
internal fun ParsingContext.numericBounds(
    min: String,
    minInclusive: Boolean,
    max: String,
    maxInclusive: Boolean
): Pair<NumericBound, NumericBound> {
    val lowerBound = NumericBound.newBuilder()
    val upperBound = NumericBound.newBuilder()
    when (primitiveType) {
        TYPE_FLOAT -> {
            val lower = min.toFloatOrNull().also { checkLower(it) }
            val upper = max.toFloatOrNull().also { checkUpper(it) }
            checkRelation(lower!!, upper!!)
            lowerBound.setFloatValue(lower)
            upperBound.setFloatValue(upper)
        }

        TYPE_DOUBLE -> {
            val lower = min.toDoubleOrNull().also { checkLower(it) }
            val upper = max.toDoubleOrNull().also { checkUpper(it) }
            checkRelation(lower!!, upper!!)
            lowerBound.setDoubleValue(lower)
            upperBound.setDoubleValue(upper)
        }

        TYPE_INT32, TYPE_SINT32, TYPE_SFIXED32 -> {
            val lower = min.toIntOrNull().also { checkLower(it) }
            val upper = max.toIntOrNull().also { checkUpper(it) }
            checkRelation(lower!!, upper!!)
            lowerBound.setInt32Value(lower)
            upperBound.setInt32Value(upper)
        }

        TYPE_INT64, TYPE_SINT64, TYPE_SFIXED64 -> {
            val lower = min.toLongOrNull().also { checkLower(it) }
            val upper = max.toLongOrNull().also { checkUpper(it) }
            checkRelation(lower!!, upper!!)
            lowerBound.setInt64Value(lower)
            upperBound.setInt64Value(upper)
        }

        TYPE_UINT32, TYPE_FIXED32 -> {
            val lower = min.toUIntOrNull().also { checkLower(it) }
            val upper = max.toUIntOrNull().also { checkUpper(it) }
            checkRelation(lower!!, upper!!)

            // The resulting `Int` value has the same binary representation as this `UInt` value.
            lowerBound.setUint32Value(lower.toInt())
            upperBound.setUint32Value(upper.toInt())
        }

        TYPE_UINT64, TYPE_FIXED64 -> {
            val lower = min.toULongOrNull().also { checkLower(it) }
            val upper = max.toULongOrNull().also { checkUpper(it) }
            checkRelation(lower!!, upper!!)

            // The resulting `Long` value has the same binary representation as this `ULong` value.
            lowerBound.setUint64Value(lower.toLong())
            upperBound.setUint64Value(upper.toLong())
        }

        else -> error(
            "`NumericBound` cannot be created for `$primitiveType` field type." +
                    " Please make sure, `RangePolicy` correctly filtered unsupported field types."
        )
    }
    lowerBound.setInclusive(minInclusive)
    upperBound.setInclusive(maxInclusive)
    return lowerBound.build() to upperBound.build()
}

private  fun ParsingContext.checkLower(value: Any?) = checkBound("lower", value != null)

private  fun ParsingContext.checkUpper(value: Any?) = checkBound("upper", value != null)

private fun ParsingContext.checkBound(boundName: String, condition: Boolean) {
    Compilation.check(condition, file, field.span) {
        "The `($RANGE)` option could not parse the range value `$range` specified for" +
                " `${field.qualifiedName}` field. The $boundName bound should be" +
                " within the range of the field (`${field.type}`)" +
                " the option is applied to."
    }
}

private fun <T : Comparable<T>> ParsingContext.checkRelation(lower: T, upper: T) {
    Compilation.check(lower < upper, file, field.span) {
        "The `($RANGE)` option could not parse the range value `$range` specified for" +
                " `${field.qualifiedName}` field. The lower bound `$lower` should be strictly" +
                " less than the upper `$upper`."
    }
}
