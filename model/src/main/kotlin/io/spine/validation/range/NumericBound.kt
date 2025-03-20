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

import io.spine.validation.NumericBound as ProtoNumericBound

/**
 * One to one Kotlin representation of [ProtoNumericBound].
 *
 * We would like to have a Kotlin counterpart because of [UInt] and [ULong] types.
 * It eases the implementation of parsing and comparisons for such bounds.
 * Otherwise, we would have to care for special cases with unsigned types (which are just `int`
 * and `long` in Protobuf) when parsing and comparing such bounds. With this class,
 * Kotlin unsigned classes do it for us.
 */
internal data class NumericBound(
    val value: Any, // Cannot use `Number` because Kotlin's `UInt` and `ULong` are not numbers.
    val inclusive: Boolean
) : Comparable<NumericBound> {

    override fun compareTo(other: NumericBound): Int {
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
 * Creates an instance of [ProtoNumericBound] from this [NumericBound].
 */
internal fun NumericBound.toProto(): ProtoNumericBound {
    val builder = ProtoNumericBound.newBuilder()
        .setInclusive(inclusive)
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
            "Cannot convert `NumericBound` to Protobuf counterpart due to unexpected" +
                    " value type: `${value::class}`."
        )
    }
    return builder.build()
}
