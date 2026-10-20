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

package io.spine.tools.validation.test

import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.tools.compiler.protobuf.descriptor
import io.spine.validation.ConstraintViolation
import io.spine.validation.ValidationException
import java.util.UUID
import kotlin.random.Random
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * This test suite verifies that a custom message validator [EarphonesValidator]
 * works when validating messages containing [Earphones] fields.
 */
@DisplayName("A message validator should")
class ValidatorIgTest {

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

private fun earphones(): Earphones = earphones {
    modelName = "SN532"
    manufacturer = UUID.randomUUID().toString()
    price = Random.nextDouble() * 100
}
