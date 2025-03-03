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

package io.spine.validation.java.option

import io.spine.base.FieldPath
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.validate.ConstraintViolation
import io.spine.validation.IF_MISSING
import io.spine.validation.RequiredField
import io.spine.validation.java.DefaultValueChecker
import io.spine.validation.java.ErrorPlaceholder
import io.spine.validation.java.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.java.FieldOptionCode
import io.spine.validation.java.ValidationCodeInjector.ValidateScope.parentPath
import io.spine.validation.java.ValidationCodeInjector.ValidateScope.violations
import io.spine.validation.java.constraintViolation
import io.spine.validation.java.fieldPath
import io.spine.validation.java.joinToString
import io.spine.validation.java.templateString

/**
 * The generator for `(required)` option.
 *
 * Generates code for a single field represented by the provided [view].
 */
internal class RequiredFieldGenerator(
    private val view: RequiredField,
    converter: JavaValueConverter,
) : DefaultValueChecker(converter) {

    private val field = view.subject
    private val declaringType = field.declaringType

    /**
     * Generates code for a field represented by the [view].
     */
    fun generate(): FieldOptionCode {
        val constraint = CodeBlock(
            """
            if (${field.hasDefaultValue()}) {
                var fieldPath = ${fieldPath(parentPath, field.name)};
                var violation = ${violation(ReadVar("fieldPath"))};
                $violations.add(violation);
            }
            """.trimIndent()
        )
        return FieldOptionCode(constraint)
    }

    private fun violation(fieldPath: Expression<FieldPath>): Expression<ConstraintViolation> {
        val placeholders = supportedPlaceholders(fieldPath)
        val errorMessage =
            templateString(view.errorMessage, placeholders, IF_MISSING, field.qualifiedName)
        return constraintViolation(errorMessage, declaringType, fieldPath, fieldValue = null)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>
    ): Map<ErrorPlaceholder, Expression<String>> = mapOf(
        FIELD_PATH to fieldPath.joinToString(),
        FIELD_TYPE to StringLiteral(field.type.name),
        PARENT_TYPE to StringLiteral(declaringType.qualifiedName)
    )
}
