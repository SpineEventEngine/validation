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

package io.spine.test.options

import io.spine.base.Identifier
import io.spine.test.tools.validate.TaskId
import io.spine.test.tools.validate.command.AssignTask
import io.spine.test.tools.validate.command.CreateProject
import io.spine.test.tools.validate.entity.Project
import io.spine.test.tools.validate.entity.Task
import io.spine.test.tools.validate.event.ProjectCreated
import io.spine.test.tools.validate.rejection.TestRejections
import io.spine.validation.assertions.assertInvalid
import io.spine.validation.assertions.assertValid
import io.spine.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("The first field in a message is assumed `(required)` in")
internal class AssumedRequiredITest {

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
    `an event` {

        @Test
        fun `requiring it`() {
            val msg = ProjectCreated.newBuilder()
            assertViolation(msg, "id")
        }
    }

    @Nested inner class
    `a rejection` {

        @Test
        fun `requiring it`() {
            val msg = TestRejections.CannotCreateProject.newBuilder()
            assertViolation(msg, "id")
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
    }
}
