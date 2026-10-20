/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.test.options

import io.spine.base.Identifier
import io.spine.test.tools.validate.TaskId
import io.spine.test.tools.validate.command.AssignTask
import io.spine.test.tools.validate.command.CreateProject
import io.spine.test.tools.validate.entity.NumberStats
import io.spine.test.tools.validate.entity.Project
import io.spine.test.tools.validate.entity.Task
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("The first field in a message is assumed `(required)` in")
internal class AssumedRequiredIgTest {

    @Nested inner class
    `a command` {

        @Test
        fun `which must have an ID`() {
            val msg = CreateProject.newBuilder()
            assertViolation(msg, "id")
        }

        @Test
        fun `an ID of which cannot be empty`() = assertValid(
            CreateProject.newBuilder()
                .setId(Identifier.newUuid())
        )

        @Test
        fun `an ID of which cannot be a default message`() {
            val builder = AssignTask.newBuilder().setTask(TaskId.getDefaultInstance())
            assertInvalid(builder)
        }
    }

    @Nested inner class
    `an entity state` {

        @Test
        fun `which is an ID of the entity`() {
            val msg = Project.newBuilder()
            assertViolation(msg, "id")
        }

        @Test
        fun `ID of which must be a non-empty value`() {
            val msg = Project.newBuilder()
                .setId(Identifier.newUuid())
            assertValid(msg)
        }

        @Test
        fun `allowing to omit, if set as not 'required' explicitly`() {
            val msg = Task.newBuilder()
            assertValid(msg)
        }

        @Test
        fun `not requiring a primitive ID`() {
            val msg = NumberStats.newBuilder()
            assertValid(msg)
        }
    }
}
