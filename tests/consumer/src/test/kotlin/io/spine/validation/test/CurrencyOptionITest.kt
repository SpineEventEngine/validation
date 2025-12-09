/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.validation.test

import io.spine.tools.validation.test.money.Mru
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * This is the integration test that verifies the custom validation implemented
 * by [CurrencyOption] in the `:java-tests:extensions` module.
 *
 * The `extensions` module declares the `(currency)` message option, which is used by
 * money data types in this module. Please see `main/proto/test/money.proto` for details.
 *
 * This test verifies that custom validation code works as expected.
 */
@DisplayName("`CurrencyOption` should generate the code which")
internal class CurrencyOptionITest {

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
