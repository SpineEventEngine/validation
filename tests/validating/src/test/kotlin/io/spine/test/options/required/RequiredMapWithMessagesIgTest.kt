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

package io.spine.test.options.required

import com.google.protobuf.Timestamp
import io.spine.test.tools.validate.MapWithMessageValues
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a map field with message values should")
internal class RequiredMapWithMessagesIgTest {

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
