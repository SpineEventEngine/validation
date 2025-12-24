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

package io.spine.test

import com.google.protobuf.Message
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldNotBe
import io.spine.test.protobuf.CardNumber
import io.spine.validation.NonValidated
import io.spine.validation.ValidatableMessage
import io.spine.validation.Validate.check
import io.spine.validation.ValidatingBuilder
import io.spine.validation.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("Generated Java code for a message should")
internal class JavaMessageSmokeTest {

    /**
     * The builder which should produce a valid instance.
     */
    private val valid: CardNumber.Builder =
        CardNumber.newBuilder().setDigits("0000 0000 0000 0000")

    /**
     * The builder which should case error on `build()`.
     */
    private val invalid: CardNumber.Builder =
        CardNumber.newBuilder().setDigits("zazazazazazaz")

    @Test
    fun `create a valid message using 'build()'`() {
        assertDoesNotThrow {
            val number = valid.build()
            check(number)
        }
    }

    @Test
    fun `throw 'ValidationException' if the message is not valid`() {
        val exception = assertThrows<ValidationException> {
            invalid.build()
        }
        exception.constraintViolations.shouldNotBeEmpty()
    }

    @Test
    fun `ignore invalid message when skipping validation intentionally via 'buildPartial'`() {
        val number: @NonValidated Message = invalid.buildPartial()
        number shouldNotBe null
        assertThrows<ValidationException> {
            check(number)
        }
    }

    @Test
    fun `make the message implement 'ValidatableMessage'`() {
        CardNumber::class.java.interfaces shouldContain ValidatableMessage::class.java
    }

    @Test
    fun `make the message builder implement 'ValidatingBuilder'`() {
        CardNumber.Builder::class.java.interfaces shouldContain ValidatingBuilder::class.java
    }
}
