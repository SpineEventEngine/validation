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

package io.spine.validation.test

import com.google.common.truth.Truth.assertThat
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(pattern)` rule should")
internal class PatternRuleITest {

    @Test
    fun `allow matching values`() {
        val player = Player.newBuilder()
            .setShirtName("Regina Falangee")
        assertNoException(player)
    }

    @Test
    fun `prohibit non-matching values`() {
        val player = Player.newBuilder()
            .setShirtName("R")
        val violation = assertValidationException(player)
        assertThat(violation.message.withPlaceholders)
            .contains("Invalid T-Shirt name")
    }

    @Test
    fun `allow partial matches`() {
        val msg = Book.newBuilder()
            .setAuthor(validAuthor())
            .setContent("Something Something Pride Something Something")
        assertNoException(msg)
    }

    @Test
    fun `allow ignoring case`() {
        val msg = Book.newBuilder()
            .setAuthor(validAuthor())
            .setContent("preJudice")
        assertNoException(msg)
    }

    @Test
    fun `fail even with loose rules`() {
        val msg = Book.newBuilder()
            .setAuthor(validAuthor())
            .setContent("something else")
        val violation = assertValidationException(msg)
        violation.fieldPath.getFieldName(0) shouldBe "content"
    }

    @Test
    fun `and handle special characters in the pattern properly`() =
        assertNoException(Team.newBuilder().setName("Sch 04"))
}

private fun validAuthor(): Author = author {
    name = "Donald Knuth"
}
