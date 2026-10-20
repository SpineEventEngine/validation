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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option in a number field should")
internal class RequiredNumberIgTest {

    /**
     * Now the test simply accepts the fact that the option set on
     * a number field accepts the default value.
     *
     * We should probably [do more](https://github.com/SpineEventEngine/validation/issues/145).
     */
    @Test
    fun `not cause validation errors`() {
        val singulars = Singulars.newBuilder()
            .setOneOrMoreBytes(ByteString.copyFromUtf8("qwerty"))
            .setNotVegetable(UltimateChoice.CHICKEN)
            .setNotEmptyString("   ")
            .setNotDefault(Enclosed.newBuilder().setValue("  "))
        assertValid(singulars)
    }
}
