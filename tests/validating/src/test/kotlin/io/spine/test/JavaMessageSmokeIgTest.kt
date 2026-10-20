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

package io.spine.test

import com.google.protobuf.Message
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
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
internal class JavaMessageSmokeIgTest {

    /**
     * The builder which should produce a valid instance.
     */
    private val valid: CardNumber.Builder =
        CardNumber.newBuilder().setDigits("0000 0000 0000 0000")

    /**
     * The builder which should cause an error on `build()`.
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
    fun `report no violations via 'validate()' when the builder content is valid`() {
        valid.validate().shouldBeEmpty()
    }

    @Test
    fun `report violations via 'validate()' without throwing when the content is invalid`() {
        val violations = assertDoesNotThrow {
            invalid.validate()
        }
        violations.shouldNotBeEmpty()
    }

    @Test
    fun `keep the builder content intact when probing via 'validate()'`() {
        val digitsBefore = invalid.digits

        invalid.validate()

        invalid.digits shouldBe digitsBefore
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
