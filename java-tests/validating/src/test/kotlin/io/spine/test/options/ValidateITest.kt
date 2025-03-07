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

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`(validate)` constraint should be compiled so that")
internal class ValidateITest {

    @Nested internal inner class
    `on a singular message field` {

        @Test
        fun `reject an invalid message`() {
        }

        @Test
        fun `accept a valid message`() {
        }

        @Test
        fun `accept any non-validatable message`() {
        }

        @Test
        fun `skip validation if the field is empty`() {
        }
    }

    @Nested internal inner class
    `on a singular 'Any' field` {

        @Test
        fun `reject an invalid enclosed message`() {
        }

        @Test
        fun `accept a valid enclosed message`() {
        }

        @Test
        fun `accept any non-validatable enclosed message`() {
        }

        @Test
        fun `skip validation if the field is empty`() {
        }
    }

    @Nested internal inner class
    `on a repeated message field` {

        @Test
        fun `reject invalid messages`() {
        }

        @Test
        fun `accept if all messages are valid`() {
        }

        @Test
        fun `accept non-validatable messages`() {
        }

        @Test
        fun `skip validation if the field is empty`() {
        }
    }

    @Nested internal inner class
    `on a repeated 'Any' field` {

        @Test
        fun `reject invalid enclosed messages`() {
        }

        @Test
        fun `accept if all enclosed messages are valid`() {
        }

        @Test
        fun `accept non-validatable enclosed messages`() {
        }

        @Test
        fun `skip validation if the field is empty`() {
        }
    }

    @Nested internal inner class
    `on a map with message values` {

        @Test
        fun `reject invalid message values`() {
        }

        @Test
        fun `accept if all message values are valid`() {
        }

        @Test
        fun `accept non-validatable message values`() {
        }

        @Test
        fun `skip validation if the field is empty`() {
        }
    }

    @Nested internal inner class
    `on a map with 'Any' values` {

        @Test
        fun `reject invalid enclosed message values`() {
        }

        @Test
        fun `accept if all enclosed message values are valid`() {
        }

        @Test
        fun `accept non-validatable enclosed message values`() {
        }

        @Test
        fun `skip validation if the field is empty`() {
        }
    }
}
