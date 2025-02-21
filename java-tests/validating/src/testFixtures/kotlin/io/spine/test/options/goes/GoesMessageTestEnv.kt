/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.spine.test.tools.validate.EnumForGoes
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parameterized [io.spine.test.options.goes.GoesErrorMessageITest].
 */
@Suppress("unused") // Data provider for parameterized test.
internal object GoesMessageTestEnv {

    const val COMPANION_FIELD_NAME = "companion"

    /**
     * Test data for the following tests:
     *
     * 1. [io.spine.test.options.goes.GoesErrorMessageITest.showDefaultErrorMessage].
     * 1. [io.spine.test.options.goes.GoesErrorMessageITest.showCustomErrorMessage].
     */
    @JvmStatic
    fun onlyTargetFields() = listOf(
        Field("message", Timestamp.getDescriptor().fullName, Timestamps.now()),
        Field("enum", EnumForGoes.getDescriptor().fullName, EnumForGoes.EFG_ITEM1),
        Field("string", "string", "some companion text"),
        Field("bytes", "bytes", ByteString.copyFromUtf8("some companion data")),
        Field("repeated", "repeated int64", listOf(1L, 2L, 3L)),
        Field("map", "map<string, int32>", mapOf("key" to 32)),
    ).map { arguments(named(it.name, "${it.name}_field"), it.type, it.value) }
}

private class Field(
    val name: String,
    val type: String,
    val value: Any,
)
