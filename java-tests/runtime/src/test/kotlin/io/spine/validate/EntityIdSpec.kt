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

package io.spine.validate

import io.spine.base.Identifier
import io.spine.test.validate.AggregateState
import io.spine.test.validate.ProjectionState
import io.spine.test.validate.aggregateState
import io.spine.test.validate.command.EntityIdMsgFieldValue
import io.spine.test.validate.command.EntityIdStringFieldValue
import io.spine.test.validate.command.entityIdMsgFieldValue
import io.spine.test.validate.command.entityIdStringFieldValue
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import io.spine.validate.given.MessageValidatorTestEnv
import io.spine.validate.given.MessageValidatorTestEnv.newStringValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@DisplayName(VALIDATION_SHOULD + "validate an entity ID")
internal class EntityIdSpec : ValidationOfConstraintTest() {

    @Nested inner class
    `in a command file and` {

        @Test
        fun `find out that non-default message is valid`() {
            val state = assertDoesNotThrow {
                entityIdMsgFieldValue {
                    value = newStringValue()
                }
            }
            assertValid(state)
        }

        @Test
        fun `find out that default message is NOT valid`() {
            val msg = EntityIdMsgFieldValue.getDefaultInstance()
            assertNotValid(msg)
        }

        @Test
        fun `find out that empty string is NOT valid`() {
            val msg = EntityIdStringFieldValue.getDefaultInstance()
            assertNotValid(msg)
        }

        @Test
        fun `find out that non-empty string is valid`() {
            val state = assertDoesNotThrow {
                entityIdStringFieldValue {
                    value = Identifier.newUuid()
                }
            }
            assertValid(state)
        }

        @Test
        fun `provide one valid violation if is not valid`() {
            val msg = EntityIdMsgFieldValue.getDefaultInstance()
            assertSingleViolation(msg, MessageValidatorTestEnv.VALUE)
        }
    }

    @Nested inner class
    `in state and` {

        @Test
        fun `consider it required by default`() {
            val stateWithDefaultId = AggregateState.getDefaultInstance()
            assertNotValid(stateWithDefaultId)
        }

        @Test
        fun `match only the first field named 'id' or ending with '_id'`() {
            val onlyEntityIdSet = assertDoesNotThrow {
                // Only ID set.
                aggregateState {
                    entityId = Identifier.newUuid()
                }
            }
            assertValid(onlyEntityIdSet)
        }

        @Test
        fun `not consider it '(required)' if the option is set explicitly set to false`() {
            val stateWithDefaultId = ProjectionState.getDefaultInstance()
            assertValid(stateWithDefaultId)
        }
    }
}
