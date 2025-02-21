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

package io.spine.test.options.setonce

import io.spine.test.tools.validate.Name
import io.spine.test.tools.validate.YearOfStudy
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.provider.Arguments

/**
 * Provides data for parametrized [io.spine.test.options.setonce.SetOnceErrorMessageITest].
 */
internal object SetOnceErrorMessageTestEnv {

    /**
     * Test data for the following tests:
     *
     * 1. [io.spine.test.options.setonce.SetOnceErrorMessageITest.defaultErrorMessage].
     * 2. [io.spine.test.options.setonce.SetOnceErrorMessageITest.customErrorMessage].
     */
    @JvmStatic
    fun allFieldTypesWithTwoDistinctValues() =
        primitiveFieldsAndTwoDistinctValues() + messageFieldsAndTwoDistinctValues()

    /**
     * Returns the test data for `message` and `enum` field types.
     *
     * Arguments for these field types are created separately because the name
     * of the field type under the test differs from the actual field type name.
     * For messages and enums, the actual field type name is not `message` or `enum`,
     * but names of their corresponding messages like [YearOfStudy] or [Name].
     */
    private fun messageFieldsAndTwoDistinctValues() = listOf(
        Arguments.arguments(
            Named.named("enum", "year_of_study"),
            YearOfStudy.getDescriptor().fullName, // A specific enum type name is expected.
            SetOnceTestEnv.FIRST_YEAR,
            SetOnceTestEnv.THIRD_YEAR
        ),
        Arguments.arguments(
            Named.named("message", "name"),
            Name.getDescriptor().fullName, // A specific message type name is expected.
            SetOnceTestEnv.JACK,
            SetOnceTestEnv.DONALD
        )
    )

    private fun primitiveFieldsAndTwoDistinctValues() = listOf(
        Primitive("double", "height", SetOnceTestEnv.SHORT_HEIGHT, SetOnceTestEnv.TALL_HEIGHT),
        Primitive("float", "weight", SetOnceTestEnv.FIFTY_KG, SetOnceTestEnv.EIGHTY_KG),
        Primitive("int32", "cash_USD", SetOnceTestEnv.TWO, SetOnceTestEnv.EIGHT),
        Primitive("int64", "cash_EUR", SetOnceTestEnv.TWENTY, SetOnceTestEnv.SEVENTY),
        Primitive("uint32", "cash_JPY", SetOnceTestEnv.TWO, SetOnceTestEnv.EIGHT),
        Primitive("uint64", "cash_GBP", SetOnceTestEnv.TWENTY, SetOnceTestEnv.SEVENTY),
        Primitive("sint32", "cash_AUD", SetOnceTestEnv.TWO, SetOnceTestEnv.EIGHT),
        Primitive("sint64", "cash_CAD", SetOnceTestEnv.TWENTY, SetOnceTestEnv.SEVENTY),
        Primitive("fixed32", "cash_CHF", SetOnceTestEnv.TWO, SetOnceTestEnv.EIGHT),
        Primitive("fixed64", "cash_CNY", SetOnceTestEnv.TWENTY, SetOnceTestEnv.SEVENTY),
        Primitive("sfixed32", "cash_PLN", SetOnceTestEnv.TWO, SetOnceTestEnv.EIGHT),
        Primitive("sfixed64", "cash_NZD", SetOnceTestEnv.TWENTY, SetOnceTestEnv.SEVENTY),
        Primitive("bool", "has_medals", SetOnceTestEnv.YES, SetOnceTestEnv.NO),
        Primitive("bytes", "signature", SetOnceTestEnv.CERT1, SetOnceTestEnv.CERT2),
    ).map { Arguments.arguments(Named.named(it.type, it.field), it.type, it.value1, it.value2) }
}

private class Primitive(
    val type: String,
    val field: String,
    val value1: Any,
    val value2: Any,
)
