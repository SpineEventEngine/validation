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

package io.spine.tools.validation.java.generate.option

import io.spine.base.FieldPath
import io.spine.server.query.select
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.jvm.CodeBlock
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.JavaValueConverter
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.jvm.field
import io.spine.tools.validation.ErrorPlaceholder
import io.spine.tools.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.tools.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.tools.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.tools.validation.ErrorPlaceholder.GOES_COMPANION
import io.spine.tools.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.tools.validation.java.expression.EmptyFieldCheck
import io.spine.tools.validation.java.expression.constraintViolation
import io.spine.tools.validation.java.expression.joinToString
import io.spine.tools.validation.java.expression.orElse
import io.spine.tools.validation.java.expression.resolve
import io.spine.tools.validation.java.expression.stringValueOf
import io.spine.tools.validation.java.expression.stringify
import io.spine.tools.validation.java.expression.templateString
import io.spine.tools.validation.java.generate.MessageScope.message
import io.spine.tools.validation.java.generate.OptionGenerator
import io.spine.tools.validation.java.generate.SingleOptionCode
import io.spine.tools.validation.java.generate.ValidateScope.parentName
import io.spine.tools.validation.java.generate.ValidateScope.parentPath
import io.spine.tools.validation.java.generate.ValidateScope.violations
import io.spine.validate.ConstraintViolation
import io.spine.tools.validation.option.GOES
import io.spine.tools.validation.GoesField

/**
 * The generator for the `(goes)` option.
 */
internal class GoesGenerator(
    private val converter: JavaValueConverter
) : OptionGenerator() {

    /**
     * All `(goes)` fields in the current compilation process.
     */
    private val allGoesFields by lazy {
        querying.select<GoesField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allGoesFields
            .filter { it.id.type == type }
            .map { GenerateGoes(it, converter).code() }
}

/**
 * Generates code for a single application of the `(goes)` option
 * represented by the [view].
 */
private class GenerateGoes(
    private val view: GoesField,
    override val converter: JavaValueConverter
) : EmptyFieldCheck {

    private val field = view.subject
    private val fieldType = field.type
    private val declaringType = field.declaringType

    /**
     * Returns the generated code.
     */
    fun code(): SingleOptionCode {
        val companion = view.companion
        val fieldGetter = message.field(field).getter<Any>()
        val constraint = CodeBlock(
            """
            if (!${field.hasDefaultValue()} && ${companion.hasDefaultValue()}) {
                var fieldPath = ${parentPath.resolve(field.name)};
                var typeName =  ${parentName.orElse(declaringType)};
                var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), fieldGetter)};
                $violations.add(violation);
            }
            """.trimIndent()
        )
        return SingleOptionCode(constraint)
    }

    private fun violation(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<io.spine.type.TypeName>,
        fieldValue: Expression<*>,
    ): Expression<ConstraintViolation> {
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(fieldPath, typeNameStr, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, GOES)
        return constraintViolation(errorMessage, typeNameStr, fieldPath, fieldValue)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<String>,
        fieldValue: Expression<*>,
    ): Map<ErrorPlaceholder, Expression<String>> = mapOf(
        FIELD_PATH to fieldPath.joinToString(),
        FIELD_VALUE to fieldType.stringValueOf(fieldValue),
        FIELD_TYPE to StringLiteral(fieldType.name),
        PARENT_TYPE to typeName,
        GOES_COMPANION to StringLiteral(view.companion.name.value)
    )
}
