/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.test

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.spine.test.tools.validate.RequiredPlaceholders
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Error message placeholders should have values")
internal class PlaceholdersSomeTest {

    @Nested
    inner class `for '(required)' option` {

        @Test
        fun `with the default error message`() {
            val exception = assertThrows<ValidationException> {
                RequiredPlaceholders.newBuilder()
                    .setValueCustom("something")
                    .build()
            }
            exception.constraintViolations shouldHaveSize 1
            val template = exception.constraintViolations[0].message
            template.withPlaceholders shouldBe "The field `\${parent.type}.\${field.path}` of the type `\${field.type}` must have a value."
            template.placeholderValueMap shouldContainExactly mapOf(
                "field.path" to "value_default",
                "field.type" to "string",
                "parent.type" to "spine.test.tools.validate.RequiredPlaceholders"
            )
        }

        @Test
        fun `with the custom error message`() {
            val exception = assertThrows<ValidationException> {
                RequiredPlaceholders.newBuilder()
                    .setValueDefault("something")
                    .build()
            }
            exception.constraintViolations shouldHaveSize 1
            val template = exception.constraintViolations[0].message
            template.withPlaceholders shouldBe "Custom for (required)."
            template.placeholderValueMap shouldContainExactly mapOf(
                "field.path" to "value_default",
                "field.type" to "string",
                "parent.type" to "spine.test.tools.validate.RequiredPlaceholders"
            )
        }
    }
}
