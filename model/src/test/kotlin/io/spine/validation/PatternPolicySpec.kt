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

import com.google.protobuf.Message
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldInclude
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.protobuf.descriptor
import io.spine.protodata.protobuf.field
import kotlin.reflect.KClass
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`PatternPolicy` should reject the option when")
internal class PatternPolicySpec : CompilationErrorTest() {

    @MethodSource("io.spine.validation.PatternPolicyTestEnv#messagesWithUnsupportedTarget")
    @ParameterizedTest(name = "when target field type is `{0}`")
    fun targetFieldHasUnsupportedType(message: KClass<out Message>) {
        val descriptor = message.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.type.name)
            shouldContain(field.qualifiedName)
            shouldContain("is not supported")
        }
    }

    @Test
    fun `when the error message contains unsupported placeholders`() {
        val message = PatternWithInvalidPlaceholders.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain(PATTERN)
            shouldContain("unsupported placeholders")
            shouldInclude("[field.name, pattern.value]")
        }
    }
}
