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

package io.spine.validate

import io.spine.base.Identifier
import io.spine.test.validate.oneof.OneofAndOtherAreRequired
import io.spine.test.validate.oneof.OneofWithOptionalFields
import io.spine.test.validate.oneof.OneofWithRequiredFields
import io.spine.test.validate.oneof.OneofWithValidation
import io.spine.test.validate.oneof.RequiredOneofWithValidation
import io.spine.test.validate.oneof.oneofWithValidation
import io.spine.test.validate.oneof.requiredOneofWithValidation
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName(VALIDATION_SHOULD + "consider `oneof`")
internal class OneofSpec : ValidationOfConstraintTest() {

    @Nested internal inner class
    `valid if` {

        @Test
        fun `a required field is set to a non-default value`() = assertValid {
            OneofWithRequiredFields.newBuilder()
                .setFirst(Identifier.newUuid())
                .build()
        }

        @Test
        fun `all required fields are set`() = assertValid {
            OneofAndOtherAreRequired.newBuilder()
                .setSecond(Identifier.newUuid())
                .setThird(Identifier.newUuid())
                .build()
        }

        @Test
        fun `an optional field is set to the default value`() {
            val optionalIsDefault = OneofWithOptionalFields.newBuilder()
                .setFirst("")
                .build()
            assertValid(optionalIsDefault)
            assertValid(OneofWithOptionalFields.getDefaultInstance())
        }

        @Test
        fun `an optional field is properly validated`() {
            val validFieldSet = OneofWithValidation.newBuilder()
                .setWithValidation("valid")
                .buildPartial()
            assertValid(validFieldSet)
        }

        @Test
        fun `an optional validated field is default`() =
            assertValid(OneofWithValidation.getDefaultInstance())

        @Test
        fun `an optional field without validation is set`() = assertValid {
            oneofWithValidation {
                noValidation = "does not require validation"
            }
        }

        @Test
        fun `a required field without validation is set`() = assertValid {
            requiredOneofWithValidation {
                rawValue = "o_0"
            }
        }

        @Test
        fun `a required field with validation is set`() = assertValid {
            requiredOneofWithValidation {
                validValue = "aaa1111"
            }
        }
    }

    @Nested internal inner class
    `invalid if` {

        @Test
        fun `a required field is set to the default value`() {
            //TODO:2023-11-18:alexander.yevsyukov: This message should not be built.
            // It looks like codegen does not implement it yet.
            val requiredIsDefault = OneofWithRequiredFields.newBuilder()
                .setFirst("")
                .build()
            assertNotValid(requiredIsDefault, false)
        }

        @Test
        fun `a field within 'oneof' is not valid`() = assertDoesNotBuild {
            OneofWithValidation.newBuilder()
                .setWithValidation("   ")
                .build()
        }

        @Test
        fun `a required field is not set`() =
            assertNotValid(OneofWithRequiredFields.getDefaultInstance(), false)

        @Test
        fun `a required field is not valid`() = assertDoesNotBuild {
            RequiredOneofWithValidation.newBuilder()
                .setValidValue("###")
                .build()
        }
    }
}
