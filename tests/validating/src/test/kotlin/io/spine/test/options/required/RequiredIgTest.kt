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

import com.google.common.truth.Truth.assertThat
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.optional.shouldBePresent
import io.spine.test.tools.validate.Singulars
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(required)` option should be compiled so that")
internal class RequiredIgTest {

    @Test
    fun `all violations on a single message are collected`() {
        val instance = Singulars.getDefaultInstance()
        val error = instance.validate()
        assertThat(error).isPresent()
        error.shouldBePresent()
        error.get().constraintViolationList shouldHaveSize 4
    }
}
