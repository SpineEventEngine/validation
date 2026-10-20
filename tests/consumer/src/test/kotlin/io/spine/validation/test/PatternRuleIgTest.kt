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

package io.spine.validation.test

import com.google.common.truth.Truth.assertThat
import io.kotest.matchers.shouldBe
import io.spine.string.format
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(pattern)` rule should")
internal class PatternRuleIgTest {

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
        assertThat(violation.message.format())
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
