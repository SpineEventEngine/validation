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
                singularWellKnownMessage {
                    value = timestamp
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe 1

            val violation = violations.first()
            violation.assert<SingularWellKnownMessage>(timestamp)
        }

        @Test
        fun `of a repeated field`() {
            val timestamps = List(3) { Timestamps.now() }
            val exception = assertThrows<ValidationException> {
                repeatedWellKnownMessage {
                    value.addAll(timestamps)
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe timestamps.size

            violations.forEachIndexed { index, violation ->
                val timestamp = timestamps[index]
                violation.assert<RepeatedWellKnownMessage>(timestamp)
            }
        }

        @Test
        fun `of a map field`() {
            val timestamps = List(3) { "Timestamp #$it" }
                .associateWith { Timestamps.now() }
            val exception = assertThrows<ValidationException> {
                mappedWellKnownMessage {
                    value.putAll(timestamps)
                }
            }

            val violations = exception.constraintViolations
            violations.size shouldBe timestamps.size

            violations.forEachIndexed { index, violation ->
                val timestamp = timestamps["Timestamp #$index"]!!
                violation.assert<MappedWellKnownMessage>(timestamp)
            }
        }
    }

    @Nested inner class
    `allow valid instances` {

        @Test
        fun `of a singular field`() {
            assertDoesNotThrow {
                singularWellKnownMessage {
                    value = ValidTimestamp
                }
            }
        }

        @Test
        fun `of a repeated field`() {
            val timestamps = List(3) { ValidTimestamp }
            assertDoesNotThrow {
                repeatedWellKnownMessage {
                    value.addAll(timestamps)
                }
            }
        }

        @Test
        fun `of a map field`() {
            val timestamps = List(3) { "Timestamp #$it" }
                .associateWith { ValidTimestamp }
            assertDoesNotThrow {
                mappedWellKnownMessage {
                    value.putAll(timestamps)
                }
            }
        }
    }
}

/**
 * Asserts that this [ConstraintViolation] has all required fields populated
 * in accordance to [TimestampValidator].
 */
private inline fun <reified T : Message> ConstraintViolation.assert(timestamp: Timestamp) {
    message shouldBe TimestampValidator.Violation.message
    fieldPath shouldBe expectedFieldPath
    typeName shouldBe T::class.descriptor.fullName
    fieldValue shouldBe toAny(timestamp.seconds)
}

private val expectedFieldPath = FieldPath("value").toBuilder()
    .mergeFrom(TimestampValidator.Violation.fieldPath)
    .build()
