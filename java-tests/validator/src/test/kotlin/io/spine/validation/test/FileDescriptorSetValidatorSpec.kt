package io.spine.validation.test

import com.google.protobuf.DescriptorProtos
import io.spine.validate.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`FileDescriptorSetValidator` should")
class FileDescriptorSetValidatorSpec {

    @Test
    fun `prohibit any instances but one`() {
        assertThrows<ValidationException> {
            validatorDescriptorSet {
                set = DescriptorProtos.FileDescriptorSet
                    .newBuilder()
                    .build()
            }
        }
        assertDoesNotThrow {
            validatorDescriptorSet {
                set = FileDescriptorSetValidator.ValidSet
            }
        }
    }
}
