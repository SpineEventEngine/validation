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

import com.google.protobuf.stringValue
import com.google.protobuf.util.Timestamps
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.pack
import io.spine.test.tools.validate.SetOnceDefaultErrorMsg
import io.spine.test.tools.validate.name
import io.spine.test.tools.validate.setOnceDefaultErrorMsg
import io.spine.test.tools.validate.setOnceExplicitFalse
import io.spine.test.tools.validate.setOnceImplicitFalse
import io.spine.validate.ValidationException
import io.spine.validation.assertions.assertValidationPasses
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`(set_once)` constraint should")
internal class SetOnceTest {

    @Test
    @Disabled
    fun `not affect fields without the option`() = assertValidationPasses {
        setOnceImplicitFalse {
            message = name { value = "MyName1" }
            message = name { value = "MyName2" }
            string = "string-1"
            string = "string-2"
            double = 0.25
            double = 0.75
            float = 0.25f
            float = 0.75f
            int32 = 5
            int32 = 10
            int64 = 5
            int64 = 10
            uint32 = 5
            uint32 = 10
            uint64 = 5
            uint64 = 10
            sint32 = 5
            sint32 = 10
            sint64 = 5
            sint64 = 10
            fixed32 = 5
            fixed32 = 10
            fixed64 = 5
            fixed64 = 10
            sfixed32 = 5
            sfixed32 = 10
            sfixed64 = 5
            sfixed64 = 10
        }
    }

    @Test
    @Disabled
    fun `not affect fields with the option set to 'false'`() = assertValidationPasses {
        setOnceExplicitFalse {
            message = name { value = "MyName1" }
            message = name { value = "MyName2" }
            string = "string-1"
            string = "string-2"
            double = 0.25
            double = 0.75
            float = 0.25f
            float = 0.75f
            int32 = 5
            int32 = 10
            int64 = 5
            int64 = 10
            uint32 = 5
            uint32 = 10
            uint64 = 5
            uint64 = 10
            sint32 = 5
            sint32 = 10
            sint64 = 5
            sint64 = 10
            fixed32 = 5
            fixed32 = 10
            fixed64 = 5
            fixed64 = 10
            sfixed32 = 5
            sfixed32 = 10
            sfixed64 = 5
            sfixed64 = 10
        }
    }

    @Test
    fun `show the default error message for message field`() {
        val firstValue = Timestamps.now()
        val secondValue = Timestamps.now()

        val exception = assertThrows<ValidationException> {
            setOnceDefaultErrorMsg {
                message = firstValue
                message = secondValue
            }
        }

        val violations = exception.constraintViolations
        violations.size shouldBe 1

        val violation = violations[0]
        violation.msgFormat shouldBe DEFAULT_MESSAGE_FORMAT
        violation.paramList shouldBe listOf("message", "$firstValue", "$secondValue")
        violation.fieldPath shouldBe FieldPath("message")
        violation.fieldValue shouldBe secondValue.pack()
        violation.typeName shouldBe SetOnceDefaultErrorMsg.getDescriptor().fullName
    }

    @Test
    fun `show the default error message for string field`() {
        val firstValue = "aaa"
        val secondValue = "bbb"

        val exception = assertThrows<ValidationException> {
            setOnceDefaultErrorMsg {
                string = firstValue
                string = secondValue
            }
        }

        val violations = exception.constraintViolations
        violations.size shouldBe 1

        val violation = violations[0]
        violation.msgFormat shouldBe DEFAULT_MESSAGE_FORMAT
        violation.paramList shouldBe listOf("string", firstValue, secondValue)
        violation.fieldPath shouldBe FieldPath("string")
        violation.fieldValue shouldBe stringValue { value = secondValue }.pack()
        violation.typeName shouldBe SetOnceDefaultErrorMsg.getDescriptor().fullName
    }

    @Test
    @Disabled
    fun `show the custom error message`() {

    }
}

private const val DEFAULT_MESSAGE_FORMAT =
    "The field `%s` already has the value `%s` and cannot be reassigned to `%s`."
