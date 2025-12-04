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

package io.spine.validation

import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parametrized tests in [io.spine.validation.RangeReactionSpec].
 */
@Suppress("unused") // Data provider for parameterized test.
object RangeReactionTestEnv {

    /**
     * Test data for [io.spine.validation.RangeReactionSpec.whenFieldHasUnsupportedType].
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
    ).map { arguments(named(it.first, it.second)) }

    /**
     * Test data for [io.spine.validation.RangeReactionSpec.withInvalidDelimiters].
     */
    @JvmStatic
    fun messagesWithInvalidDelimiters() = listOf(
        RangeInvalidDelimiter1::class,
        RangeInvalidDelimiter2::class,
        RangeInvalidDelimiter3::class,
        RangeInvalidDelimiter4::class,
    ).map { arguments(it) }

    /**
     * Test data for [io.spine.validation.RangeReactionSpec.withOverflowValue].
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
    ).map { arguments(it.first, it.second) }

    /**
     * Test data for [io.spine.validation.RangeReactionSpec.withLowerEqualOrMoreThanUpper].
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
    ).map { arguments(it.first, it.second) }
}
