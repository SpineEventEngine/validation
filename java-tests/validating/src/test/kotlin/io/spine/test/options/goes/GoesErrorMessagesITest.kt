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

package io.spine.test.options.goes

import com.google.protobuf.Message.Builder
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.test.options.goes.given.GoesMessagesTestEnv.COMPANION_FIELD_NAME
import io.spine.test.options.goes.given.protoValue
import io.spine.test.tools.validate.GoesCustomMessage
import io.spine.test.tools.validate.GoesDefaultMessage
import io.spine.type.TypeUrl
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import com.google.protobuf.Any as ProtobufAny

/**
 * Tests error messages of `(goes)` option.
 */
@DisplayName("`(goes)` constraint should")
internal class GoesErrorMessagesITest {

    @MethodSource("io.spine.test.options.goes.given.GoesMessagesTestEnv#onlyTargetFields")
    @ParameterizedTest(name = "show the default error message for `{0}` field")
    fun showDefaultErrorMessage(fieldName: String, fieldValue: Any) =
        GoesDefaultMessage.newBuilder()
            .assertErrorMessage(
                fieldName,
                fieldValue,
                listOf(fieldName, COMPANION_FIELD_NAME)
            ) { _: Int ->
                DEFAULT_MESSAGE_FORMAT
            }

    @MethodSource("io.spine.test.options.goes.given.GoesMessagesTestEnv#onlyTargetFields")
    @ParameterizedTest(name = "show the custom error message for `{0}` field")
    fun showCustomErrorMessage(fieldName: String, fieldValue: Any) = GoesCustomMessage.newBuilder()
        .assertErrorMessage(
            fieldName,
            fieldValue,
            listOf(COMPANION_FIELD_NAME, fieldName),
            ::customMessageFormat
        )
}

private fun Builder.assertErrorMessage(
    fieldName: String,
    fieldValue: Any,
    expectedParams: List<String>,
    expectedFormat: (Int) -> String
) {
    val descriptor = descriptorForType
    val field = descriptor.findFieldByName(fieldName)!!
    val protoValue = protoValue(field, fieldValue)
    val exception = assertThrows<ValidationException> {
        setField(field, protoValue)
            .build()
    }

    val violations = exception.constraintViolations.also { it.size shouldBe 1 }
    val violation = violations.first()
    with(violation) {
        msgFormat shouldBe expectedFormat(field.index + 1)
        paramList shouldBe expectedParams
        typeName shouldBe "${TypeUrl.from(descriptor)}"
        fieldPath shouldBe FieldPath(fieldName)
        this.fieldValue shouldBe ProtobufAny.getDefaultInstance()
        violationList.shouldBeEmpty()
    }
}

private const val DEFAULT_MESSAGE_FORMAT =
    "The field `%s` can only be set when `%s` field is defined."

private fun customMessageFormat(fieldNumber: Int) = "Field_$fieldNumber: `%s`, `%s`."
