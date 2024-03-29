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

package io.spine.validate;

import io.spine.test.validate.AggregateState;
import io.spine.test.validate.ProjectionState;
import io.spine.test.validate.command.EntityIdMsgFieldValue;
import io.spine.test.validate.command.EntityIdStringFieldValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.validate.ValidationOfConstraintTest.VALIDATION_SHOULD;
import static io.spine.validate.given.MessageValidatorTestEnv.VALUE;
import static io.spine.validate.given.MessageValidatorTestEnv.newStringValue;

@DisplayName(VALIDATION_SHOULD + "validate an entity ID")
class EntityIdTest extends ValidationOfConstraintTest {
    
    @Nested
    @DisplayName("in a command file and")
    class InCommandFile {

        @Test
        @DisplayName("find out that non-default message is valid")
        void findOutThatMessageEntityIdInCommandIsValid() {
            var msg = EntityIdMsgFieldValue.newBuilder()
                    .setValue(newStringValue())
                    .build();
            assertValid(msg);
        }

        @Test
        @DisplayName("find out that default message is NOT valid")
        void findOutThatMessageEntityIdInCommandIsNotValid() {
            var msg = EntityIdMsgFieldValue.getDefaultInstance();
            assertNotValid(msg);
        }

        @Test
        @DisplayName("find out that non-empty string is valid")
        void findOutThatStringEntityIdInCommandIsValid() {
            var msg = EntityIdStringFieldValue.newBuilder()
                    .setValue(newUuid())
                    .build();
            assertValid(msg);
        }

        @Test
        @DisplayName("find out that empty string is NOT valid")
        void findOutThatStringEntityIdInCommandIsNotValid() {
            var msg = EntityIdStringFieldValue.getDefaultInstance();
            assertNotValid(msg);
        }

        @Test
        @DisplayName("provide one valid violation if is not valid")
        void provideOneValidViolationIfEntityIdInCommandIsNotValid() {
            var msg = EntityIdMsgFieldValue.getDefaultInstance();
            assertSingleViolation(msg, VALUE);
        }
    }

    @Nested
    @DisplayName("in state and")
    class InState {

        @Test
        @DisplayName("consider it required by default")
        void requiredByDefault() {
            var stateWithDefaultId = AggregateState.getDefaultInstance();
            assertNotValid(stateWithDefaultId);
        }

        @Test
        @DisplayName("match only the first field named `id` or ending with `_id`")
        void onlyFirstField() {
            var onlyEntityIdSet = AggregateState.newBuilder()
                    .setEntityId(newUuid())
                    .build();
            assertValid(onlyEntityIdSet);
        }

        @Test
        @DisplayName("not consider it (required) if the option is set explicitly set to false")
        void notRequiredIfOptionIsFalse() {
            var stateWithDefaultId = ProjectionState.getDefaultInstance();
            assertValid(stateWithDefaultId);
        }
    }
}
