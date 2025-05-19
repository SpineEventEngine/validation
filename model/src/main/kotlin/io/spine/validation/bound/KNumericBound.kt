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

package io.spine.validation.bound

import io.spine.base.FieldPath
import io.spine.string.qualifiedClassName
import io.spine.string.simply

/**
 * One-to-one Kotlin representation of [NumericBound].
 *
 * We would like to have a Kotlin counterpart because of [UInt] and [ULong] types.
 *
 * It eases the implementation of parsing and comparisons for such bounds.
 * Otherwise, we would have to care for special cases with unsigned types
 * (which come just as `int` and `long` from Protobuf) when parsing and comparing them.
 *
 * With this data class, Kotlin unsigned classes do it for us.
 *
 * @param value The value of this bound. It can be any implementation of [Number],
 *   [UInt] or [ULong] (they don't extend [Number]) and [FieldPath] for field-based bounds.
 * @param exclusive Specifies whether this bound is exclusive.
 */
internal data class KNumericBound(
    val value: Any,
    val exclusive: Boolean
) : Comparable<KNumericBound> {

    override fun compareTo(other: KNumericBound): Int {
        val otherValue = other.value
        if (otherValue::class != value::class) {
            error(
                "Illegal comparison of numeric bounds with incompatible value types." +
                        " Type of the instance value: `${value.qualifiedClassName}`, the other" +
                        " value has the type of `${otherValue.qualifiedClassName}`."
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
                "Illegal comparison of numeric bound with unsupported value type:" +
                        " `${value.qualifiedClassName}`."
            )
        }
    }
}

/**
 * Creates an instance of [NumericBound] from this [KNumericBound].
 */
internal fun KNumericBound.toProto(): NumericBound {
    val builder = NumericBound.newBuilder()
        .setExclusive(exclusive)
    when (value) {
        is Int -> builder.setInt32Value(value)
        is Double -> builder.setDoubleValue(value)
        is Long -> builder.setInt64Value(value)
        is Float -> builder.setFloatValue(value)

        // The resulting `int` value will have the same binary representation as `UInt` value.
        is UInt -> builder.setUint32Value(value.toInt())

        // The resulting `long` value will have the same binary representation as `ULong` value.
        is ULong -> builder.setUint64Value(value.toLong())

        is FieldPath -> builder.setFieldValue(value)

        else -> error(
            "Cannot convert `${simply<KNumericBound>()}` to `${simply<NumericBound>()}`" +
                    " due to unexpected value type: `${value.qualifiedClassName}`."
        )
    }
    return builder.build()
}
