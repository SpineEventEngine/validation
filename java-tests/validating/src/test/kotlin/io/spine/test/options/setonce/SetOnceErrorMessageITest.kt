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

import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps.now
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.protobuf.field
import io.spine.test.options.setonce.TestEnv.EIGHTEEN
import io.spine.test.options.setonce.TestEnv.EIGHTY
import io.spine.test.options.setonce.TestEnv.EIGHTY_KG
import io.spine.test.options.setonce.TestEnv.FIFTY_KG
import io.spine.test.options.setonce.TestEnv.FULL_SIGNATURE
import io.spine.test.options.setonce.TestEnv.METER_AND_EIGHT
import io.spine.test.options.setonce.TestEnv.METER_AND_HALF
import io.spine.test.options.setonce.TestEnv.NO
import io.spine.test.options.setonce.TestEnv.SHORT_SIGNATURE
import io.spine.test.options.setonce.TestEnv.SIXTEEN
import io.spine.test.options.setonce.TestEnv.SIXTY
import io.spine.test.options.setonce.TestEnv.STUDENT1
import io.spine.test.options.setonce.TestEnv.STUDENT2
import io.spine.test.options.setonce.TestEnv.YES
import io.spine.test.tools.validate.Accommodation.ACM_APARTMENT
import io.spine.test.tools.validate.Accommodation.ACM_HOSTEL
import io.spine.test.tools.validate.SetOnceCustomErrorMsg
import io.spine.test.tools.validate.SetOnceDefaultErrorMsg
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`(set_once)` should show")
internal class SetOnceErrorMessageITest {

    companion object {

        @JvmStatic
        fun fields() = listOf(
            arguments("message", now(), now()),
            arguments("string", STUDENT1, STUDENT2),
            arguments("double", METER_AND_HALF, METER_AND_EIGHT),
            arguments("float", FIFTY_KG, EIGHTY_KG),
            arguments("int32", SIXTEEN, SIXTY),
            arguments("int64", EIGHTEEN, EIGHTY),
            arguments("uint32", SIXTEEN, SIXTY),
            arguments("uint64", EIGHTEEN, EIGHTY),
            arguments("sint32", SIXTEEN, SIXTY),
            arguments("sint64", EIGHTEEN, EIGHTY),
            arguments("fixed32", SIXTEEN, SIXTY),
            arguments("fixed64", EIGHTEEN, EIGHTY),
            arguments("sfixed32", SIXTEEN, SIXTY),
            arguments("sfixed64", EIGHTEEN, EIGHTY),
            arguments("bool", YES, NO),
            arguments("bytes", FULL_SIGNATURE, SHORT_SIGNATURE),
        )
    }

    @ParameterizedTest(name = "the default error message for `{0}` fields")
    @MethodSource("fields")
    fun <T> defaultErrorMessage(fieldName: String, value: T, nextValue: T & Any) =
        assertDefaultMessage(fieldName, value, nextValue)

    // TODO:2024-11-28:yevhenii.nadtochii: Document two nuances with enums.
    @Test
    fun `the default error message for enum fields`() = assertDefaultMessage(
        fieldName = "enum",
        value = ACM_HOSTEL.valueDescriptor,
        nextValue = ACM_APARTMENT.valueDescriptor,
        violatedValue = ACM_APARTMENT
    )

    @ParameterizedTest(name = "the custom error message for `{0}` fields")
    @MethodSource("fields")
    fun <T> customErrorMessage(fieldName: String, value: T, nextValue: T & Any) =
        assertCustomMessage(fieldName, value, nextValue)

    // TODO:2024-11-28:yevhenii.nadtochii: Document two nuances with enums.
    @Test
    fun `the custom error message for enum fields`() = assertCustomMessage(
        fieldName = "enum",
        value = ACM_HOSTEL.valueDescriptor,
        nextValue = ACM_APARTMENT.valueDescriptor,
        violatedValue = ACM_APARTMENT
    )
}

private fun <T> assertDefaultMessage(
    fieldName: String,
    value: T,
    nextValue: T & Any,
    violatedValue: T & Any = nextValue
) = assertErrorMessage(fieldName, value, nextValue, violatedValue, SetOnceDefaultErrorMsg.newBuilder(), listOf(fieldName, "$value", "$nextValue")) { DEFAULT_MESSAGE_FORMAT }

private fun <T> assertCustomMessage(
    fieldName: String,
    value: T,
    nextValue: T & Any,
    violatedValue: T & Any = nextValue
) = assertErrorMessage(fieldName, value, nextValue, violatedValue, SetOnceCustomErrorMsg.newBuilder(), listOf(fieldName, "$nextValue", "$value"), ::customMessageFormat)

private fun <T> assertErrorMessage(
    fieldName: String,
    value: T,
    nextValue: T & Any,
    violatedValue: T & Any = nextValue,
    messageFixture: Message.Builder,
    expectedParams: List<String>,
    expectedFormat: (Int) -> String // Document it is a field index.
) {
    // `(set_once)` doesn't throw if the new value equals to the current one.
    check(value != nextValue)

    val messageDescriptor = messageFixture.descriptorForType
    val field = messageDescriptor.field(fieldName)!!
    val exception = assertThrows<ValidationException> {
        messageFixture
            .setField(field, value)
            .setField(field, nextValue)
    }

    val violations = exception.constraintViolations.also { it.size shouldBe 1 }
    val violation = violations.first()
    with(violation) {
        msgFormat shouldBe expectedFormat(field.index + 1)
        paramList shouldBe expectedParams
        fieldPath shouldBe FieldPath(fieldName)
        fieldValue shouldBe toAny(violatedValue)
        typeName shouldBe messageDescriptor.fullName
    }
}

private const val DEFAULT_MESSAGE_FORMAT =
    "The field `%s` already has the value `%s` and cannot be reassigned to `%s`."

// document `i+1`
private fun customMessageFormat(i: Int) =
    "Field_$i: `%s`; the proposed value: `%s`; current value: `%s`."
