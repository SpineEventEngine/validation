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

package io.spine.validate

import com.google.common.testing.NullPointerTester
import com.google.protobuf.Message
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.base.Field
import io.spine.base.Time
import io.spine.code.proto.FieldContext
import io.spine.test.type.PersonName
import io.spine.test.validate.Passport
import io.spine.test.validate.RequiredStringValue
import io.spine.testing.UtilityClassTest
import io.spine.testing.logging.mute.MuteLogging
import io.spine.type.TypeName
import io.spine.validate.Validate.checkValidChange
import io.spine.validate.Validate.violationsOf
import io.spine.validate.Validate.violationsOfCustomConstraints
import io.spine.validate.diags.ViolationText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`Validate` utility class should")
internal class ValidateUntilitySpec : UtilityClassTest<Validate>(Validate::class.java) {

    override fun configure(tester: NullPointerTester) {
        super.configure(tester)
        tester.setDefault(Message::class.java, Time.currentTime())
            .setDefault(FieldContext::class.java, FieldContext.empty())
    }

    @Test
    fun `run custom validation and obtain no violations if there are no custom constraints`() {
        val message = RequiredStringValue.getDefaultInstance()
        val violations = violationsOf(message)
        val customViolations = violationsOfCustomConstraints(message)

        violations shouldHaveSize 1
        customViolations.shouldBeEmpty()
    }

    @Test
    fun `format message from constraint violation`() {
        val violation = ConstraintViolation.newBuilder()
            .setMsgFormat("test %s test %s")
            .addParam("1")
            .addParam("2")
            .build()
        val formatted = ViolationText.of(violation).toString()

        formatted shouldBe "test 1 test 2"
    }

    @MuteLogging
    @Nested
    @DisplayName("test message changes upon `(set_once)` and")
    internal inner class SetOnce {

        @Test
        fun `throw 'ValidationException' with several violations`() {
            val oldValue = Passport.newBuilder()
                .setId("MT 111")
                .setBirthplace("London")
                .build()
            val newValue = Passport.newBuilder()
                .setId("JC 424")
                .setBirthplace("Edinburgh")
                .build()
            checkViolated(oldValue, newValue, BIRTHPLACE)
        }

        @Test
        fun `allow overriding if '(set_once) = false'`() {
            val id = "JB 007"
            val oldValue = Passport.newBuilder()
                .setId(id)
                .build()
            val name = PersonName.newBuilder()
                .setGivenName("John")
                .setFamilyName("Doe")
                .build()
            val newValue = Passport.newBuilder()
                .setId(id)
                .setName(name)
                .build()
            checkValidChange(oldValue, newValue)
        }

        private fun checkViolated(oldValue: Passport, newValue: Passport, vararg fields: String) {
            val exception = assertThrows<ValidationException> {
                checkValidChange(oldValue, newValue)
            }
            val violations = exception.constraintViolations
            violations shouldHaveSize fields.size

            for (i in fields.indices) {
                val violation = violations[i]
                val field = fields[i]

                violation!!.msgFormat shouldContain "(set_once)"

                val expectedTypeName = TypeName.of(newValue).value()
                violation.typeName shouldContain expectedTypeName

                violation.fieldPath shouldBe Field.parse(field).path()
            }
        }
    }

    companion object {
        private const val BIRTHPLACE = "birthplace"
    }
}
