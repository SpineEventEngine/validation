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

package io.spine.test.options.goes

import com.google.protobuf.Message
import io.spine.test.options.set
import io.spine.validate.ValidationException
import io.spine.validation.newBuilder
import io.spine.validation.protodata.getDescriptor
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
internal class GoesOneWayITest {

    @MethodSource("io.spine.test.options.goes.GoesOneWayTestEnv#onlyTargetFields")
    @ParameterizedTest(name = "throw if only the target `{1}` field is set")
    fun throwIfOnlyTargetFieldSet(message: Class<Message>, fieldName: String, fieldValue: Any) {
        val descriptor = message.getDescriptor()
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
        val descriptor = message.getDescriptor()
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
        val descriptor = message.getDescriptor()
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
