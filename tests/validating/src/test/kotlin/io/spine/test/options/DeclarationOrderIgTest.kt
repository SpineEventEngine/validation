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
internal class DeclarationOrderIgTest {

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
