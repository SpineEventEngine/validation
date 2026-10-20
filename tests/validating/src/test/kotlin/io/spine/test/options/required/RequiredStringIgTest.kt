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

@DisplayName("`(required)` option in a string field should")
internal class RequiredStringIgTest {

    @Test
    fun `require a non-empty value being set`() {
        val singulars = Singulars.newBuilder()
        assertViolation(singulars, "not_empty_string")
    }

    @Test
    fun `accept non-empty values`() {
        val singulars = Singulars.newBuilder()
            .setNotEmptyString(" ")
            .setNotVegetable(UltimateChoice.FISH)
            .setNotDefault(Enclosed.newBuilder().setValue("  "))
            .setOneOrMoreBytes(ByteString.copyFromUtf8("foobar"))
        assertValid(singulars)
    }
}
