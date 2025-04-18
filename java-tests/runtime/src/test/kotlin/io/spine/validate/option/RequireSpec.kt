/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import com.google.common.truth.Truth.assertThat
import io.spine.test.validate.requiredfield.EveryFieldOptional
import io.spine.test.validate.requiredfield.EveryFieldRequired
import io.spine.test.validate.requiredfield.OneofFieldAndOtherFieldRequired
import io.spine.test.validate.requiredfield.OneofRequired
import io.spine.validate.ValidationOfConstraintTest
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName(VALIDATION_SHOULD + "analyze `(require)` message option and consider message")
internal class RequireSpec : ValidationOfConstraintTest() {

    @Nested
    @DisplayName("valid if")
    internal inner class Valid {

        @Test
        fun `all required fields are set`() = assertValid {
            EveryFieldRequired.newBuilder()
                .setFirst("first field set")
                .setSecond("second field set")
                .setThird("third field set")
                .build()
        }

        @Nested
        @DisplayName("'oneof' field")
        internal inner class OneofField {

            @Test
            fun `'first' is set`() = assertValid {
                OneofRequired.newBuilder()
                    .setFirst("first field set")
                    .build()
            }

            @Test
            fun `'second' is set`() = assertValid {
                OneofRequired.newBuilder()
                    .setSecond("second field set")
                    .build()
            }
        }

        @Test
        fun `all fields are optional`() {
            assertValid(EveryFieldOptional.getDefaultInstance())
            assertValid(
                EveryFieldOptional.newBuilder()
                    .setFirst("first field set")
                    .setThird("third field set")
                    .build()
            )
        }
    }

    @Nested
    @DisplayName("invalid if")
    internal inner class Invalid {

        @Test
        fun `a required field is not set`() {
            assertNotValid(EveryFieldRequired.getDefaultInstance(), false)
            val onlyOneRequiredSet = EveryFieldRequired.newBuilder()
                .setFirst("only one set")
                .build()
            assertNotValid(onlyOneRequiredSet, false)

            val twoRequiredSet = EveryFieldRequired.newBuilder()
                .setFirst("first set")
                .setSecond("second set")
                .build()
            assertNotValid(twoRequiredSet, false)
        }

        @Test
        fun `a required 'oneof' is not set`() {
            assertNotValid(OneofRequired.getDefaultInstance(), false)
            val withDefaultValue = OneofRequired.newBuilder()
                .setFirst("")
                .build()
            assertNotValid(withDefaultValue, false)
        }

        @Test
        fun `'oneof' or other field is not set`() {
            val exception = assertThrows<IllegalStateException> {
                validate(OneofFieldAndOtherFieldRequired.getDefaultInstance())
            }
            assertThat(exception)
                .hasCauseThat()
                .hasMessageThat()
                .contains("(")
        }
    }
}
