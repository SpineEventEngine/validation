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
import io.kotest.matchers.string.shouldInclude
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.protobuf.field
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`RequiredPolicy` should")
internal class RequiredPolicySpec : CompilationErrorTest() {

    @Test
    fun `reject option on a boolean field`() {
        val message = RequiredBoolField.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("really")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `reject option on an integer field`() {
        val message = RequiredIntField.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("zero")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `reject option on a signed integer field`() {
        val message = RequiredSignedInt.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("signed")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `reject option on a double field`() {
        val message = RequiredDoubleField.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("temperature")
        error.message shouldContain unsupportedFieldType(field)
    }
}

@DisplayName("`IfMissingPolicy` should")
internal class IfMissingPolicySpec : CompilationErrorTest() {

    @Test
    fun `reject without '(required)'`() {
        val message = IfMissingWithoutRequired.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain(IF_MISSING)
            shouldContain(REQUIRED)
        }
    }

    @Test
    fun `reject unsupported placeholders`() {
        val message = IfMissingWithInvalidPlaceholders.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain(IF_MISSING)
            shouldContain("with unsupported placeholders")
            shouldInclude("[field.name, field.value]")
        }
    }
}

private fun unsupportedFieldType(field: Field) =
    "The field type `${field.type.name}` of the `${field.qualifiedName}` is not supported" +
            " by the `($REQUIRED)` option."
