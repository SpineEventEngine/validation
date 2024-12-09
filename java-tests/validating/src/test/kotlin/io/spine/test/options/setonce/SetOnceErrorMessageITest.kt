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

package io.spine.test.options.setonce

import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Message.Builder
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.protobuf.field
import io.spine.test.options.setonce.TestEnv.CERT1
import io.spine.test.options.setonce.TestEnv.CERT2
import io.spine.test.options.setonce.TestEnv.DONALD
import io.spine.test.options.setonce.TestEnv.TWENTY
import io.spine.test.options.setonce.TestEnv.SEVENTY
import io.spine.test.options.setonce.TestEnv.EIGHTY_KG
import io.spine.test.options.setonce.TestEnv.FIFTY_KG
import io.spine.test.options.setonce.TestEnv.FIRST_YEAR
import io.spine.test.options.setonce.TestEnv.JACK
import io.spine.test.options.setonce.TestEnv.NO
import io.spine.test.options.setonce.TestEnv.SHORT_HEIGHT
import io.spine.test.options.setonce.TestEnv.TWO
import io.spine.test.options.setonce.TestEnv.EIGHT
import io.spine.test.options.setonce.TestEnv.STUDENT1
import io.spine.test.options.setonce.TestEnv.STUDENT2
import io.spine.test.options.setonce.TestEnv.TALL_HEIGHT
import io.spine.test.options.setonce.TestEnv.THIRD_YEAR
import io.spine.test.options.setonce.TestEnv.YES
import io.spine.test.tools.validate.StudentCustomMessage
import io.spine.test.tools.validate.StudentDefaultMessage
import io.spine.test.tools.validate.YearOfStudy
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`(set_once)` constraint should")
internal class SetOnceErrorMessageITest {

    @MethodSource("allFieldTypesWithTwoDistinctValues")
    @ParameterizedTest(name = "show the default error message for `{0}` field")
    fun <T : Any> defaultErrorMessage(fieldName: String, value1: T, value2: T) =
        assertDefaultMessage(fieldName, value1, value2)

    @MethodSource("allFieldTypesWithTwoDistinctValues")
    @ParameterizedTest(name = "show the custom error message for `{0}` field")
    fun <T : Any> customErrorMessage(fieldName: String, value1: T, value2: T) =
        assertCustomMessage(fieldName, value1, value2)

    private companion object {

        @JvmStatic
        fun allFieldTypesWithTwoDistinctValues() = listOf(
            arguments(named("message", "name"), JACK, DONALD),
            arguments(named("string", "id"), STUDENT1, STUDENT2),
            arguments(named("double", "height"), SHORT_HEIGHT, TALL_HEIGHT),
            arguments(named("float", "weight"), FIFTY_KG, EIGHTY_KG),
            arguments(named("int32", "cash_USD"), TWO, EIGHT),
            arguments(named("int64", "cash_EUR"), TWENTY, SEVENTY),
            arguments(named("uint32", "cash_JPY"), TWO, EIGHT),
            arguments(named("uint64", "cash_GBP"), TWENTY, SEVENTY),
            arguments(named("sint32", "cash_AUD"), TWO, EIGHT),
            arguments(named("sint64", "cash_CAD"), TWENTY, SEVENTY),
            arguments(named("fixed32", "cash_CHF"), TWO, EIGHT),
            arguments(named("fixed64", "cash_CNY"), TWENTY, SEVENTY),
            arguments(named("sfixed32", "cash_PLN"), TWO, EIGHT),
            arguments(named("sfixed64", "cash_NZD"), TWENTY, SEVENTY),
            arguments(named("bool", "has_medals"), YES, NO),
            arguments(named("enum", "signature"), CERT1, CERT2),

            // For some reason, for enums, `Message.Builder.setField()` expects value
            // descriptors instead of constants or their ordinal numbers.
            arguments("year_of_study", FIRST_YEAR.valueDescriptor, THIRD_YEAR.valueDescriptor)
        )
    }
}

private fun <T : Any> assertDefaultMessage(fieldName: String, value1: T, value2: T) {
    val builder = StudentDefaultMessage.newBuilder()
    val expectedParams = listOf(fieldName, "$value1", "$value2")
    val expectedFormat = { _: Int -> DEFAULT_MESSAGE_FORMAT }
    return builder.assertErrorMessage(fieldName, value1, value2, expectedParams, expectedFormat)
}

private fun <T : Any> assertCustomMessage(fieldName: String, value1: T, value2: T) {
    val builder = StudentCustomMessage.newBuilder()
    val expectedParams = listOf("$value1", fieldName, "$value2")
    val expectedFormat = ::customMessageFormat
    return builder.assertErrorMessage(fieldName, value1, value2, expectedParams, expectedFormat)
}

/**
 * Asserts that this message [Builder] throws [ValidationException] with
 * the expected parameters when [fieldName] is set twice.
 *
 * Notice on enum fields: we have to pass enums as value descriptors
 * (see [SetOnceErrorMessageITest.allFieldTypesWithTwoDistinctValues]),
 * so we also have to take this into account during assertions because
 * in `ConstraintViolation` they still arrive as Java enum constants.
 *
 * @param fieldName The field to set.
 * @param value1 The first field value to set.
 * @param value2 The second field value to set for triggering the exception.
 * @param expectedParams The list of params to check upon `ConstraintViolation.param`.
 * @param expectedFormat The format string to check upon `ConstraintViolation.msg_format`.
 */
private fun <T : Any> Builder.assertErrorMessage(
    fieldName: String,
    value1: T,
    value2: T,
    expectedParams: List<String>,
    expectedFormat: (Int) -> String
) {
    check(value1 != value2)

    val field = descriptorForType.field(fieldName)!!
    val exception = assertThrows<ValidationException> {
        setField(field, value1)
        setField(field, value2)
    }

    val violations = exception.constraintViolations.also { it.size shouldBe 1 }
    val violation = violations.first()
    with(violation) {
        msgFormat shouldBe expectedFormat(field.index + 1)
        paramList shouldBe expectedParams
        fieldPath shouldBe FieldPath(fieldName)
        typeName shouldBe  this@assertErrorMessage.descriptorForType.fullName

        // Enums are a bit special. See the method docs for details.
        if (value2 is EnumValueDescriptor) {
            // Any enum in `(set_once)` tests is `YearOfStudy`, so it is safe.
            val enumConstant = YearOfStudy.forNumber(value2.number)
            fieldValue shouldBe toAny(enumConstant)
        } else {
            fieldValue shouldBe toAny(value2)
        }
    }
}

private const val DEFAULT_MESSAGE_FORMAT =
    "The field `%s` already has the value `%s` and cannot be reassigned to `%s`."

private fun customMessageFormat(fieldNumber: Int) =
    "Field_$fieldNumber: `%s`, `%s`, `%s`."
