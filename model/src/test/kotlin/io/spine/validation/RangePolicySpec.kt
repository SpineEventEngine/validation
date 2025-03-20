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

@DisplayName("`RangePolicy` should reject the range")
internal class RangePolicySpec : CompilationErrorTest() {

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithUnsupportedFieldType")
    @ParameterizedTest(name = "when the field type is `{0}`")
    fun whenFieldHasUnsupportedType(message: KClass<out Message>) {
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
    @ParameterizedTest(name = "with an invalid delimiter in `{0}`")
    fun withInvalidDelimiters(message: KClass<out Message>) {
        val descriptor = message.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("The lower and upper bounds should be separated")
        }
    }

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithOverflowValues")
    @ParameterizedTest(name = "with a value causing an overflow in `{0}`")
    fun withOverflowValue(message: KClass<out Message>, value: String) {
        val descriptor = message.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain(value)
            shouldContain("bound value is out of range")
        }
    }

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithLowerEqualOrMoreThanUpper")
    @ParameterizedTest(name = "if the lower bound is equal or more than the upper one in `{0}`")
    fun withLowerEqualOrMoreThanUpper(message: KClass<out Message>, value: String) {
        val descriptor = message.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain(value)
            shouldContain("should be less than the upper")
        }
    }

    @Test
    fun `with an invalid opening symbol`() {
        val descriptor = RangeInvalidOpening::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("The lower bound should begin either")
        }
    }

    @Test
    fun `with an invalid closing symbol`() {
        val descriptor = RangeInvalidClosing::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("The upper bound should end either")
        }
    }

    @Test
    fun `with integer number specified for lower bound of 'float' field`() {
        val descriptor = RangeInvalidLowerFloat::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("`0` bound value has an invalid format")
            shouldContain("make sure the provided value is a floating-point number")
        }
    }

    @Test
    fun `with integer number specified for upper bound of 'float' field`() {
        val descriptor = RangeInvalidUpperFloat::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("`15` bound value has an invalid format")
            shouldContain("make sure the provided value is a floating-point number")
        }
    }

    @Test
    fun `with integer number specified for lower bound of 'double' field`() {
        val descriptor = RangeInvalidLowerDouble::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("`0` bound value has an invalid format")
            shouldContain("make sure the provided value is a floating-point number")
        }
    }

    @Test
    fun `with integer number specified for upper bound of 'double' field`() {
        val descriptor = RangeInvalidUpperDouble::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("`15` bound value has an invalid format")
            shouldContain("make sure the provided value is a floating-point number")
        }
    }

    @Test
    fun `with floating-point number specified for lower bound of 'int32' field`() {
        val descriptor = RangeInvalidLowerInt::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("`0.0` bound value has an invalid format")
            shouldContain("make sure the provided value is an integer number")
        }
    }

    @Test
    fun `with floating-point number specified for upper bound of 'int32' field`() {
        val descriptor = RangeInvalidUpperInt::class.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("value")
        error.message.run {
            shouldContain(field.qualifiedName)
            shouldContain("`15.0` bound value has an invalid format")
            shouldContain("make sure the provided value is an integer number")
        }
    }
}
