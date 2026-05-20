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

import com.google.protobuf.Timestamp
import io.spine.test.tools.validate.MapWithMessageValues
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a map field with message values should")
internal class RequiredMapWithMessagesITest {

    @Test
    fun `require at least one entry`() {
        val instance = MapWithMessageValues.newBuilder()
        assertViolation(instance, "timestamp")
    }

    /**
     * An entry whose value is the default instance of the message type is
     * treated as missing — analogously to how the default instance is
     * rejected for a singular `(required)` message field.
     */
    @Test
    fun `prohibit an entry whose value is the default message instance`() {
        val instance = MapWithMessageValues.newBuilder()
            .putTimestamp("k", Timestamp.getDefaultInstance())
        assertViolation(instance, "timestamp")
    }

    @Test
    fun `prohibit a map that mixes a valid entry with a default-instance entry`() {
        val instance = MapWithMessageValues.newBuilder()
            .putTimestamp("a", nonDefaultTimestamp())
            .putTimestamp("b", Timestamp.getDefaultInstance())
        assertInvalid(instance)
    }

    @Test
    fun `allow entries with non-default message values`() {
        val instance = MapWithMessageValues.newBuilder()
            .putTimestamp("a", nonDefaultTimestamp())
            .putTimestamp("b", Timestamp.newBuilder().setNanos(1).build())
        assertValid(instance)
    }

    @Test
    fun `allow an entry with the empty key as long as the value is non-default`() {
        val instance = MapWithMessageValues.newBuilder()
            .putTimestamp("", nonDefaultTimestamp())
        assertValid(instance)
    }

    private fun nonDefaultTimestamp(): Timestamp =
        Timestamp.newBuilder().setSeconds(1_700_000_000L).build()
}
