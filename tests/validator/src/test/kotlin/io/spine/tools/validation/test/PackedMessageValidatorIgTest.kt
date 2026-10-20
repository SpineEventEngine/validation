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

import com.google.protobuf.util.Timestamps
import io.spine.protobuf.pack
import io.spine.validation.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("A validator for messages packed into `Any` should")
internal class PackedMessageValidatorIgTest {

    /**
     * This is going to be invalid because of [TheOnlyTimeValid] validator
     * available in the classpath.
     */
    private val invalid = Timestamps.now().pack()

    private val valid = TheOnlyTimeValid.ValidTimestamp.pack()

    @Test
    fun `validate a singular field`() {
        assertThrows<ValidationException> {
            packedFields {
                singular = invalid
            }
        }
        assertDoesNotThrow {
            packedFields {
                singular = valid
            }
        }
    }

    @Test
    fun `validate a repeated field`() {
        assertThrows<ValidationException> {
            packedFields {
                repeated.addAll(listOf(valid, valid, invalid))
            }
        }
        assertDoesNotThrow {
            packedFields {
                repeated.addAll(listOf(valid, valid, valid))
            }
        }
    }

    @Test
    fun `validate a map field`() {
        assertThrows<ValidationException> {
            packedFields {
                mapped.put("1", valid)
                mapped.put("2", valid)
                mapped.put("3", invalid)
            }
        }
        assertDoesNotThrow {
            packedFields {
                mapped.put("1", valid)
                mapped.put("2", valid)
                mapped.put("3", valid)
            }
        }
    }
}
