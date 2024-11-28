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

import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.util.Timestamps.now
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter
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
import io.spine.test.tools.validate.SetOnceDefaultErrorMsg
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`(set_once)` constraint should show the default error message")
internal class SetOnceDefaultMsgITest {

    @Test
    fun `for message field`() = assertDefaultMessage(field("message"), now(), now())

    @Test
    fun `for string field`() = assertDefaultMessage(field("string"), STUDENT1, STUDENT2)

    @Test
    fun `for double field`() = assertDefaultMessage(field("double"), METER_AND_HALF, METER_AND_EIGHT)

    @Test
    fun `for float field`() = assertDefaultMessage(field("float"), FIFTY_KG, EIGHTY_KG)

    @Test
    fun `for int32 field`() = assertDefaultMessage(field("int32"), SIXTEEN, SIXTY)

    @Test
    fun `for int64 field`() = assertDefaultMessage(field("int64"), EIGHTEEN, EIGHTY)

    @Test
    fun `for uint32 field`() = assertDefaultMessage(field("uint32"), SIXTEEN, SIXTY)

    @Test
    fun `for uint64 field`() = assertDefaultMessage(field("uint64"), EIGHTEEN, EIGHTY)

    @Test
    fun `for sint32 field`() = assertDefaultMessage(field("sint32"), SIXTEEN, SIXTY)

    @Test
    fun `for sint64 field`() = assertDefaultMessage(field("sint64"), EIGHTEEN, EIGHTY)

    @Test
    fun `for fixed32 field`() = assertDefaultMessage(field("fixed32"), SIXTEEN, SIXTY)

    @Test
    fun `for fixed64 field`() = assertDefaultMessage(field("fixed64"), EIGHTEEN, EIGHTY)

    @Test
    fun `for sfixed32 field`() = assertDefaultMessage(field("sfixed32"), SIXTEEN, SIXTY)

    @Test
    fun `for sfixed64 field`() = assertDefaultMessage(field("sfixed64"), EIGHTEEN, EIGHTY)

    @Test
    fun `for bool field`() = assertDefaultMessage(field("bool"), YES, NO)

    @Test
    fun `for bytes field`() = assertDefaultMessage(field("bytes"), FULL_SIGNATURE, SHORT_SIGNATURE)

    // TODO:2024-11-28:yevhenii.nadtochii: Can we get rid of `violatedValue` property?
    @Test
    fun `for enum field`() = assertDefaultMessage(
        field = field("enum"),
        value = ACM_HOSTEL.valueDescriptor,
        nextValue = ACM_APARTMENT.valueDescriptor,
        violatedValue = ACM_APARTMENT.number
    )
}

private fun <T> assertDefaultMessage(
    field: FieldDescriptor,
    value: T,
    nextValue: T & Any,
    violatedValue: T & Any = nextValue
) {
    // `(set_once)` doesn't throw if the new value equals to the current one.
    value shouldNotBe nextValue

    val exception = assertThrows<ValidationException> {
        SetOnceDefaultErrorMsg.newBuilder()
            .setField(field, value)
            .setField(field, nextValue)
    }

    val violations = exception.constraintViolations
    violations.size shouldBe 1

    val violation = violations[0]
    violation.msgFormat shouldBe DEFAULT_MESSAGE_FORMAT
    violation.paramList shouldBe listOf(field.name, "$value", "$nextValue")
    violation.fieldPath shouldBe FieldPath(field.name)
    violation.fieldValue shouldBe TypeConverter.toAny(violatedValue)
    violation.typeName shouldBe SetOnceDefaultErrorMsg.getDescriptor().fullName
}

private fun field(name: String) = SetOnceDefaultErrorMsg.getDescriptor()
    .findFieldByName(name)

private const val DEFAULT_MESSAGE_FORMAT =
    "The field `%s` already has the value `%s` and cannot be reassigned to `%s`."
