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
import com.google.protobuf.ByteString
import com.google.protobuf.util.Timestamps
import io.spine.base.Identifier.newUuid
import io.spine.test.validate.payment
import io.spine.test.validate.paymentId
import io.spine.test.validate.withFieldNotFound
import io.spine.validate.ValidationOfConstraintTest
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName(VALIDATION_SHOULD + "analyze `(goes)` option and find out that ")
internal class GoesSpec : ValidationOfConstraintTest() {

    @Nested
    @DisplayName("`(goes).with` fields")
    inner class GoesWithFields {

        private val paymentId = paymentId { uuid = newUuid() }

        @Test
        fun `are both optional` () = assertValid {
            payment {
                description = "Scheduled payment"
            }
        }

        @Test
        fun `not filled separately` () {
            assertNotValid(
                payment {
                    id = paymentId
                }
            )
            assertNotValid(
                payment {
                    timestamp = Timestamps.MAX_VALUE
                }
            )
        }

        @Test
        fun `are filled simultaneously`() = assertValid {
            payment {
                id = paymentId
                timestamp = Timestamps.MAX_VALUE
            }
        }
    }

    /**
     * This method checks that the validation of the `(goes).with` constraint fails
     * if the option refers to a field which is not present in the message.
     *
     * The `build()` method for this message passes because the `(goes)` option is not
     * yet supported by code generation. This option is checked on runtime.
     */
    @Test
    fun `The value of the 'with' field is not found`() {
        val msg = withFieldNotFound {
            id = newUuid()
            // Here we populate the field which has `(goes).with` option referring to a
            // field which is not present in the message (`user_id`).
            avatar = ByteString.copyFrom(byteArrayOf(0, 1, 2))
        }
        val exception = assertThrows<IllegalStateException> {
            validate(msg)
        }
        assertThat(exception)
            .hasCauseThat()
            .hasMessageThat()
            .contains("user_id")
    }
}
