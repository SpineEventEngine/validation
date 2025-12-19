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
 * Tests how `(goes)` option sets up a mutual dependency between two fields.
 *
 * For example:
 *
 * ```
 * string title = 1 [(goes].with = "text"];
 * bytes text = 2 [(goes).with = "title"];
 * ```
 */
@DisplayName("`(goes)` constraint should")
internal class GoesMutualITest {

    @Suppress("MaxLineLength") // So not to wrap the test display name.
    @MethodSource("io.spine.test.options.goes.GoesMutualTestEnv#interdependentFields")
    @ParameterizedTest(name = "throw if one of mutually dependent `{1}` and `{3}` fields is not set")
    fun throwIfOneOfMutuallyDependentFieldsNotSet(
        message: Class<out Message>,
        fieldName1: String,
        fieldValue1: Any,
        fieldName2: String,
        fieldValue2: Any
    ) {
        val descriptor = message.descriptor
        val field1 = descriptor.findFieldByName(fieldName1)!!
        assertThrows<ValidationException> {
            message.newBuilder()
                .set(field1, fieldValue1)
                .build()
        }
        val field2 = descriptor.findFieldByName(fieldName2)!!
        assertThrows<ValidationException> {
            message.newBuilder()
                .set(field2, fieldValue2)
                .build()
        }
    }

    @MethodSource("io.spine.test.options.goes.GoesMutualTestEnv#interdependentFields")
    @ParameterizedTest(name = "pass if both mutually dependent `{1}` and `{3}` fields are set")
    fun passIfBothMutuallyDependentFieldsSet(
        message: Class<out Message>,
        fieldName1: String,
        fieldValue1: Any,
        fieldName2: String,
        fieldValue2: Any
    ) {
        val descriptor = message.descriptor
        val field1 = descriptor.findFieldByName(fieldName1)!!
        val field2 = descriptor.findFieldByName(fieldName2)!!
        assertDoesNotThrow {
            message.newBuilder()
                .set(field1, fieldValue1)
                .set(field2, fieldValue2)
                .build()
        }
    }

    @Suppress("MaxLineLength") // Due to a long method source.
    @MethodSource("io.spine.test.options.goes.GoesMutualTestEnv#messagesWithInterdependentFields")
    @ParameterizedTest(name = "pass if both mutually dependent fields are not set")
    fun passIfBothMutuallyDependentFieldsNotSet(message: Class<out Message>) {
        assertDoesNotThrow {
            message.newBuilder()
                .build()
        }
    }
}
