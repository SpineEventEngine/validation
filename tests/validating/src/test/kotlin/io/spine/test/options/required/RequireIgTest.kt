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
import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.test.tools.validate.Citizen
import io.spine.test.tools.validate.Due
import io.spine.test.tools.validate.FieldGroup
import io.spine.type.TypeName
import io.spine.string.format
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import java.nio.charset.StandardCharsets.UTF_16
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(require)` option should be compiled so that")
internal class RequireIgTest {

    @Test
    fun `unset field produces a violation`() {
        val invalidMessage = Citizen.newBuilder()
        assertInvalidWithParam(invalidMessage, "tax_number")
    }

    @Test
    fun `unset fields produce a violation`() {
        val invalidMessage = Due.newBuilder()
        assertInvalidWithParam(invalidMessage, "date | never")
    }

    @Test
    fun `incomplete group causes a violation`() {
        val invalidMessage = FieldGroup.newBuilder()
            .setA1("a1")
            .setB2(ByteString.copyFrom("b2", UTF_16))
        assertInvalidWithParam(invalidMessage, "a1 & a2 | b1 & b2")
    }

    @Test
    fun `at least one alternative satisfies the constraint`() {
        val message = FieldGroup.newBuilder()
            .setA1("a1")
            .addA2("a2")
        assertValid(message)
    }

    @Test
    fun `if all the alternatives satisfy the constraint`() {
        val message = FieldGroup.newBuilder()
            .setA1("a1")
            .addA2("a2")
            .putB1(42, 314)
            .setB2(ByteString.copyFromUtf8("b2"))
        assertValid(message)
    }
}

private fun assertInvalidWithParam(message: Message.Builder, violationParam: String) {
    val violations = assertInvalid(message)
    val partial = message.buildPartial()
    violations.size shouldBe 1
    violations[0].message.format() shouldContain violationParam
    violations[0].typeName shouldBe TypeName.of(partial).value()
}
