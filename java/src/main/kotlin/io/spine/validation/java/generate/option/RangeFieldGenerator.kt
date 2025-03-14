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

import io.spine.base.FieldPath
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isSingular
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.Literal
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.field
import io.spine.type.TypeName
import io.spine.validate.ConstraintViolation
import io.spine.validation.GOES
import io.spine.validation.NumericBound
import io.spine.validation.RangeField
import io.spine.validation.java.expression.joinToString
import io.spine.validation.java.expression.orElse
import io.spine.validation.java.expression.resolve
import io.spine.validation.java.expression.stringValueOf
import io.spine.validation.java.expression.stringify
import io.spine.validation.java.generate.FieldOptionCode
import io.spine.validation.java.generate.FieldOptionGenerator
import io.spine.validation.java.generate.ValidationCodeInjector.MessageScope.message
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentPath
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentName
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.violations
import io.spine.validation.java.violation.ErrorPlaceholder
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.java.violation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.java.violation.ErrorPlaceholder.RANGE_VALUE
import io.spine.validation.java.violation.constraintViolation
import io.spine.validation.java.violation.templateString

/**
 * The generator for `(range)` option.
 *
 * Generates code for a single field represented by the provided [view].
 */
internal class RangeFieldGenerator(private val view: RangeField) : FieldOptionGenerator {

    private val field = view.subject
    private val fieldType = field.type
    private val declaringType = field.declaringType
    private val getter = message.field(field).getter<Any>()

    private val min = view.minValue
    private val max = view.maxValue


    /**
     * Generates code for a field represented by the [view].
     */
    override fun generate(): FieldOptionCode = when {
        fieldType.isSingular -> CodeBlock(
            """
            if ($getter ${if (min.exclusive) "<=" else "<"} ${min.bound.asLiteral()} || $getter ${if (max.exclusive) ">=" else ">"} ${max.bound.asLiteral()}) {
                var fieldPath = ${parentPath.resolve(field.name)};
                var typeName =  ${parentName.orElse(declaringType)};
                var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), getter)};
                $violations.add(violation);
            }
            """.trimIndent()
        )

        fieldType.isList ->
            CodeBlock(
                """
                for (var element : $getter) {
                    if (element ${if (min.exclusive) "<=" else "<"} ${min.bound.asLiteral()} || element ${if (max.exclusive) ">=" else ">"} ${max.bound.asLiteral()}) {
                        var fieldPath = ${parentPath.resolve(field.name)};
                        var typeName =  ${parentName.orElse(declaringType)};
                        var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), ReadVar("element"))};
                        $violations.add(violation);
                    }
                }
                """.trimIndent()
            )

        else -> error(
            "The field type `${fieldType.name}` is not supported by `RangeFieldGenerator`." +
                    " Please ensure that the supported field types in this generator match those" +
                    " used by `RangePolicy` when validating the `RangeFieldDiscovered` event."
        )
    }.run { FieldOptionCode(this) }

    private fun violation(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<TypeName>,
        fieldValue: Expression<Any>,
    ): Expression<ConstraintViolation> {
        val qualifiedName = field.qualifiedName
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(fieldPath, typeNameStr, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, GOES, qualifiedName)
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
        RANGE_VALUE to StringLiteral(view.range)
    )
}

private fun NumericBound.asLiteral() =
    Literal(if (hasInt64Value()) "${int64Value}L" else doubleValue)
