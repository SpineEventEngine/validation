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

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.spine.base.Time
import io.spine.base.fieldPath
import io.spine.test.tools.validate.ArchiveId
import io.spine.test.tools.validate.Paper
import io.spine.test.tools.validate.paper
import io.spine.type.TypeName
import io.spine.validate.constraintViolation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@DisplayName("`(goes)` option should be compiled so that")
internal class GoesITest {

    @Test
    @Disabled("https://github.com/SpineEventEngine/mc-java/issues/119")
    fun `it indicates violation if the associated field is not set and the target field is set`() {
        val paper = Paper.newBuilder()
            .setWhenArchived(Time.currentTime())
            .buildPartial()
        val error = paper.validate()
        error.shouldBePresent()

        val violations = error.get().constraintViolationList
        violations.size shouldBe 1

        val expected = constraintViolation {
            typeName = TypeName.of(paper).value()
            fieldPath = fieldPath { fieldName.add("when_archived") }
        }

        assertThat(violations[0])
            .comparingExpectedFieldsOnly()
            .isEqualTo(expected)
    }

    @Nested inner class
    `it indicates no violation if` {

        @Test
        fun `both fields are set`() {
            val paper = assertDoesNotThrow {
                paper {
                    archiveId = ArchiveId.generate()
                    whenArchived = Time.currentTime()
                }
            }
            paper.validate().shouldBeEmpty()
        }

        @Test
        fun `neither field is set`() {
            val paper = assertDoesNotThrow {
                paper { }
            }
            paper.validate().shouldBeEmpty()
        }
    }

    @Test
    fun `there is no violation if the associated field is set and target is not set`() {
        val paper = assertDoesNotThrow {
            paper {
                archiveId = ArchiveId.generate()
            }
        }
        paper.validate().shouldBeEmpty()
    }
}
