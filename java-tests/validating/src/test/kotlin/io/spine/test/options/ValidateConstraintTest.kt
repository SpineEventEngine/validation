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
import io.spine.base.Identifier
import io.spine.test.tools.validate.Address
import io.spine.test.tools.validate.BinaryTree
import io.spine.test.tools.validate.DeliveryReceiver
import io.spine.test.tools.validate.EmailAddress
import io.spine.test.tools.validate.PersonName
import io.spine.test.tools.validate.PhoneNumber
import io.spine.test.tools.validate.Town
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@DisplayName("`(validate)` constraint should be compiled so that")
internal class ValidateConstraintTest {

    @Test
    fun `message fields are validated`() {
        val invalidName = PersonName.newBuilder()
            .setGivenName("Adam")
            .buildPartial()
        val invalidAddress = Address.newBuilder()
            .setSecondLine("Wall St. 1")
            .buildPartial()
        val msg = DeliveryReceiver.newBuilder()
            .setName(invalidName)
            .setAddress(invalidAddress)
            .buildPartial()

        val error = msg.validate()

        error.shouldBePresent()

        val violations = error.get().constraintViolationList

        // We should have 2 violations instead of 1.
        // But we do not handle the `(required_field)` option.
        // See https://github.com/SpineEventEngine/validation/issues/148
        violations.size shouldBe 1

        val receiverViolation = violations[0]
        receiverViolation!!.fieldPath.getFieldName(0) shouldBe "address"

        val nestedViolations = receiverViolation.violationList
        nestedViolations.size shouldBe 2
        nestedViolations[0].fieldPath.getFieldName(0) shouldBe "first_line"
        nestedViolations[1].fieldPath.getFieldName(0) shouldBe "town"
    }

    @Test
    @DisplayName("repeated message fields are validated and violations are stored separately")
    fun validateRepeated() {
        val name = PersonName.newBuilder().setGivenName("Eve")
        val invalidNumbers = arrayOf(
            PhoneNumber.getDefaultInstance(),
            PhoneNumber.newBuilder().setDigits("not a number").buildPartial(),
            PhoneNumber.newBuilder().setDigits("definitely not a number").buildPartial()
        )
        val town = Town.newBuilder()
            .setName("London")
            .setCountry("UK")
        val address = Address.newBuilder()
            .setFirstLine("Strand 42")
            .setTown(town)
        val msg = DeliveryReceiver.newBuilder()
            .setName(name)
            .setAddress(address)
            .addContact(invalidNumbers[0])
            .addContact(invalidNumbers[1])
            .addContact(invalidNumbers[2])
            .buildPartial()

        val error = msg.validate()

        error.shouldBePresent()

        val violations = error.get().constraintViolationList
        violations.size shouldBe 2

        for (violation in violations) {
            violation.violationList.size shouldBe 1
            violation.fieldPath.getFieldName(0) shouldBe "contact"
        }
    }

    @Test
    @DisplayName("recursive validation has an exit point")
    fun recursive() {
        val nonEmptyAny = Identifier.pack(Identifier.newUuid())
        val tree = BinaryTree.newBuilder()
            .setValue(nonEmptyAny)
            .setLeftChild(
                BinaryTree.newBuilder()
                    .setValue(nonEmptyAny)
            )
            // We don't have to set the `right_child` field because the field is not `(required)`.
            .build()

        val error = tree.validate()

        error.shouldBeEmpty()
    }

    @Test
    @DisplayName("not run validation if the option is `false`")
    fun ignoreIfFalse() {
        val name = PersonName.newBuilder().setGivenName("Shawn")
        val town = Town.newBuilder().setName("Oeiras").setCountry("Portugal")
        val invalidEmail = EmailAddress.newBuilder()
            .setValue("definitely not an email")
            .buildPartial()
        val address = Address.newBuilder().setFirstLine("Window St. 2").setTown(town)
        val msg = assertDoesNotThrow {
            // ... because the option `(validate)` is set to `false` on
            // the `email` field of the `DeliveryReceiver` message.
            DeliveryReceiver.newBuilder()
                .setName(name)
                .setAddress(
                    address
                )
                .addEmail(invalidEmail)
                .build()
        }

        msg.validate().shouldBeEmpty()
    }
}
