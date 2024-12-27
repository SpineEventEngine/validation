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

package io.spine.test.options

import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.spine.test.tools.validate.Email
import io.spine.test.tools.validate.ShippingAddress
import io.spine.test.tools.validate.SimplePersonName
import io.spine.test.tools.validate.User
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("External constraints in the generated code should")
internal class ExternalConstraintITest {

    @Test
    @Disabled("We probably do not want to support external constraints since 2.0.0 at all.")
    fun `call external validation`() {
        val user = User.newBuilder()
            .addContact(Email.newBuilder().setValue("not an email"))
            .buildPartial()

        val error = user.validate()
        error.shouldBePresent()

        val violations = error.get().constraintViolationList

        violations.size shouldBe 2
        violations[0].fieldName shouldBe "contact"
    }

    @Test
    fun `invoke generated validation if no external validation is defined`() {
        val address = ShippingAddress.newBuilder()
            .setSecondLine("first line is required and not set")
            .buildPartial()
        val user = User.newBuilder()
            .addShippingAddress(address)
            .buildPartial()

        val error = user.validate()
        error.shouldBePresent()

        val violations = error.get().constraintViolationList

        violations.size shouldBe 1
        violations[0].fieldName shouldBe "shipping_address"
    }

    @Test
    fun `ignore external constraints if '(validate)' is not set`() {
        val name = SimplePersonName.newBuilder()
            .setValue("A")
            .buildPartial()
        val user = User.newBuilder()
            .setName(name)
            .buildPartial()

        user.validate().shouldBeEmpty()
    }
}
