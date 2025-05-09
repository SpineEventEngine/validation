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

package io.spine.test.options.required

import com.google.protobuf.ByteString
import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.test.tools.validate.Citizen
import io.spine.test.tools.validate.Due
import io.spine.test.tools.validate.FieldGroup
import io.spine.type.TypeName
import io.spine.validate.format
import io.spine.validation.assertions.assertInvalid
import io.spine.validation.assertions.assertValid
import java.nio.charset.StandardCharsets.UTF_16
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(require)` option should be compiled so that")
internal class RequireITest {

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
