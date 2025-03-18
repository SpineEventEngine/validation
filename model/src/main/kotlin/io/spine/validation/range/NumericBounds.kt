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
import kotlin.contracts.contract

internal fun ParsingContext.lowerBound(value: String, inclusive: Boolean): NumericBound =
    numericBound("lower", value, inclusive)

internal fun ParsingContext.upperBound(value: String, inclusive: Boolean): NumericBound =
    numericBound("upper", value, inclusive)

private fun ParsingContext.numericBound(
    boundName: String,
    value: String,
    inclusive: Boolean
): NumericBound {
    val bound = NumericBound.newBuilder()
    when (primitiveType) {
        TYPE_FLOAT -> {
            val float = value.toFloatOrNull()
            compilationCheck(boundName, float != null)
            bound.setFloatValue(float)
        }

        TYPE_DOUBLE -> {
            val double = value.toDoubleOrNull()
            compilationCheck(boundName, double != null)
            bound.setDoubleValue(double)
        }

        TYPE_INT32, TYPE_SINT32, TYPE_SFIXED32 -> {
            val int32 = value.toIntOrNull()
            compilationCheck(boundName, int32 != null)
            bound.setInt32Value(int32)
        }

        TYPE_INT64, TYPE_SINT64, TYPE_SFIXED64 -> {
            val int64 = value.toLongOrNull()
            compilationCheck(boundName, int64 != null)
            bound.setInt64Value(int64)
        }

        TYPE_UINT32, TYPE_FIXED32 -> {
            val uint32 = value.toUIntOrNull()
            compilationCheck(boundName, uint32 != null)
            bound.setUint32Value(uint32.toInt()) // The resulting `Int` value has the same binary
                                                 // representation as this `UInt` value.
        }


        TYPE_UINT64, TYPE_FIXED64 -> {
            val uint64 = value.toULongOrNull()
            compilationCheck(boundName, uint64 != null)
            bound.setUint64Value(uint64.toLong()) // The resulting `Long` value has the same binary
                                                  // representation as this `ULong` value.
        }

        else -> error(
            "`NumericBound` cannot be created for `$primitiveType` field type." +
                    " Please make sure, `RangePolicy` correctly filtered unsupported field types."
        )
    }
    return bound.setInclusive(inclusive)
        .build()
}

private fun ParsingContext.compilationCheck(boundName: String, condition: Boolean) {
    contract {
        returns() implies condition
    }
    Compilation.check(condition, file, field.span) {
        "The `($RANGE)` option could not parse the range value `$range` specified for" +
                " `${field.qualifiedName}` field. The $boundName bound should be" +
                " within the range of the field (`${field.type}`)" +
                " the option is applied to."
    }
}
