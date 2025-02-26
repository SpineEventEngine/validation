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

import com.google.protobuf.Message.Builder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.protobuf.field
import io.spine.test.options.set
import io.spine.test.tools.validate.StudentCustomMessage
import io.spine.test.tools.validate.StudentDefaultMessage
import io.spine.validate.RuntimeErrorPlaceholder.FIELD_PATH
import io.spine.validate.RuntimeErrorPlaceholder.FIELD_PROPOSED_VALUE
import io.spine.validate.RuntimeErrorPlaceholder.FIELD_TYPE
import io.spine.validate.RuntimeErrorPlaceholder.FIELD_VALUE
import io.spine.validate.RuntimeErrorPlaceholder.PARENT_TYPE
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests [ConstraintViolation][io.spine.validate.ConstraintViolation]s created by `(set_once)`.
 */
@DisplayName("`(set_once)` constraint should")
internal class SetOnceViolationITest {

    @Suppress("MaxLineLength") // Long method source.
    @MethodSource("io.spine.test.options.setonce.SetOnceViolationTestEnv#allFieldTypesWithTwoDistinctValues")
    @ParameterizedTest(name = "use the default error message for `{0}` field")
    fun <T : Any> useDefaultErrorMessage(fieldName: String, fieldType: String, value1: T, value2: T) =
        StudentDefaultMessage.newBuilder()
            .assertConstraintViolation(fieldName, fieldType, value1, value2, ::defaultTemplate)

    @Suppress("MaxLineLength") // Long method source.
    @MethodSource("io.spine.test.options.setonce.SetOnceViolationTestEnv#allFieldTypesWithTwoDistinctValues")
    @ParameterizedTest(name = "use the custom error message for `{0}` field")
    fun <T : Any> useCustomErrorMessage(fieldName: String, fieldType: String, value1: T, value2: T) =
        StudentCustomMessage.newBuilder()
            .assertConstraintViolation(fieldName, fieldType, value1, value2, ::customTemplate)
}

/**
 * Asserts that this message [Builder] throws [ValidationException] with
 * the expected parameters when [fieldName] is set twice.
 *
 * @param fieldName The name of the field.
 * @param fieldType The name of the field type.
 * @param fieldValue1 The first field value to set.
 * @param fieldValue2 The second field value to set.
 * @param template The error message template to check.
 */
private fun <T : Any> Builder.assertConstraintViolation(
    fieldName: String,
    fieldType: String,
    fieldValue1: T,
    fieldValue2: T,
    template: (Int) -> String,
) {
    check(fieldValue1 != fieldValue2)

    val field = descriptorForType.field(fieldName)!!
    val parentType = descriptorForType.fullName
    val exception = assertThrows<ValidationException> {
        set(field, fieldValue1)
        set(field, fieldValue2)
    }

    val violations = exception.constraintViolations.also { it.size shouldBe 1 }
    val violation = violations.first()

    with(violation) {
        message.withPlaceholders shouldBe template(field.index + 1)
        message.placeholderValueMap shouldContainExactly mapOf(
            FIELD_PATH to fieldName,
            FIELD_TYPE to fieldType,
            FIELD_VALUE to "$fieldValue1",
            FIELD_PROPOSED_VALUE to "$fieldValue2",
            PARENT_TYPE to parentType
        ).mapKeys { it.key.toString() }

        typeName shouldBe parentType
        fieldPath shouldBe FieldPath(fieldName)
        fieldValue shouldBe toAny(fieldValue2)
    }
}

@Suppress("UNUSED_PARAMETER") // The function should match the expected interface.
private fun defaultTemplate(fieldNumber: Int) =
    "The field `\${parent.type}.\${field.path}` of the type `\${field.type}` already has" +
            " the value `\${field.value}` and cannot be reassigned to `\${field.proposed_value}`."

private fun customTemplate(fieldNumber: Int) =
    "Field_$fieldNumber:" +
            " `\${field.value}`, `\${field.path}`, `\${field.proposed_value}`, `\${field.type}`."
