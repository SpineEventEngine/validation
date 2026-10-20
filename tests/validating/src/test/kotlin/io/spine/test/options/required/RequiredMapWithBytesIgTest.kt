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

import com.google.protobuf.ByteString
import io.spine.test.tools.validate.MapWithBytesValues
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a map field with bytes values should")
internal class RequiredMapWithBytesIgTest {

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
