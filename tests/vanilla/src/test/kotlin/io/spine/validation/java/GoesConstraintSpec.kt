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

package io.spine.validation.java

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.optional.shouldBePresent
import io.spine.base.FieldPath
import io.spine.base.Identifier
import io.spine.base.Time.currentTime
import io.spine.validation.ConstraintViolation
import io.spine.validation.ValidatableMessage
import io.spine.tools.validation.java.given.ArchiveId
import io.spine.tools.validation.java.given.Paper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(goes)` option should be compiled so that")
internal class GoesConstraintSpec {

    private fun assertValid(m: ValidatableMessage) = assertThat(m.validate()).isEmpty()

    private fun generate() = ArchiveId.newBuilder().setUuid(Identifier.newUuid()).build()

    @Test
    fun `if associated field is not set and the target field is set 'a violation is produced'`() {
        val paper = Paper.newBuilder()
            .setWhenArchived(currentTime())
            .buildPartial()

        val error = paper.validate()
        error.shouldBePresent()

        val violations = error.get().constraintViolationList
        violations shouldHaveSize 1

        assertThat(violations[0])
            .comparingExpectedFieldsOnly()
            .isEqualTo(
                ConstraintViolation.newBuilder()
                    .setTypeName(paper.descriptorForType.fullName)
                    .setFieldPath(
                        FieldPath.newBuilder()
                            .addFieldName("when_archived")
                    )
                    .build()
            )
    }

    @Test
    fun `if both fields are set, no violation`() {
        val paper = Paper.newBuilder()
            .setArchive(generate())
            .setWhenArchived(currentTime())
            .build()
        assertValid(paper)
    }

    @Test
    fun `if neither field is set, no violation`() {
        val paper = Paper.newBuilder().build()
        assertValid(paper)
    }

    @Test
    fun `if the associated field is set and target is not set, no violation`() {
        val paper = Paper.newBuilder()
            .setArchive(generate())
            .build()
        assertValid(paper)
    }
}

