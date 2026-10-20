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

package io.spine.test.options.goes

import com.google.protobuf.Message
import io.spine.protobuf.newBuilder
import io.spine.tools.compiler.protobuf.descriptor
import io.spine.test.options.set
import io.spine.validation.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests how `(goes)` option sets up a one-way dependency between two fields.
 *
 * For example:
 *
 * ```
 * string title = 1;
 * bytes text = 2 [(goes).with = "title"];
 * ```
 */
@DisplayName("`(goes)` constraint should")
internal class GoesOneWayIgTest {

    @MethodSource("io.spine.test.options.goes.GoesOneWayTestEnv#onlyTargetFields")
    @ParameterizedTest(name = "throw if only the target `{1}` field is set")
    fun throwIfOnlyTargetFieldSet(message: Class<Message>, fieldName: String, fieldValue: Any) {
        val descriptor = message.descriptor
        val field = descriptor.findFieldByName(fieldName)!!
        assertThrows<ValidationException> {
            message.newBuilder()
                .set(field, fieldValue)
                .build()
        }
    }

    @MethodSource("io.spine.test.options.goes.GoesOneWayTestEnv#onlyCompanionFields")
    @ParameterizedTest(name = "pass if only the companion `{1}` field is set")
    fun passIfOnlyCompanionFieldSet(
        message: Class<out Message>,
        fieldName: String,
        fieldValue: Any
    ) {
        val descriptor = message.descriptor
        val companionField = descriptor.findFieldByName(fieldName)!!
        assertDoesNotThrow {
            message.newBuilder()
                .set(companionField, fieldValue)
                .build()
        }
    }

    @Suppress("MaxLineLength") // So not to wrap the test name.
    @MethodSource("io.spine.test.options.goes.GoesOneWayTestEnv#bothTargetAndCompanionFields")
    @ParameterizedTest(name = "pass if both the target `{1}` and its companion `{3}` fields are set")
    fun passIfBothTargetAndCompanionFieldsSet(
        message: Class<out Message>,
        companionName: String,
        companionValue: Any,
        fieldName: String,
        fieldValue: Any
    ) {
        val descriptor = message.descriptor
        val field = descriptor.findFieldByName(fieldName)!!
        val companionField = descriptor.findFieldByName(companionName)!!
        assertDoesNotThrow {
            message.newBuilder()
                .set(field, fieldValue)
                .set(companionField, companionValue)
                .build()
        }
    }
}
