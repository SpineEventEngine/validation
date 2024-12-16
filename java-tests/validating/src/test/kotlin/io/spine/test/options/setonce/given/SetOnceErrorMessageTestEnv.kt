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

package io.spine.test.options.setonce.given

import io.spine.test.tools.validate.Name
import io.spine.test.tools.validate.YearOfStudy
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parametrized [io.spine.test.options.setonce.SetOnceErrorMessageITest].
 */
internal object SetOnceErrorMessageTestEnv {

    @JvmStatic
    fun allFieldTypesWithTwoDistinctValues() = listOf(
        // For some reason, for enums, `Message.Builder.setField()` expects value
        // descriptors instead of constants or their ordinal numbers.
        arguments(
            named("enum", "year_of_study"),
            TestEnv.FIRST_YEAR.valueDescriptor, TestEnv.THIRD_YEAR.valueDescriptor,
            YearOfStudy.getDescriptor().fullName // Expected a specific enum type.
        ),
        arguments(
            named("message", "name"),
            TestEnv.JACK, TestEnv.DONALD,
            Name.getDescriptor().fullName // Expected a specific message type.
        )
    ) + primitiveFieldTypesWithTwoDistinctValues().map {
        arguments(named(it.type, it.field), it.value1, it.value2, it.type)
    }

    private fun primitiveFieldTypesWithTwoDistinctValues() = listOf(
        Primitive("double", "height", TestEnv.SHORT_HEIGHT, TestEnv.TALL_HEIGHT),
        Primitive("float", "weight", TestEnv.FIFTY_KG, TestEnv.EIGHTY_KG),
        Primitive("int32", "cash_USD", TestEnv.TWO, TestEnv.EIGHT),
        Primitive("int64", "cash_EUR", TestEnv.TWENTY, TestEnv.SEVENTY),
        Primitive("uint32", "cash_JPY", TestEnv.TWO, TestEnv.EIGHT),
        Primitive("uint64", "cash_GBP", TestEnv.TWENTY, TestEnv.SEVENTY),
        Primitive("sint32", "cash_AUD", TestEnv.TWO, TestEnv.EIGHT),
        Primitive("sint64", "cash_CAD", TestEnv.TWENTY, TestEnv.SEVENTY),
        Primitive("fixed32", "cash_CHF", TestEnv.TWO, TestEnv.EIGHT),
        Primitive("fixed64", "cash_CNY", TestEnv.TWENTY, TestEnv.SEVENTY),
        Primitive("sfixed32", "cash_PLN", TestEnv.TWO, TestEnv.EIGHT),
        Primitive("sfixed64", "cash_NZD", TestEnv.TWENTY, TestEnv.SEVENTY),
        Primitive("bool", "has_medals", TestEnv.YES, TestEnv.NO),
        Primitive("bytes", "signature", TestEnv.CERT1, TestEnv.CERT2),
    )
}

private class Primitive(
    val type: String,
    val field: String,
    val value1: Any,
    val value2: Any,
)
