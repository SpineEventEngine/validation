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

package io.spine.validation.test

import io.spine.tools.validation.test.money.Mru
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * This is the integration test that verifies the custom validation implemented
 * by [CurrencyOption] in the `:java-tests:extensions` module.
 *
 * The `extensions` module declares the `(currency)` message option, which is used by
 * money data types in this module. See `main/proto/test/money.proto` for details.
 *
 * This test verifies that custom validation code works as expected.
 */
@DisplayName("`CurrencyOption` should generate the code which")
internal class CurrencyOptionIgTest {

    @Test
    fun `throws 'ValidationException' if actual value is greater than the threshold`() {
        assertValidationException(Mru.newBuilder().setKhoums(6))
    }

    @Test
    fun `throws 'ValidationException' if actual value is equal to the threshold`() {
        assertValidationException(Mru.newBuilder().setKhoums(5))
    }

    @Test
    fun `throws no exceptions if actual value is less than the threshold`() {
        assertNoException(Mru.newBuilder().setKhoums(4))
    }
}
