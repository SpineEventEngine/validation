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
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.protobuf.descriptor
import io.spine.protodata.protobuf.field
import kotlin.reflect.KClass
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`RangePolicy` should")
internal class RangePolicySpec : CompilationErrorTest() {

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithUnsupportedFieldType")
    @ParameterizedTest(name = "reject when the field type is `{0}`")
    fun rejectWhenFieldHasUnsupportedType(message: KClass<out Message>) {
        val descriptor = message.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.type.name)
            shouldContain(field.qualifiedName)
            shouldContain("is not supported")
        }
    }

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithInvalidDelimiters")
    @ParameterizedTest(name = "reject the range with an invalid delimiter")
    fun rejectInvalidDelimiters(message: KClass<out Message>) {
        val descriptor = message.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain("could not parse the range")
            shouldContain(field.qualifiedName)
            shouldContain("The lower and upper bounds should be separated")
        }
    }

    @Test
    fun `reject the range with an invalid opening symbol`() {
        val descriptor = RangeInvalidOpening::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain("could not parse the range")
            shouldContain(field.qualifiedName)
            shouldContain("The lower bound should begin either")
        }
    }

    @Test
    fun `reject the range with an invalid closing symbol`() {
        val descriptor = RangeInvalidClosing::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain("could not parse the range")
            shouldContain(field.qualifiedName)
            shouldContain("The upper bound should end either")
        }
    }

    @Test
    fun `reject the range with integer values on 'double' field`() {
        var descriptor = RangeInvalidLeftInt::class.descriptor
        var error = assertCompilationFails(descriptor)
        var field = descriptor.field("value")
        error.message.run {
            shouldContain("could not parse the range")
            shouldContain(field.qualifiedName)
            shouldContain("The lower bound should be within the range of the field")
        }

        descriptor = RangeInvalidRightInt::class.descriptor
        error = assertCompilationFails(descriptor)
        field = descriptor.field("value")
        error.message.run {
            shouldContain("could not parse the range")
            shouldContain(field.qualifiedName)
            shouldContain("The upper bound should be within the range of the field")
        }
    }

    @Test
    fun `reject the range with floating-point values on integer field`() {
        var descriptor = RangeInvalidLeftInt::class.descriptor
        var error = assertCompilationFails(descriptor)
        var field = descriptor.field("value")
        error.message.run {
            shouldContain("could not parse the range")
            shouldContain(field.qualifiedName)
            shouldContain("The lower bound should be within the range of the field")
        }

        descriptor = RangeInvalidRightInt::class.descriptor
        error = assertCompilationFails(descriptor)
        field = descriptor.field("value")
        error.message.run {
            shouldContain("could not parse the range")
            shouldContain(field.qualifiedName)
            shouldContain("The upper bound should be within the range of the field")
        }
    }
}
