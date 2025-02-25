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

import io.spine.test.options.setonce.SetOnceTestEnv.CERT1
import io.spine.test.options.setonce.SetOnceTestEnv.CERT2
import io.spine.test.options.setonce.SetOnceTestEnv.DONALD
import io.spine.test.options.setonce.SetOnceTestEnv.EIGHT
import io.spine.test.options.setonce.SetOnceTestEnv.EIGHTY_KG
import io.spine.test.options.setonce.SetOnceTestEnv.FIFTY_KG
import io.spine.test.options.setonce.SetOnceTestEnv.FIRST_YEAR
import io.spine.test.options.setonce.SetOnceTestEnv.JACK
import io.spine.test.options.setonce.SetOnceTestEnv.NO
import io.spine.test.options.setonce.SetOnceTestEnv.SEVENTY
import io.spine.test.options.setonce.SetOnceTestEnv.SHORT_HEIGHT
import io.spine.test.options.setonce.SetOnceTestEnv.TALL_HEIGHT
import io.spine.test.options.setonce.SetOnceTestEnv.THIRD_YEAR
import io.spine.test.options.setonce.SetOnceTestEnv.TWENTY
import io.spine.test.options.setonce.SetOnceTestEnv.TWO
import io.spine.test.options.setonce.SetOnceTestEnv.YES
import io.spine.test.tools.validate.Name
import io.spine.test.tools.validate.YearOfStudy
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parametrized [io.spine.test.options.setonce.SetOnceViolationITest].
 */
internal object SetOnceViolationTestEnv {

    /**
     * Test data for the following tests:
     *
     * 1. [io.spine.test.options.setonce.SetOnceViolationITest.useDefaultErrorMessage].
     * 2. [io.spine.test.options.setonce.SetOnceViolationITest.useCustomErrorMessage].
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
        arguments(
            named("enum", "year_of_study"),
            YearOfStudy.getDescriptor().fullName, // A specific enum type name is expected.
            FIRST_YEAR,
            THIRD_YEAR
        ),
        arguments(
            named("message", "name"),
            Name.getDescriptor().fullName, // A specific message type name is expected.
            JACK,
            DONALD
        )
    )

    private fun primitiveFieldsAndTwoDistinctValues() = listOf(
        Primitive("double", "height", SHORT_HEIGHT, TALL_HEIGHT),
        Primitive("float", "weight", FIFTY_KG, EIGHTY_KG),
        Primitive("int32", "cash_USD", TWO, EIGHT),
        Primitive("int64", "cash_EUR", TWENTY, SEVENTY),
        Primitive("uint32", "cash_JPY", TWO, EIGHT),
        Primitive("uint64", "cash_GBP", TWENTY, SEVENTY),
        Primitive("sint32", "cash_AUD", TWO, EIGHT),
        Primitive("sint64", "cash_CAD", TWENTY, SEVENTY),
        Primitive("fixed32", "cash_CHF", TWO, EIGHT),
        Primitive("fixed64", "cash_CNY", TWENTY, SEVENTY),
        Primitive("sfixed32", "cash_PLN", TWO, EIGHT),
        Primitive("sfixed64", "cash_NZD", TWENTY, SEVENTY),
        Primitive("bool", "has_medals", YES, NO),
        Primitive("bytes", "signature", CERT1, CERT2),
    ).map { arguments(named(it.type, it.field), it.type, it.value1, it.value2) }
}

private class Primitive(
    val type: String,
    val field: String,
    val value1: Any,
    val value2: Any,
)
