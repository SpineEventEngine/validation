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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` rule should")
internal class RequiredRuleITest {

    @Test
    fun `reject an unset string field`() {
        val builder = Author.newBuilder()
        val violation = assertValidationException(builder)
        violation.msgFormat shouldContain "Author must have a name"
    }

    @Test
    fun `reject an unset message field`() {
        val builder = Book.newBuilder()
        assertValidationException(builder).also {
            it.msgFormat shouldContain "value must be set"
            it.fieldPath.getFieldName(0) shouldBe "author"
        }
    }

    @Test
    fun `pass if a value is set`() = assertNoException(
        Author.newBuilder().setName("Evans")
    )

    @Test
    fun `pass if not required`() = assertNoException(
        // The only `(required)` field of `Book` is `author`.
        Book.newBuilder().setAuthor(validAuthor())
        // Other fields do not have this option.
    )

    @Test
    @Disabled("Until we propagate the `validate` option property to collection elements.")
    fun `reject a list contains only default values`() {
        val builder = Blizzard.newBuilder()
            .addSnowflake(Snowflake.getDefaultInstance())
        assertValidationException(builder)
    }

    @Test
    fun `pass if a list contains all non-default values`() = assertNoException(
        Blizzard.newBuilder()
            .addSnowflake(
                Snowflake.newBuilder()
                    .setEdges(3)
                    .setVertices(3)
            )
    )

    @Test
    @Disabled("Until we propagate the `validate` option property to collection elements.")
    fun `reject if a list contains at least one default value`() {
        val builder = Blizzard.newBuilder()
            .addSnowflake(
                Snowflake.newBuilder()
                    .setEdges(3)
                    .setVertices(3)
            )
            .addSnowflake(Snowflake.getDefaultInstance())
        assertValidationException(builder)
    }
}

private fun validAuthor(): Author = author {
    name = "Donald Knuth"
}
