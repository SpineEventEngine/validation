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

package io.spine.test.options

import com.google.protobuf.Message
import io.kotest.matchers.collections.shouldContainExactly
import io.spine.test.tools.validate.ChoiceInDeclarationOrder
import io.spine.test.tools.validate.MinInDeclarationOrder
import io.spine.test.tools.validate.RequiredInDeclarationOrder
import io.spine.validation.Validate.violationsOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Verifies that the generated code checks the constraints of an option
 * in the order of declaration.
 *
 * A message reports the violations in the order of its constraints, so the tests
 * check the order of the violations. The test messages declare the constrained
 * fields and groups in an order that differs from both the order of the field numbers
 * and the alphabetical order of the names, so the tests are unlikely to pass when
 * the constraints go in another order.
 */
@DisplayName("Generated constraints should follow the order of declaration, so that")
internal class DeclarationOrderITest {

    @Test
    fun `violations of '(required)' fields go in the order of the fields`() {
        val message = RequiredInDeclarationOrder.getDefaultInstance()

        violatedPaths(message).shouldContainExactly(
            "echo", "bravo", "hotel", "alpha", "golf", "charlie", "foxtrot", "delta"
        )
    }

    @Test
    fun `violations of '(min)' fields go in the order of the fields`() {
        val message = MinInDeclarationOrder.getDefaultInstance()

        violatedPaths(message).shouldContainExactly(
            "delta", "alpha", "echo", "charlie", "bravo"
        )
    }

    @Test
    fun `violations of '(choice)' groups go in the order of the groups`() {
        val message = ChoiceInDeclarationOrder.getDefaultInstance()

        violatedPaths(message).shouldContainExactly(
            "charlie", "alpha", "echo", "bravo", "delta"
        )
    }
}

/**
 * Returns the field paths of the violations of the given [message]
 * in the order of the violations.
 */
private fun violatedPaths(message: Message): List<String> =
    violationsOf(message).map { it.fieldPath.fieldNameList.joinToString(".") }
