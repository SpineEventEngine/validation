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
import io.spine.base.Identifier
import io.spine.test.tools.validate.Enclosed
import io.spine.test.tools.validate.Singulars
import io.spine.test.tools.validate.UltimateChoice
import io.spine.tools.validation.assertions.assertValid
import io.spine.tools.validation.assertions.assertViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("(required)` option in a message field should")
internal class RequiredMessageIgTest {

    @Test
    fun `prohibit a default message`() {
        val singulars = Singulars.newBuilder()
        assertViolation(singulars, "not_default")
    }

    @Test
    fun `accept a non-default instance`() {
        val singulars = Singulars.newBuilder()
            .setNotVegetable(UltimateChoice.CHICKEN)
            .setOneOrMoreBytes(ByteString.copyFromUtf8("lalala"))
            .setNotDefault(Enclosed.newBuilder().setValue(Identifier.newUuid()))
            .setNotEmptyString(" ")
        assertValid(singulars)
    }
}
