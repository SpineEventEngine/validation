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

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.spine.test.tools.validate.EnumForGoes
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parameterized [io.spine.test.options.goes.GoesViolationIgTest].
 */
@Suppress("unused") // Data provider for parameterized test.
internal object GoesViolationTestEnv {

    /**
     * Test data for the following tests:
     *
     * 1. [io.spine.test.options.goes.GoesViolationIgTest.useDefaultErrorMessage].
     * 1. [io.spine.test.options.goes.GoesViolationIgTest.useCustomErrorMessage].
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

private data class Field(
    val name: String,
    val type: String,
    val value: Any,
)
