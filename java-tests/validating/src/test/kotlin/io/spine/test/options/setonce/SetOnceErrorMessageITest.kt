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
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.protobuf.field
import io.spine.test.tools.validate.StudentCustomMessage
import io.spine.test.tools.validate.StudentDefaultMessage
import io.spine.test.tools.validate.YearOfStudy
import io.spine.validate.ErrorPlaceholder.FIELD_PATH
import io.spine.validate.ErrorPlaceholder.FIELD_PROPOSED_VALUE
import io.spine.validate.ErrorPlaceholder.FIELD_TYPE
import io.spine.validate.ErrorPlaceholder.FIELD_VALUE
import io.spine.validate.ErrorPlaceholder.PARENT_TYPE
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`(set_once)` constraint should")
internal class SetOnceErrorMessageITest {

    @Suppress("MaxLineLength") // Long method source.
    @MethodSource("io.spine.test.options.setonce.given.SetOnceErrorMessageTestEnv#allFieldTypesWithTwoDistinctValues")
    @ParameterizedTest(name = "show the default error message for `{0}` field")
    fun <T : Any> defaultErrorMessage(fieldName: String, fieldType: String, value1: T, value2: T) =
        assertDefault(fieldName, fieldType, value1, value2)

    @Suppress("MaxLineLength") // Long method source.
    @MethodSource("io.spine.test.options.setonce.given.SetOnceErrorMessageTestEnv#allFieldTypesWithTwoDistinctValues")
    @ParameterizedTest(name = "show the custom error message for `{0}` field")
    fun <T : Any> customErrorMessage(fieldName: String, fieldType: String, value1: T, value2: T) =
        assertCustom(fieldName, fieldType, value1, value2)
}

private fun <T : Any> assertDefault(fieldName: String, fieldType: String, value1: T, value2: T) {
    val builder = StudentDefaultMessage.newBuilder()
    val template = { _: Int -> DEFAULT_MESSAGE }
    return builder.assertErrorMessage(template, fieldName, fieldType, value1, value2)
}

private fun <T : Any> assertCustom(fieldName: String, fieldType: String, value1: T, value2: T) {
    val builder = StudentCustomMessage.newBuilder()
    val template = ::customErrorMessage
    return builder.assertErrorMessage(template, fieldName, fieldType, value1, value2)
}

/**
 * Asserts that this message [Builder] throws [ValidationException] with
 * the expected parameters when [fieldName] is set twice.
 *
 * Notice on enum fields: we have to pass enums as value descriptors
 * (see [io.spine.test.options.setonce.given.SetOnceErrorMessageTestEnv.allFieldTypesWithTwoDistinctValues]),
 * so we also have to take this into account during assertions because
 * in `ConstraintViolation` they still arrive as Java enum constants.
 *
 * @param template The error message template to check.
 * @param fieldName The field to set.
 * @param value1 The first field value to set.
 * @param value2 The second field value to set for triggering the exception.
 */
private fun <T : Any> Builder.assertErrorMessage(
    template: (Int) -> String,
    fieldName: String,
    fieldType: String,
    value1: T,
    value2: T,
) {
    check(value1 != value2)

    val field = descriptorForType.field(fieldName)!!
    val parentType = descriptorForType.fullName
    val exception = assertThrows<ValidationException> {
        setField(field, value1)
        setField(field, value2)
    }

    val violations = exception.constraintViolations.also { it.size shouldBe 1 }
    val violation = violations.first()

    with(violation) {
        message.withPlaceholders shouldBe template(field.index + 1)
        message.placeholderValueMap shouldContainExactly mapOf(
            FIELD_PATH to fieldName,
            FIELD_TYPE to fieldType,
            FIELD_VALUE to "$value1",
            FIELD_PROPOSED_VALUE to "$value2",
            PARENT_TYPE to parentType
        ).mapKeys { it.key.toString() }

        fieldPath shouldBe FieldPath(fieldName)
        typeName shouldBe parentType

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

private const val DEFAULT_MESSAGE =
    "The field `\${parent.type}.\${field.path}` of the type `\${field.type}` already has " +
            "the value `\${field.value}` and cannot be reassigned to `\${field.proposed_value}`."

private fun customErrorMessage(fieldNumber: Int) =
    "Field_$fieldNumber: " +
            "`\${field.value}`, `\${field.path}`, `\${field.proposed_value}`, `\${field.type}`."
