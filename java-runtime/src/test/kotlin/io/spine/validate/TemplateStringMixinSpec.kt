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

package io.spine.validate

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`TemplateStringMixin` should")
internal class TemplateStringMixinSpec {

    @Nested
    inner class
    `format the template string` {

        @Test
        fun `returning the correct result`() {
            val template = templateString {
                withPlaceholders = "My dog's name is \${dog.name}."
                placeholderValue["dog.name"] = "Fido"
            }
            template.format() shouldBe "My dog's name is Fido."
        }

        @Test
        fun `returning an empty string if given an empty template`() {
            TemplateString.getDefaultInstance().format() shouldBe ""
        }

        @Test
        fun `throwing when a placeholder has no value`() {
            assertThrows<IllegalArgumentException> {
                val template = templateString {
                    withPlaceholders = "My dog's name is \${dog.name}."
                }
                template.format()
            }
        }

        @Test
        fun `ignore when a placeholder with a value is not used`() {
            assertDoesNotThrow {
                val template = templateString {
                    withPlaceholders = "My dog's name is Fido."
                    placeholderValue["dog.name"] = "Fido"
                }
                template.format()
            }
        }
    }

    @Test
    fun `format with missing placeholders`() {
        val template = templateString {
            withPlaceholders = "My dog's name is \${dog.name} and its breed is \${dog.breed}."
            placeholderValue["dog.name"] = "Fido"
        }
        template.formatUnsafe() shouldBe "My dog's name is Fido and its breed is \${dog.breed}."
    }
}
