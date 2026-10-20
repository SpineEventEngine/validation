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

import com.google.protobuf.Message.Builder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.protobuf.field
import io.spine.test.options.set
import io.spine.test.options.asPlaceholderValue
import io.spine.test.tools.validate.StudentCustomMessage
import io.spine.test.tools.validate.StudentDefaultMessage
import io.spine.validation.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import io.spine.validation.StandardPlaceholder.FIELD_PATH
import io.spine.validation.StandardPlaceholder.FIELD_PROPOSED_VALUE
import io.spine.validation.StandardPlaceholder.FIELD_TYPE
import io.spine.validation.StandardPlaceholder.FIELD_VALUE
import io.spine.validation.StandardPlaceholder.PARENT_TYPE

/**
 * Tests [ConstraintViolation][io.spine.validation.ConstraintViolation]s created by `(set_once)`.
 */
@DisplayName("`(set_once)` constraint should")
internal class SetOnceViolationIgTest {

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
            FIELD_PATH.value to fieldName,
            FIELD_TYPE.value to fieldType,
            FIELD_VALUE.value to fieldValue1.asPlaceholderValue(),
            FIELD_PROPOSED_VALUE.value to fieldValue2.asPlaceholderValue(),
            PARENT_TYPE.value to parentType
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
