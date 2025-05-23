package io.spine.validation.test

import com.google.protobuf.util.Timestamps
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`TimestampValidator` should")
class TimestampValidatorSpec {

    @Test
    fun `prohibit any instances but one`() {
        assertThrows<ValidationException> {
            validatorTimestamp {
                createdAt = Timestamps.now()
            }
        }
        assertDoesNotThrow {
            validatorTimestamp {
                createdAt = TimestampValidator.ValidTimestamp
            }
        }
    }
}
