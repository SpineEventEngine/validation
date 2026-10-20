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
import io.spine.test.tools.validate.ByteMatrix
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a repeated bytes field should")
internal class RequiredRepeatedBytesIgTest {

    private val field = "value"

    @Test
    fun `require at least one element`() {
        val instance = ByteMatrix.newBuilder()
        assertViolation(instance, field)
    }

    /**
     * An empty `bytes` element is treated as missing — analogously to how
     * an empty byte sequence is rejected for a singular `(required) bytes` field.
     */
    @Test
    fun `prohibit all elements with empty byte sequences`() {
        val allEmpty = ByteMatrix.newBuilder()
            .addValue(ByteString.EMPTY)
            .addValue(ByteString.EMPTY)
        assertViolation(allEmpty, field)
    }

    @Test
    fun `prohibit even one empty byte sequence`() {
        val instance = ByteMatrix.newBuilder()
            .addValue(ByteString.copyFromUtf8("non-empty"))
            .addValue(ByteString.EMPTY)
        assertInvalid(instance)
    }

    @Test
    fun `allow elements with non-empty byte sequences`() {
        val instance = ByteMatrix.newBuilder()
            .addValue(ByteString.copyFromUtf8("a"))
            .addValue(ByteString.copyFrom(byteArrayOf(0)))
        assertValid(instance)
    }
}
