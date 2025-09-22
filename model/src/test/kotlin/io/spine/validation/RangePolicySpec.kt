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
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import kotlin.reflect.KClass
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`RangePolicy` should reject the range")
internal class RangePolicySpec : CompilationErrorTest() {

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithUnsupportedFieldType")
    @ParameterizedTest(name = "when the field type is `{0}`")
    fun whenFieldHasUnsupportedType(message: KClass<out Message>) =
        assertCompilationFails(message) { field ->
            shouldContain(field.type.name)
            shouldContain(field.qualifiedName)
            shouldContain("a filed of an unsupported type")
        }

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithInvalidDelimiters")
    @ParameterizedTest(name = "with an invalid delimiter in `{0}`")
    fun withInvalidDelimiter(message: KClass<out Message>) =
        assertCompilationFails(message) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("the lower and upper bounds must be separated either with")
        }

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithOverflowValues")
    @ParameterizedTest(name = "with a bound value causing an overflow in `{0}`")
    fun withOverflowValue(message: KClass<out Message>, value: String) =
        assertCompilationFails(message) { field ->
            shouldContain(field.qualifiedName)
            shouldContain(value)
            shouldContain("value is out of range")
        }

    @MethodSource("io.spine.validation.RangePolicyTestEnv#messagesWithLowerEqualOrMoreThanUpper")
    @ParameterizedTest(name = "with the lower bound is equal or more than the upper one in `{0}`")
    fun withLowerEqualOrMoreThanUpper(message: KClass<out Message>, value: String) =
        assertCompilationFails(message) { field ->
            shouldContain(field.qualifiedName)
            shouldContain(value)
            shouldContain("must be less than the upper bound")
        }

    @Test
    fun `with an invalid opening symbol`() =
        assertCompilationFails(RangeInvalidOpening::class) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("the lower bound must begin either with")
        }

    @Test
    fun `with an invalid closing symbol`() =
        assertCompilationFails(RangeInvalidClosing::class) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("the upper bound must end either with")
        }

    @Test
    fun `with integer number specified for lower bound of 'float' field`() =
        assertCompilationFails(RangeInvalidLowerFloat::class) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("Value: `0`")
            shouldContain("a floating-point number is required for this field type")
        }

    @Test
    fun `with integer number specified for upper bound of 'float' field`() =
        assertCompilationFails(RangeInvalidUpperFloat::class) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("Value: `15`")
            shouldContain("a floating-point number is required for this field type")
        }

    @Test
    fun `with integer number specified for lower bound of 'double' field`() =
        assertCompilationFails(RangeInvalidLowerDouble::class) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("Value: `0`")
            shouldContain("a floating-point number is required for this field type")
        }

    @Test
    fun `with integer number specified for upper bound of 'double' field`() =
        assertCompilationFails(RangeInvalidUpperDouble::class) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("Value: `15`")
            shouldContain("a floating-point number is required for this field type")
        }

    @Test
    fun `with floating-point number specified for lower bound of 'int32' field`() =
        assertCompilationFails(RangeInvalidLowerInt::class) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("Value: `0.0`") // The bound value.
            shouldContain("an integer number is required for this field type")
        }

    @Test
    fun `with floating-point number specified for upper bound of 'int32' field`() =
        assertCompilationFails(RangeInvalidUpperInt::class) { field ->
            shouldContain(field.qualifiedName)
            shouldContain("Value: `15.0`") // The bound value.
            shouldContain("an integer number is required for this field type")
        }

    @Test
    fun `with unsupported placeholders in the error message`() =
        assertCompilationFails(RangeWithInvalidPlaceholders::class) { field ->
            shouldContain(RANGE)
            shouldContain(field.qualifiedName)
            shouldContain("unsupported placeholders")
            shouldInclude("[field.name, range]")
        }

    @Test
    fun `with a non-existing field as a bound`() =
        assertCompilationFails(RangeWithNonExistingFieldBound::class) { field ->
            shouldContain(RANGE)
            shouldContain(field.qualifiedName)
            shouldContain("Field path: `timestamp.minutes`")
            shouldContain("make sure the field path refers to a valid field")
        }

    @Test
    fun `with a non-numeric field as a bound`() =
        assertCompilationFails(RangeWithNonNumericFieldBound::class) { field ->
            shouldContain(RANGE)
            shouldContain(field.qualifiedName)
            shouldContain("Field path: `error.type`")
            shouldContain("the specified field is not of a numeric type")
        }

    @Test
    fun `with a repeated field as a bound`() =
        assertCompilationFails(RangeWithRepeatedFieldBound::class) { field ->
            shouldContain(RANGE)
            shouldContain(field.qualifiedName)
            shouldContain("Field path: `error_code`")
            shouldContain("the specified field is not of a numeric type")
        }

    @Test
    fun `with self as a bound`() =
        assertCompilationFails(RangeWithSelfReferencing::class) { field ->
            shouldContain(RANGE)
            shouldContain(field.qualifiedName)
            shouldContain("self-referencing is not allowed")
        }
}
