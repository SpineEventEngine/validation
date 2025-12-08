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

package io.spine.tools.validation

import io.spine.validation.RangeDoubleLowerOverflow
import io.spine.validation.RangeDoubleMinMax
import io.spine.validation.RangeDoubleUpperOverflow
import io.spine.validation.RangeFixed32LowerOverflow
import io.spine.validation.RangeFixed32MinMax
import io.spine.validation.RangeFixed32UpperOverflow
import io.spine.validation.RangeFixed64LowerOverflow
import io.spine.validation.RangeFixed64MinMax
import io.spine.validation.RangeFixed64UpperOverflow
import io.spine.validation.RangeFloatLowerOverflow
import io.spine.validation.RangeFloatMinMax
import io.spine.validation.RangeFloatUpperOverflow
import io.spine.validation.RangeInt32LowerOverflow
import io.spine.validation.RangeInt32MinMax
import io.spine.validation.RangeInt32UpperOverflow
import io.spine.validation.RangeInt64LowerOverflow
import io.spine.validation.RangeInt64MinMax
import io.spine.validation.RangeInt64UpperOverflow
import io.spine.validation.RangeInvalidDelimiter1
import io.spine.validation.RangeInvalidDelimiter2
import io.spine.validation.RangeInvalidDelimiter3
import io.spine.validation.RangeInvalidDelimiter4
import io.spine.validation.RangeOnBool
import io.spine.validation.RangeOnBoolRepeated
import io.spine.validation.RangeOnBytes
import io.spine.validation.RangeOnBytesRepeated
import io.spine.validation.RangeOnDoubleMap
import io.spine.validation.RangeOnEnum
import io.spine.validation.RangeOnEnumRepeated
import io.spine.validation.RangeOnIntMap
import io.spine.validation.RangeOnMessage
import io.spine.validation.RangeOnMessageRepeated
import io.spine.validation.RangeOnString
import io.spine.validation.RangeOnStringMap
import io.spine.validation.RangeOnStringRepeated
import io.spine.validation.RangeSFixed32LowerOverflow
import io.spine.validation.RangeSFixed32MinMax
import io.spine.validation.RangeSFixed32UpperOverflow
import io.spine.validation.RangeSFixed64LowerOverflow
import io.spine.validation.RangeSFixed64MinMax
import io.spine.validation.RangeSFixed64UpperOverflow
import io.spine.validation.RangeSInt32LowerOverflow
import io.spine.validation.RangeSInt32MinMax
import io.spine.validation.RangeSInt32UpperOverflow
import io.spine.validation.RangeSInt64LowerOverflow
import io.spine.validation.RangeSInt64MinMax
import io.spine.validation.RangeSInt64UpperOverflow
import io.spine.validation.RangeUInt32LowerOverflow
import io.spine.validation.RangeUInt32MinMax
import io.spine.validation.RangeUInt32UpperOverflow
import io.spine.validation.RangeUInt64LowerOverflow
import io.spine.validation.RangeUInt64MinMax
import io.spine.validation.RangeUInt64UpperOverflow
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.provider.Arguments

/**
 * Provides data for parametrized tests in `RangeReactionSpec`.
 */
@Suppress("unused") // Data provider for parameterized test.
object RangeReactionTestEnv {

    /**
     * Test data for `RangeReactionSpec.whenFieldHasUnsupportedType()`.
     */
    @JvmStatic
    fun messagesWithUnsupportedFieldType() = listOf(
        "string" to RangeOnString::class,
        "bool" to RangeOnBool::class,
        "bytes" to RangeOnBytes::class,
        "message" to RangeOnMessage::class,
        "enum" to RangeOnEnum::class,
        "repeated of string" to RangeOnStringRepeated::class,
        "repeated of bool" to RangeOnBoolRepeated::class,
        "repeated of bytes" to RangeOnBytesRepeated::class,
        "repeated of message" to RangeOnMessageRepeated::class,
        "repeated of enum" to RangeOnEnumRepeated::class,
        "map of int32" to RangeOnIntMap::class,
        "map of double" to RangeOnDoubleMap::class,
        "map of string" to RangeOnStringMap::class,
    ).map { Arguments.arguments(Named.named(it.first, it.second)) }

    /**
     * Test data for `RangeReactionSpec.withInvalidDelimiters()`.
     */
    @JvmStatic
    fun messagesWithInvalidDelimiters() = listOf(
        RangeInvalidDelimiter1::class,
        RangeInvalidDelimiter2::class,
        RangeInvalidDelimiter3::class,
        RangeInvalidDelimiter4::class,
    ).map { Arguments.arguments(it) }

    /**
     * Test data for `RangeReactionSpec.withOverflowValue()`.
     */
    @JvmStatic
    fun messagesWithOverflowValues() = listOf(
        RangeFloatLowerOverflow::class to "-3.5028235E38",
        RangeFloatUpperOverflow::class to "3.5028235E38",
        RangeDoubleLowerOverflow::class to "-1.8976931348623157E308",
        RangeDoubleUpperOverflow::class to "1.8976931348623157E308",
        RangeInt32LowerOverflow::class to "-2147483649",
        RangeInt32UpperOverflow::class to "2147483648",
        RangeInt64LowerOverflow::class to "-9223372036854775809",
        RangeInt64UpperOverflow::class to "9223372036854775808",
        RangeUInt32LowerOverflow::class to "-1",
        RangeUInt32UpperOverflow::class to "4294967296",
        RangeUInt64LowerOverflow::class to "-1",
        RangeUInt64UpperOverflow::class to "18446744073709551616",
        RangeSInt32LowerOverflow::class to "-2147483649",
        RangeSInt32UpperOverflow::class to "2147483648",
        RangeSInt64LowerOverflow::class to "-9223372036854775809",
        RangeSInt64UpperOverflow::class to "9223372036854775808",
        RangeFixed32LowerOverflow::class to "-1",
        RangeFixed32UpperOverflow::class to "4294967296",
        RangeFixed64LowerOverflow::class to "-1",
        RangeFixed64UpperOverflow::class to "18446744073709551616",
        RangeSFixed32LowerOverflow::class to "-2147483649",
        RangeSFixed32UpperOverflow::class to "2147483648",
        RangeSFixed64LowerOverflow::class to "-9223372036854775809",
        RangeSFixed64UpperOverflow::class to "9223372036854775808",
    ).map { Arguments.arguments(it.first, it.second) }

    /**
     * Test data for `RangeReactionSpec.withLowerEqualOrMoreThanUpper()`.
     */
    @JvmStatic
    fun messagesWithLowerEqualOrMoreThanUpper() = listOf(
        RangeFloatMinMax::class to "5.5",
        RangeDoubleMinMax::class to "-5.5",
        RangeInt32MinMax::class to "5",
        RangeInt64MinMax::class to "-5",
        RangeUInt32MinMax::class to "8",
        RangeUInt64MinMax::class to "8",
        RangeSInt32MinMax::class to "5",
        RangeSInt64MinMax::class to "5",
        RangeFixed32MinMax::class to "8",
        RangeFixed64MinMax::class to "8",
        RangeSFixed32MinMax::class to "5",
        RangeSFixed64MinMax::class to "5",
    ).map { Arguments.arguments(it.first, it.second) }
}
