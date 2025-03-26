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
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.check
import io.spine.validation.NumericBound as ProtoNumericBound

/**
 * One-to-one Kotlin representation of [ProtoNumericBound].
 *
 * We would like to have a Kotlin counterpart because of [UInt] and [ULong] types.
 * It eases the implementation of parsing and comparisons for such bounds.
 * Otherwise, we would have to care for special cases with unsigned types
 * (which are just `int` and `long` in Protobuf) when parsing and comparing
 * such bounds. With this data class, Kotlin unsigned classes do it for us.
 */
internal data class KotlinNumericBound(
    val value: Any, // Cannot use `Number` because Kotlin's `UInt` and `ULong` are not numbers.
    val exclusive: Boolean
) : Comparable<KotlinNumericBound> {

    override fun compareTo(other: KotlinNumericBound): Int {
        val otherValue = other.value
        if (otherValue::class != value::class) {
            error(
                "Illegal comparison of numeric bounds with incompatible value types." +
                        " Type of the instance value: `${value::class}`, the other value has" +
                        " the type of `${otherValue::class}`."
            )
        }
        return when (value) {
            is Float -> value.compareTo(otherValue as Float)
            is Double -> value.compareTo(otherValue as Double)
            is Int -> value.compareTo(otherValue as Int)
            is Long -> value.compareTo(otherValue as Long)
            is UInt -> value.compareTo(otherValue as UInt)
            is ULong -> value.compareTo(otherValue as ULong)
            else -> error(
                "Illegal comparison of numeric bound with unsupported" +
                        " value type: `${value::class}`."
            )
        }
    }
}

/**
 * Creates an instance of [ProtoNumericBound] from this [KotlinNumericBound].
 */
internal fun KotlinNumericBound.toProto(): ProtoNumericBound {
    val builder = ProtoNumericBound.newBuilder()
        .setExclusive(exclusive)
    when (value) {
        is Float -> builder.setFloatValue(value)
        is Double -> builder.setDoubleValue(value)
        is Int -> builder.setInt32Value(value)
        is Long -> builder.setInt64Value(value)

        // The resulting `int` value will have the same binary representation as `UInt` value.
        is UInt -> builder.setUint32Value(value.toInt())

        // The resulting `long` value will have the same binary representation as `ULong` value.
        is ULong -> builder.setUint64Value(value.toLong())

        else -> error(
            "Cannot convert `KotlinNumericBound` to `NumericBound` due to unexpected" +
                    " value type: `${value::class}`."
        )
    }
    return builder.build()
}

/**
 * Parses the given string [value] to a [KotlinNumericBound].
 *
 * This method checks the following:
 *
 * 1) The provided number has `.` for floating-point fields, and does not have `.`
 *    for integer fields.
 * 2) The provided number fits into the range of the target field type.
 *
 * Any violation of the above conditions leads to a compilation error.
 *
 * @return The parsed numeric bound.
 */
internal fun NumericBoundContext.checkNumericBound(
    value: String,
    exclusive: Boolean
): KotlinNumericBound {
    if (primitiveType in listOf(TYPE_FLOAT, TYPE_DOUBLE)) {
        Compilation.check(FLOAT.matches(value), file, field.span) {
            "The `($optionName)` option could not parse the `$value` bound value specified for" +
                    " `${field.qualifiedName}` field. Please make sure the provided value is" +
                    " a floating-point number. Examples: `12.3`, `-0.1`, `6.02E2`."
        }
    } else {
        Compilation.check(INTEGER.matches(value), file, field.span) {
            "The `($optionName)` option could not parse the `$value` bound value specified for" +
                    " `${field.qualifiedName}` field. Please make sure the provided value is" +
                    " an integer number. Examples: `123`, `-567823`."
        }
    }
    val number = when (primitiveType) {
        TYPE_FLOAT -> value.toFloatOrNull().takeIf { !"$it".contains("Infinity") }
        TYPE_DOUBLE -> value.toDoubleOrNull().takeIf { !"$it".contains("Infinity") }
        TYPE_INT32, TYPE_SINT32, TYPE_SFIXED32 -> value.toIntOrNull()
        TYPE_INT64, TYPE_SINT64, TYPE_SFIXED64 -> value.toLongOrNull()
        TYPE_UINT32, TYPE_FIXED32 -> value.toUIntOrNull()
        TYPE_UINT64, TYPE_FIXED64 -> value.toULongOrNull()
        else -> unexpectedPrimitiveType(primitiveType)
    }
    Compilation.check(number != null, file, field.span) {
        "The `($optionName)` option could not parse the `$value` bound value specified for" +
                " `${field.qualifiedName}` field. The value is out of range for the field" +
                " type `${field.type.name}` the option is applied to."
    }
    return KotlinNumericBound(number!!, exclusive)
}

private fun unexpectedPrimitiveType(primitiveType: PrimitiveType): Nothing =
    error(
        "`KotlinNumericBound` cannot be created for `$primitiveType` field type." +
                " Please make sure the policy correctly filtered unsupported field types."
    )

private val INTEGER = Regex("[-+]?\\d+")
private val FLOAT = Regex("[-+]?\\d+\\.\\d+([eE][-+]?\\d+)?")
