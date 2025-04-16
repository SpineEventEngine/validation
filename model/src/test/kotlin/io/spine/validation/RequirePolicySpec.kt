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

package io.spine.validation

import io.kotest.matchers.string.shouldContain
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.protobuf.field
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`RequirePolicy` should")
internal class RequirePolicySpec : CompilationErrorTest() {

    @Test
    fun `reject a boolean field`() {
        val message = RequireWithBool.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("bool_field")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `reject a signed integer field`() {
        val message = RequireWithSignedInt.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("int32_field")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `reject an unsigned integer field`() {
        val message = RequireWithUnsignedInt.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("uint64_field")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `reject a double field`() {
        val message = RequireWithDouble.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("double_field")
        error.message shouldContain unsupportedFieldType(field)
    }
}

private fun unsupportedFieldType(field: Field) =
    "The field type `${field.type.name}` of the `${field.qualifiedName}` is not supported" +
            " by the `($REQUIRE)` option."
