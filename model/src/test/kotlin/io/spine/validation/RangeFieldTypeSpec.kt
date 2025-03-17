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
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.protobuf.field
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`RangePolicy` should reject when the option is applied to")
internal class RangeFieldTypeSpec : CompilationErrorTest() {

    @Test
    fun `'string' field`() {
        val message = RangeOnString.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `'bool' field`() {
        val message = RangeOnBool.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `'bytes' field`() {
        val message = RangeOnBytes.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `the message field`() {
        val message = RangeOnMessage.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `the enum field`() {
        val message = RangeOnEnum.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message shouldContain unsupportedFieldType(field)
    }

    @Test
    fun `the map field`() {
        val message = RangeOnMap.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message shouldContain unsupportedFieldType(field)
    }
}

private fun unsupportedFieldType(field: Field) =
    "The field type `${field.type}` of `${field.qualifiedName}` is not supported" +
            " by the `($RANGE)` option. Supported field types: numbers and repeated" +
            " of numbers."
