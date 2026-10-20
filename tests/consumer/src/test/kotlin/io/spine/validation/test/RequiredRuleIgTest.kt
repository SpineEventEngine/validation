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

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.string.format
import io.spine.string.formatUnsafe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` rule should")
internal class RequiredRuleIgTest {

    @Test
    fun `reject an unset string field`() {
        val builder = Author.newBuilder()
        val violation = assertValidationException(builder)
        violation.message.format() shouldContain "Author must have a name"
    }

    @Test
    fun `reject an unset message field`() {
        val builder = Book.newBuilder()
        assertValidationException(builder).also {
            it.message.formatUnsafe() shouldContain "must have a non-default value"
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
    fun `reject a list contains only default values`() {
        val builder = Blizzard.newBuilder()
            .addSnowflake(Snowflake.getDefaultInstance())

        // We should have three violations:
        //  - One violation is expected because of the default value of `Snowflake`.
        //  - Two properties of `Snowflake` are required,
        //    so we expect two violations because of `(validate)`.
        assertValidationExceptions(builder) shouldHaveSize 3
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
    fun `reject if a list contains at least one default value`() {
        val builder = Blizzard.newBuilder()
            .addSnowflake(
                Snowflake.newBuilder()
                    .setEdges(3)
                    .setVertices(3)
            )
            .addSnowflake(Snowflake.getDefaultInstance())

        // Again, three violations: one for being a default message plus
        // two for properties of the `Snowflake` because we validate it too.
        assertValidationExceptions(builder) shouldHaveSize 3
    }
}

private fun validAuthor(): Author = author {
    name = "Donald Knuth"
}
