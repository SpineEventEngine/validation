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

package io.spine.validation.option

import com.google.common.collect.ImmutableList
import com.google.errorprone.annotations.Immutable
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.spine.code.proto.FieldContext
import io.spine.test.validate.option.ATestMessageWithConstraint
import io.spine.test.validate.option.TestFieldOptionProto
import io.spine.test.validate.option.aTestMessage
import io.spine.test.validate.option.noValidationTestMessage
import io.spine.testing.TestValues.randomString
import io.spine.validation.Constraint
import io.spine.validation.ConstraintViolation
import io.spine.validation.CustomConstraint
import io.spine.validation.FieldValue
import io.spine.validation.MessageValue
import io.spine.validation.constraintViolation
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`FieldValidatingOption` should")
internal class FieldValidatingOptionSpec {

    @Test
    fun `return empty value if option is not present in external or field constraints`() {
        val msg = ATestMessageWithConstraint.getDefaultInstance()
        val value = MessageValue.atTopLevel(msg)
        val fieldValue = valueField(value)
        val maxLength = MaxLength()

        maxLength.valueFrom(fieldValue.context()).shouldBeEmpty()
    }

    @Test
    fun `return value if option is present in field option`() {
        val msg = aTestMessage {
            value = randomString()
        }
        val value = MessageValue.atTopLevel(msg)
        val fieldValue = valueField(value)
        val maxLength = MaxLength()

        maxLength.valueFrom(fieldValue.context()).shouldBePresent()
    }

    @Test
    fun `throw 'IllegalStateException' if a specified option is not a field option`() {
        val msg = ATestMessageWithConstraint.getDefaultInstance()
        val value = MessageValue.atTopLevel(msg)
        val fieldValue = valueField(value)
        val maxLength = MaxLength()

        assertThrows<IllegalStateException> {
            maxLength.optionValue(fieldValue.context())
        }
    }

    @Test
    fun `not validate field if option is not present in external or field constraints`() {
        val msg = noValidationTestMessage {
            value = randomString()
        }
        val value = MessageValue.atTopLevel(msg)
        val fieldValue = valueField(value)
        val maxLength = MaxLength()

        maxLength.shouldValidate(fieldValue.context()) shouldBe false
    }

    @Test
    fun `validate field if option is present in field option`() {
        val msg = aTestMessage {
            value = randomString()
        }
        val value = MessageValue.atTopLevel(msg)
        val fieldValue = valueField(value)
        val maxLength = MaxLength()

        maxLength.shouldValidate(fieldValue.context()) shouldBe true
    }

    private fun valueField(value: MessageValue): FieldValue {
        return value.valueOf("value").orElseGet { fail() }
    }
}

/**
 * Creates a new instance of this constraint.
 *
 * @param optionValue A value that describes the field constraints.
 * @param field The constrained field.
 */
@Immutable
private class MaxLengthConstraint(
    optionValue: Int,
    field: FieldContext
) : FieldConstraint<Int>(optionValue, field.targetDeclaration()), CustomConstraint {

    override fun formattedErrorMessage(field: FieldContext): String {
        return "Value of `${field.targetDeclaration()}` must not be longer than `${optionValue()}`."
    }

    override fun validate(containingMessage: MessageValue): ImmutableList<ConstraintViolation> {
        val value = containingMessage.valueOf(field())
        val maxLength = optionValue()
        val context = value.context()
        val violation = constraintViolation {
            fieldPath = context.fieldPath()
            typeName = containingMessage.declaration().name().value()
            message = errorMessage(context)
        }
        return value.nonDefault()
            .filter { it.toString().length > maxLength }
            .map { violation }
            .collect(ImmutableList.toImmutableList())
    }
}

@Immutable
private class MaxLength : FieldValidatingOption<Int>(TestFieldOptionProto.maxLength) {
    override fun constraintFor(field: FieldContext): Constraint {
        return MaxLengthConstraint(optionValue(field), field)
    }
}
