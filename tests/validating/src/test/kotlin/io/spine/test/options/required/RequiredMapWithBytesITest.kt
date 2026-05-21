/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.test.options.required

import com.google.protobuf.ByteString
import io.spine.test.tools.validate.MapWithBytesValues
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a map field with bytes values should")
internal class RequiredMapWithBytesITest {

    private val field = "payload"

    @Test
    fun `require at least one entry`() {
        val instance = MapWithBytesValues.newBuilder()
        assertViolation(instance, field)
    }

    /**
     * An entry whose value is an empty byte sequence is treated as missing —
     * analogously to how an empty byte sequence is rejected for a singular
     * `(required) bytes` field.
     */
    @Test
    fun `prohibit all entries with empty byte sequences`() {
        val allEmpty = MapWithBytesValues.newBuilder()
            .putPayload("a", ByteString.EMPTY)
            .putPayload("b", ByteString.EMPTY)
        assertViolation(allEmpty, field)
    }

    @Test
    fun `prohibit even one empty byte sequence`() {
        val instance = MapWithBytesValues.newBuilder()
            .putPayload("a", ByteString.copyFromUtf8("non-empty"))
            .putPayload("b", ByteString.EMPTY)
        assertInvalid(instance)
    }

    @Test
    fun `allow entries with non-empty byte sequences`() {
        val instance = MapWithBytesValues.newBuilder()
            .putPayload("a", ByteString.copyFromUtf8("x"))
            .putPayload("b", ByteString.copyFrom(byteArrayOf(0)))
        assertValid(instance)
    }

    @Test
    fun `allow an entry with the empty key as long as the value is non-empty`() {
        val instance = MapWithBytesValues.newBuilder()
            .putPayload("", ByteString.copyFromUtf8("v"))
        assertValid(instance)
    }
}
