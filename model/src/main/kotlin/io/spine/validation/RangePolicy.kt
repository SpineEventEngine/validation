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
import io.spine.validation.event.RangeFieldDiscovered
import io.spine.validation.event.rangeFieldDiscovered

internal class RangePolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = RANGE)
        event: FieldOptionDiscovered
    ): Just<RangeFieldDiscovered> {
        val field = event.subject
        val fieldType = field.type
        val file = event.file
        checkFieldType(fieldType, field, file)

        val option = event.option.unpack<RangeOption>()
        val range = option.value
        val bounds = range.split(RANGE_DELIMITER)
        checkDelimiter(bounds, range, field, file)

        val numberType = if (fieldType.isList) fieldType.list.primitive else fieldType.primitive
        val (left, right) = bounds
        val (min, max) =
            if (numberType in integerPrimitives) {
                val min = minValue {
                    bound = numericBound {
                        int64Value = left.toLong()
                    }
                    exclusive = left.contains("(")
                }
                val max = maxValue {
                    bound = numericBound {
                        int64Value = right.toLong()
                    }
                    exclusive = right.contains(")")
                }
                min to max
            } else {
                val min = minValue {
                    bound = numericBound {
                        doubleValue = left.toDouble()
                    }
                    exclusive = left.contains("(")
                }
                val max = maxValue {
                    bound = numericBound {
                        doubleValue = right.toDouble()
                    }
                    exclusive = right.contains(")")
                }
                min to max
            }

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return rangeFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            minValue = min
            maxValue = max
        }.just()
    }
}

private fun checkFieldType(type: FieldType, field: Field, file: File) =
    Compilation.check(type.isSupported(), file, field.span) {
        "The field type `$field.type` of `${field.qualifiedName}` is not supported" +
                " by the `($RANGE)` option. Supported field types: numbers and repeated" +
                " of numbers."
    }

private fun checkDelimiter(bounds: List<String>, range: String, field: Field, file: File) =
    Compilation.check(bounds.size == 2, file, field.span) {
        "The passed range has an incorrect format: `$range`. The `($RANGE)` option requires" +
                " `$RANGE_DELIMITER` to be used as a delimiter between left and right numeric bounds." +
                " For example, `(0${RANGE_DELIMITER}10]`."
    }

private const val RANGE_DELIMITER = ".."

private fun FieldType.isSupported(): Boolean = isSingularNumber || isRepeatedNumber

private val FieldType.isSingularNumber
    get() = primitive in numberPrimitives

private val FieldType.isRepeatedNumber
    get() = list.primitive in numberPrimitives

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

private val numberPrimitives = floatingPrimitives + integerPrimitives
