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
import io.spine.test.tools.validate.Enclosed
import io.spine.test.tools.validate.Singulars
import io.spine.test.tools.validate.UltimateChoice
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option set in a `bytes` field should")
internal class RequiredBytesIgTest {

    @Test
    fun `require non-empty value`() {
        val singulars = Singulars.newBuilder()
        assertViolation(singulars, "one_or_more_bytes")
    }

    @Test
    fun `allow all zeros`() {
        val nonZeros = Singulars.newBuilder()
            .setNotDefault(Enclosed.newBuilder().setValue("non-default enclosed"))
            .setNotVegetable(UltimateChoice.CHICKEN)
            .setOneOrMoreBytes(ByteString.copyFromUtf8("non-empty"))
            .setNotEmptyString("str")
        assertValid(nonZeros)

        val zeros = byteArrayOf(0)
        val withZeroes = Singulars.newBuilder()
            .setOneOrMoreBytes(ByteString.copyFrom(zeros))
            .setNotVegetable(UltimateChoice.CHICKEN)
            .setNotDefault(Enclosed.newBuilder().setValue("   "))
            .setNotEmptyString("  ")
        assertValid(withZeroes)
    }
}
