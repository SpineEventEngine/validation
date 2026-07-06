/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.test

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.spine.test.tools.validate.InterestRate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Tests the [validate][io.spine.validation.ValidatingBuilder.validate] probe
 * against a real generated builder.
 *
 * [InterestRate] declares a single `(min)` constraint on its `percent` field
 * (`percent` must be `> 0.0`), which makes it convenient for probing both the
 * valid and the invalid content of a builder.
 */
@DisplayName("`ValidatingBuilder` should")
internal class ValidatingBuilderSpec {

    @Test
    fun `return no violations when the builder content is valid`() {
        val builder = InterestRate.newBuilder()
            .setPercent(117.3f)

        builder.validate().shouldBeEmpty()
    }

    @Test
    fun `return violations of invalid content without throwing`() {
        val builder = InterestRate.newBuilder()
            .setPercent(-3f)

        val violations = assertDoesNotThrow { builder.validate() }

        violations.shouldNotBeEmpty()
        violations.single().fieldPath.fieldNameList shouldBe listOf("percent")
    }

    @Test
    fun `leave the builder content intact`() {
        val builder = InterestRate.newBuilder()
            .setPercent(-3f)
        val before = builder.buildPartial()

        builder.validate()

        builder.buildPartial() shouldBe before
    }
}
