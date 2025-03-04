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

package io.spine.validation.java.generate.option

import com.google.protobuf.Message
import io.spine.base.FieldPath
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.This
import io.spine.protodata.java.field
import io.spine.validate.ConstraintViolation
import io.spine.validation.GOES
import io.spine.validation.GoesField
import io.spine.validation.java.expression.EmptyFieldCheck
import io.spine.validation.java.violation.ErrorPlaceholder
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.java.violation.ErrorPlaceholder.GOES_COMPANION
import io.spine.validation.java.violation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.java.generate.FieldOptionCode
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentPath
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.violations
import io.spine.validation.java.violation.constraintViolation
import io.spine.validation.java.violation.fieldPath
import io.spine.validation.java.violation.joinToString
import io.spine.validation.java.expression.stringValueOf
import io.spine.validation.java.generate.FieldOptionGenerator
import io.spine.validation.java.violation.templateString

/**
 * The generator for `(goes)` option.
 *
 * Generates code for a single field represented by the provided [view].
 */
internal class GoesFieldGenerator(
    private val view: GoesField,
    override val converter: JavaValueConverter
) : FieldOptionGenerator, EmptyFieldCheck {

    private val field = view.subject
    private val fieldType = field.type
    private val declaringType = field.declaringType

    /**
     * Generates code for a field represented by the [view].
     */
    override fun code(): FieldOptionCode {
        val field = view.subject
        val companion = view.companion
        val fieldGetter = This<Message>()
            .field(field)
            .getter<Any>()
        val constraint = CodeBlock(
            """
            if (!${field.hasDefaultValue()} && ${companion.hasDefaultValue()}) {
                var fieldPath = ${fieldPath(parentPath, field.name)};
                var violation = ${violation(ReadVar("fieldPath"), fieldGetter)};
                $violations.add(violation);
            }
            """.trimIndent()
        )
        return FieldOptionCode(constraint)
    }

    private fun violation(
        fieldPath: Expression<FieldPath>,
        fieldValue: Expression<*>,
    ): Expression<ConstraintViolation> {
        val qualifiedName = field.qualifiedName
        val placeholders = supportedPlaceholders(fieldPath, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, GOES, qualifiedName)
        return constraintViolation(errorMessage, declaringType, fieldPath, fieldValue)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        fieldValue: Expression<*>,
    ): Map<ErrorPlaceholder, Expression<String>> = mapOf(
        FIELD_PATH to fieldPath.joinToString(),
        FIELD_VALUE to fieldType.stringValueOf(fieldValue),
        FIELD_TYPE to StringLiteral(fieldType.name),
        PARENT_TYPE to StringLiteral(declaringType.qualifiedName),
        GOES_COMPANION to StringLiteral(view.companion.name.value)
    )
}
