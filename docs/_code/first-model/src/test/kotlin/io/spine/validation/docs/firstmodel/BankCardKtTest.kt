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

package io.spine.validation.docs.firstmodel

import io.spine.validation.ValidationException
import io.spine.validation.format
import org.junit.jupiter.api.Test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.string.shouldContain

class BankCardKtTest {

    @Test
    fun `throw ValidationException if digits are invalid`() {
        // #docfragment "invalid-digits"
        shouldThrow<ValidationException> {
            bankCard {
                digits = "invalid"
                owner = "ALEX SMITH"
            }
        }
        // #enddocfragment "invalid-digits"
    }

    @Test
    fun `throw ValidationException if owner is invalid`() {
        shouldThrow<ValidationException> {
            bankCard {
                digits = "1234 5678 1234 5678"
                owner = "Al"
            }
        }
    }

    @Test
    fun `throw ValidationException if tags are not distinct`() {
        shouldThrow<ValidationException> {
            bankCard {
                digits = "1234 5678 1234 5678"
                owner = "ALEX SMITH"
                tags.add("personal")
                tags.add("personal")
            }
        }
    }

    @Test
    fun `be built if all fields are valid`() {
        shouldNotThrowAny {
            bankCard {
                digits = "1234 5678 1234 5678"
                owner = "ALEX SMITH"
                tags.add("personal")
                tags.add("travel")
            }
        }
    }

    @Test
    fun `allow multiple words in the owner name`() {
        shouldNotThrowAny {
            bankCard {
                digits = "1234 5678 1234 5678"
                owner = "John Jacob Jingleheimer Schmidt"
            }
        }
    }

    @Test
    fun `provide a formatted error message for an invalid card`() {
        // #docfragment "error-message"
        val card = BankCard.newBuilder()
            .setOwner("ALEX SMITH")
            .setDigits("wrong number")
            .buildPartial() // There is no Kotlin DSL for this.
        val error = card.validate()
        error.shouldBePresent()

        val violation = error.get().constraintViolationList[0]
        val formatted = violation.message.format()

        formatted shouldContain "digits"
        formatted shouldContain "wrong number"
        // #enddocfragment "error-message"
    }
}
