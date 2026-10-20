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

package io.spine.test.options.goes

import com.google.protobuf.Message.Builder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.protobuf.field
import io.spine.test.options.set
import io.spine.test.options.asPlaceholderValue
import io.spine.test.tools.validate.GoesCustomMessage
import io.spine.test.tools.validate.GoesDefaultMessage
import io.spine.validation.StandardPlaceholder.FIELD_PATH
import io.spine.validation.StandardPlaceholder.FIELD_TYPE
import io.spine.validation.StandardPlaceholder.FIELD_VALUE
import io.spine.validation.StandardPlaceholder.GOES_COMPANION
import io.spine.validation.StandardPlaceholder.PARENT_TYPE
import io.spine.validation.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests [ConstraintViolation][io.spine.validation.ConstraintViolation]s created by `(goes)`.
 */
@DisplayName("`(goes)` constraint should")
internal class GoesViolationIgTest {

    @MethodSource("io.spine.test.options.goes.GoesViolationTestEnv#onlyTargetFields")
    @ParameterizedTest(name = "use the default error message for `{0}` field")
    fun useDefaultErrorMessage(fieldName: String, fieldType: String, fieldValue: Any) =
        GoesDefaultMessage.newBuilder()
            .assertConstraintViolation(fieldName, fieldType, fieldValue, ::defaultTemplate)

    @MethodSource("io.spine.test.options.goes.GoesViolationTestEnv#onlyTargetFields")
    @ParameterizedTest(name = "use the custom error message for `{0}` field")
    fun useCustomErrorMessage(fieldName: String, fieldType: String, fieldValue: Any) =
        GoesCustomMessage.newBuilder()
            .assertConstraintViolation(fieldName, fieldType, fieldValue, ::customTemplate)
}

/**
 * Asserts that this message [Builder] throws [ValidationException] with
 * the expected parameters when [fieldName] is set without its [COMPANION_FIELD].
 *
 * @param fieldName The name of the field.
 * @param fieldType The name of the field type.
 * @param value The field value to set.
 * @param template The error message template to check.
 */
private fun Builder.assertConstraintViolation(
    fieldName: String,
    fieldType: String,
    value: Any,
    template: (Int) -> String
) {
    val field = descriptorForType.field(fieldName)!!
    val parentType = descriptorForType.fullName
    val exception = assertThrows<ValidationException> {
        set(field, value)
            .build()
    }

    val violations = exception.constraintViolations.also { it.size shouldBe 1 }
    val violation = violations.first()

    with(violation) {
        message.withPlaceholders shouldBe template(field.index + 1)
        message.placeholderValueMap shouldContainExactly mapOf(
            FIELD_PATH.value to fieldName,
            FIELD_VALUE.value to value.asPlaceholderValue(),
            FIELD_TYPE.value to fieldType,
            PARENT_TYPE.value to parentType,
            GOES_COMPANION.value to COMPANION_FIELD,
        ).mapKeys { it.key.toString() }

        typeName shouldBe parentType
        fieldPath shouldBe FieldPath(fieldName)
        fieldValue shouldBe toAny(value)
    }
}

@Suppress("UNUSED_PARAMETER") // The function should match the expected interface.
private fun defaultTemplate(fieldNumber: Int) =
    "The field `\${goes.companion}` must also be set when `\${field.path}`" +
            " is set in `\${parent.type}`."

private fun customTemplate(fieldNumber: Int) =
    "Field_$fieldNumber: `{companionName}`, `{fieldName}`."
