/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.validation.test

import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.tools.compiler.protobuf.descriptor
import io.spine.validate.ConstraintViolation
import io.spine.validate.ValidationException
import java.util.UUID
import kotlin.random.Random
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`EarphonesValidator` should")
class EarphonesValidatorSpec {

    @Nested inner class
    `prohibit invalid instances` {

        @Test
        fun `of a singular field`() {
            val earphones = earphones()
            val exception = assertThrows<ValidationException> {
                singularDependencyMessage {
                    value = earphones
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe 1

            val violation = violations.first()
            violation.assert<SingularDependencyMessage>(earphones)
        }

        @Test
        fun `of a repeated field`() {
            val earphones = List(3) { earphones() }
            val exception = assertThrows<ValidationException> {
                repeatedDependencyMessage {
                    value.addAll(earphones)
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe earphones.size

            violations.forEachIndexed { index, violation ->
                val timestamp = earphones[index]
                violation.assert<RepeatedDependencyMessage>(timestamp)
            }
        }

        @Test
        fun `of a map field`() {
            val earphones = List(3) { "Earphones #$it" }
                .associateWith { earphones() }
            val exception = assertThrows<ValidationException> {
                mappedDependencyMessage {
                    value.putAll(earphones)
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe earphones.size

            violations.forEachIndexed { index, violation ->
                val timestamp = earphones["Earphones #$index"]!!
                violation.assert<MappedDependencyMessage>(timestamp)
            }
        }
    }

    @Nested inner class
    `allow valid instances` {

        @Test
        fun `of a singular field`() {
            assertDoesNotThrow {
                singularDependencyMessage {
                    value = EarphonesValidator.ValidEarphones
                }
            }
        }

        @Test
        fun `of a repeated field`() {
            val timestamps = List(3) { EarphonesValidator.ValidEarphones }
            assertDoesNotThrow {
                repeatedDependencyMessage {
                    value.addAll(timestamps)
                }
            }
        }

        @Test
        fun `of a map field`() {
            val timestamps = List(3) { "Earphones #$it" }
                .associateWith { EarphonesValidator.ValidEarphones }
            assertDoesNotThrow {
                mappedDependencyMessage {
                    value.putAll(timestamps)
                }
            }
        }
    }
}

/**
 * Asserts that this [ConstraintViolation] has all required fields populated
 * in accordance to [EarphonesValidator].
 */
private inline fun <reified T : Message> ConstraintViolation.assert(earphones: Earphones) {
    message shouldBe EarphonesValidator.Violation.message
    fieldPath shouldBe expectedFieldPath
    typeName shouldBe T::class.descriptor.fullName
    fieldValue shouldBe toAny(earphones.price)
}

private val expectedFieldPath = FieldPath("value").toBuilder()
    .mergeFrom(EarphonesValidator.Violation.fieldPath)
    .build()

private fun earphones() = earphones {
    modelName = "SN532"
    manufacturer = UUID.randomUUID().toString()
    price = Random.nextDouble() * 100
}
