/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.validate.option

import com.google.protobuf.StringValue
import io.spine.base.Identifier
import io.spine.test.validate.CustomMessageWithNoRequiredOption
import io.spine.test.validate.Planet
import io.spine.test.validate.RepeatedRequiredMsgFieldValue
import io.spine.test.validate.RequiredBooleanFieldValue
import io.spine.test.validate.RequiredByteStringFieldValue
import io.spine.test.validate.RequiredEnumFieldValue
import io.spine.test.validate.RequiredMsgFieldValue
import io.spine.test.validate.RequiredStringFieldValue
import io.spine.test.validate.repeatedRequiredMsgFieldValue
import io.spine.test.validate.requiredByteStringFieldValue
import io.spine.test.validate.requiredStringFieldValue
import io.spine.testing.logging.mute.MuteLogging
import io.spine.validate.ValidationOfConstraintTest
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import io.spine.validate.given.MessageValidatorTestEnv
import io.spine.validate.given.MessageValidatorTestEnv.newByteString
import io.spine.validate.given.MessageValidatorTestEnv.newStringValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName(VALIDATION_SHOULD + "analyze `(required)` option and")
internal class RequiredTest : ValidationOfConstraintTest() {
    
    @Test
    fun `find out that required 'Message' field is set`() = assertValid {
        RequiredMsgFieldValue.newBuilder()
            .setValue(newStringValue())
            .build()
    }

    @Test
    fun `find out that required message field is NOT set`() = assertNotValid(
        RequiredMsgFieldValue.getDefaultInstance()
    )

    @Test
    fun `find out that required 'String' field is set`() = assertValid {
        RequiredStringFieldValue.newBuilder()
            .setValue(Identifier.newUuid())
            .build()
    }

    @Test
    fun `find out that required 'String' field is NOT set`() {
        val invalidMsg = RequiredStringFieldValue.getDefaultInstance()
        assertNotValid(invalidMsg)
    }

    @Test
    fun `find out that required 'ByteString' field is set`() = assertValid {
        requiredByteStringFieldValue {
            value = newByteString()
        }
    }

    @Test
    fun `find out that required 'ByteString' field is NOT set`() {
        assertCheckFails(
            RequiredByteStringFieldValue.getDefaultInstance()
        )
        assertDoesNotBuild {
            requiredStringFieldValue {  }
        }
    }

    @Test
    fun `find out that required 'Enum' field is set`() = assertValid {
        RequiredEnumFieldValue.newBuilder()
            .setValue(Planet.EARTH)
            .build()
    }

    @Test
    fun `find out that required 'Enum' field is NOT set`() = assertNotValid(
        RequiredEnumFieldValue.getDefaultInstance()
    )

    @MuteLogging
    @Test
    fun `find out that required NON-SET 'Boolean' field passes validation`() = assertValid(
        RequiredBooleanFieldValue.getDefaultInstance()
    )

    @Test
    fun `find out that repeated required field has valid values`() = assertValid {
        RepeatedRequiredMsgFieldValue.newBuilder()
            .addValue(newStringValue())
            .addValue(newStringValue())
            .build()
    }

    @Test
    fun `find out that repeated required field has no values`() = assertNotValid(
        RepeatedRequiredMsgFieldValue.getDefaultInstance()
    )

    @Test
    fun `accept 'repeated' and 'required' field with default message entries`() {
        // One valid value, one default instance. We do not validate deeper.
        assertValid {
            repeatedRequiredMsgFieldValue {
                value.add(newStringValue()) // valid value
                value.add(StringValue.getDefaultInstance()) // empty value
            }
        }
        // Two default instances.
        repeatedRequiredMsgFieldValue {
            value.add(StringValue.getDefaultInstance())
            value.add(StringValue.getDefaultInstance())
        }
    }

    @Test
    fun `consider field is valid if no required option set`() {
        val validMsg = StringValue.getDefaultInstance()
        assertValid(validMsg)
    }

    @Test
    fun `provide one valid violation if required field is NOT set`() {
        val invalidMsg = RequiredStringFieldValue.getDefaultInstance()
        assertSingleViolation(invalidMsg, MessageValidatorTestEnv.VALUE)
    }

    @Test
    fun `ignore 'IfMissingOption' if field is not marked required`() = assertValid {
        CustomMessageWithNoRequiredOption.getDefaultInstance()
    }
}
