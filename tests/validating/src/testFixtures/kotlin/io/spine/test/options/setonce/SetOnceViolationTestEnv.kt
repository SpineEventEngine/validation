/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
 * Provides data for parametrized [io.spine.test.options.setonce.SetOnceViolationIgTest].
 */
@Suppress("unused") // Data provider for parameterized test.
internal object SetOnceViolationTestEnv {

    /**
     * Test data for the following tests:
     *
     * 1. [io.spine.test.options.setonce.SetOnceViolationIgTest.useDefaultErrorMessage].
     * 2. [io.spine.test.options.setonce.SetOnceViolationIgTest.useCustomErrorMessage].
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

private data class Primitive(
    val type: String,
    val field: String,
    val value1: Any,
    val value2: Any,
)
