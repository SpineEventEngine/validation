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

package io.spine.test.options.goes.given

import com.google.protobuf.ByteString
import com.google.protobuf.util.Timestamps
import io.spine.test.tools.validate.EnumForGoes
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parameterized [io.spine.test.options.goes.GoesErrorMessagesITest].
 */
@Suppress("unused") // Data provider for parameterized test.
internal object GoesMessagesTestEnv {

    const val COMPANION_FIELD_NAME = "companion"

    private val fieldValues = listOf(
        "message" to Timestamps.now(),
        "enum" to EnumForGoes.EFG_ITEM1.valueDescriptor,
        "string" to "some companion text",
        "bytes" to ByteString.copyFromUtf8("some companion data"),
        "repeated" to listOf(1L, 2L, 3L),
        "map" to mapOf("key" to 32),
    )

    /**
     * Test data for the following tests:
     *
     * 1. [io.spine.test.options.goes.GoesErrorMessagesITest.showDefaultErrorMessage].
     * 1. [io.spine.test.options.goes.GoesErrorMessagesITest.showCustomErrorMessage].
     */
    @JvmStatic
    fun onlyTargetFields() = fieldValues.map { (fieldType, fieldValue) ->
        val fieldName = "${fieldType}_field"
        arguments(named(fieldType, fieldName), fieldValue)
    }
}
