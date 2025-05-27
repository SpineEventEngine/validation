package io.spine.validation.test

import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.protodata.protobuf.descriptor
import io.spine.validate.ConstraintViolation
import io.spine.validate.ValidationException
import io.spine.validation.test.TimestampValidator.Companion.ValidTimestamp
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`TimestampValidator` should")
class TimestampValidatorSpec {

    @Nested inner class
    `prohibit invalid instances` {

        @Test
        fun `of a singular field`() {
            val timestamp = Timestamps.now()
            val exception = assertThrows<ValidationException> {
                singularExternalMessage {
                    value = timestamp
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe 1

            val violation = violations.first()
            violation.assert<SingularExternalMessage>(timestamp)
        }

        @Test
        fun `of a repeated field`() {
            val timestamps = List(3) { Timestamps.now() }
            val exception = assertThrows<ValidationException> {
                repeatedExternalMessage {
                    value.addAll(timestamps)
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe timestamps.size

            violations.forEachIndexed { index, violation ->
                val timestamp = timestamps[index]
                violation.assert<RepeatedExternalMessage>(timestamp)
            }
        }

        @Test
        fun `of a map field`() {
            val timestamps = List(3) { "Timestamp #$it" }
                .associateWith { Timestamps.now() }
            val exception = assertThrows<ValidationException> {
                mappedExternalMessage {
                    value.putAll(timestamps)
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe timestamps.size

            violations.forEachIndexed { index, violation ->
                val timestamp = timestamps["Timestamp #$index"]!!
                violation.assert<MappedExternalMessage>(timestamp)
            }
        }
    }

    @Nested inner class
    `allow valid instances` {

        @Test
        fun `of a singular field`() {
            assertDoesNotThrow {
                singularExternalMessage {
                    value = ValidTimestamp
                }
            }
        }

        @Test
        fun `of a repeated field`() {
            val timestamps = List(3) { ValidTimestamp }
            assertDoesNotThrow {
                repeatedExternalMessage {
                    value.addAll(timestamps)
                }
            }
        }

        @Test
        fun `of a map field`() {
            val timestamps = List(3) { "Timestamp #$it" }
                .associateWith { ValidTimestamp }
            assertDoesNotThrow {
                mappedExternalMessage {
                    value.putAll(timestamps)
                }
            }
        }
    }
}

/**
 * Asserts that this [ConstraintViolation] has all required fields populated.
 */
private inline fun <reified T : Message> ConstraintViolation.assert(timestamp: Timestamp) {
    message shouldBe TimestampValidator.Violation.message
    fieldPath shouldBe expectedFieldPath
    typeName shouldBe T::class.descriptor.fullName
    fieldValue shouldBe toAny(timestamp.seconds)
}

/**
 * The field path consists of the field name that contains the external message type
 * plus the path relative to the message type.
 *
 * In test stubs, all fields containing external message types are named `value`.
 * The relative part is provided by the tested validator.
 */
private val expectedFieldPath = FieldPath("value").toBuilder()
    .mergeFrom(TimestampValidator.Violation.fieldPath)
    .build()
