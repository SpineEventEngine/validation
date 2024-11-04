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

package io.spine.validate

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.spine.validate.option.ValidatingOptionFactory
import java.util.function.Function
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ValidatingOptionFactory` should")
internal class ValidatingOptionFactorySpec {

    private lateinit var options: ValidatingOptionFactory

    @BeforeEach
    fun setUp() {
        options = object : ValidatingOptionFactory {}
    }

    /**
     * Verifies that [ValidatingOptionFactory] does not force the classes that
     * implement this interface to provide implementations for the methods.
     *
     * The interface has all the methods declared as `default` providing the implementations.
     *
     * This test checks that the requirement is met by providing the simplest possible
     * implementation via an anonymous object. If this test compiles, we're good to go.
     */
    @Test
    fun `have no abstract methods`() {
        val factory = object : ValidatingOptionFactory {}
        factory.forBoolean().size shouldBe 0
    }

    @Test
    fun `provide empty sets of options for all types by default`() {
        assetEmpty { it.forBoolean() }
        assetEmpty { it.forByteString() }
        assetEmpty { it.forDouble() }
        assetEmpty { it.forEnum() }
        assetEmpty { it.forFloat() }
        assetEmpty { it.forInt() }
        assetEmpty { it.forLong() }
        assetEmpty { it.forMessage() }
        assetEmpty { it.forString() }
    }

    private fun assetEmpty(typeSelector: Function<ValidatingOptionFactory, Set<*>>) {
        val result = typeSelector.apply(options)
        result.shouldBeEmpty()
    }
}
