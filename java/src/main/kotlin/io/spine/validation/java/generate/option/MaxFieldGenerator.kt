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
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.field
import io.spine.type.TypeName
import io.spine.validate.ConstraintViolation
import io.spine.validation.MaxField
import io.spine.validation.NumericBound.ValueCase.UINT32_VALUE
import io.spine.validation.NumericBound.ValueCase.UINT64_VALUE
import io.spine.validation.RANGE
import io.spine.validation.java.expression.IntegerClass
import io.spine.validation.java.expression.LongClass
import io.spine.validation.java.expression.joinToString
import io.spine.validation.java.expression.orElse
import io.spine.validation.java.expression.resolve
import io.spine.validation.java.expression.stringValueOf
import io.spine.validation.java.expression.stringify
import io.spine.validation.java.generate.FieldOptionCode
import io.spine.validation.java.generate.FieldOptionGenerator
import io.spine.validation.java.generate.ValidationCodeInjector.MessageScope.message
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentName
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentPath
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.violations
import io.spine.validation.java.violation.ErrorPlaceholder
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.java.violation.ErrorPlaceholder.MAX_OPERATOR
import io.spine.validation.java.violation.ErrorPlaceholder.MAX_VALUE
import io.spine.validation.java.violation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.java.violation.constraintViolation
import io.spine.validation.java.violation.templateString

/**
 * The generator for `(max)` option.
 *
 * Generates code for a single field represented by the provided [view].
 */
internal class MaxFieldGenerator(private val view: MaxField) : FieldOptionGenerator {

    private val field = view.subject
    private val fieldType = field.type
    private val declaringType = field.declaringType
    private val getter = message.field(field).getter<Any>()
    private val bound = view.upperBound
    private val operator = if (bound.inclusive) ">" else ">="

    /**
     * Generates code for a field represented by the [view].
     */
    @Suppress("UNCHECKED_CAST") // The cast is guaranteed due to the field type checks.
    override fun generate(): FieldOptionCode = when {
        fieldType.isSingular -> checkWithinTheRange(getter as Expression<Number>)

        fieldType.isList ->
            CodeBlock(
                """
                for (var element : $getter) {
                    ${checkWithinTheRange(ReadVar("element"))}
                }
                """.trimIndent()
            )

        else -> error(
            "The field type `${fieldType.name}` is not supported by `MaxFieldGenerator`." +
                    " Please ensure that the supported field types in this generator match those" +
                    " used by `MaxPolicy` when validating the `MaxFieldDiscovered` event."
        )
    }.run { FieldOptionCode(this) }

    /**
     * Returns a [CodeBlock] that checks that the given [value] is within the upper [bound].
     *
     * If the passed value is out of the range, creates an instance of [ConstraintViolation]
     * adding it to the [violations] list.
     */
    private fun checkWithinTheRange(value: Expression<Number>): CodeBlock =
        CodeBlock(
            """
            if (${doesNotBelongToRange(value)}) {
                var fieldPath = ${parentPath.resolve(field.name)};
                var typeName =  ${parentName.orElse(declaringType)};
                var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), value)};
                $violations.add(violation);
            }    
            """.trimIndent()
        )

    /**
     * Returns a boolean expression that checks if the given [value] is within
     * the [lower] and [bound] bounds.
     *
     * Unsigned values are handled in a special way because Java does not support
     * them natively. [IntegerClass] and [LongClass] classes provide static methods
     * to treat signed types as unsigned, including parsing, printing, comparison
     * and math operations. Outside of these methods, these primitives remain just
     * signed types.
     */
    private fun doesNotBelongToRange(value: Expression<Number>): Expression<Boolean> {
        val valueCase = bound.valueCase // This case is the same for both `lower` and `upper`.
        val literal = bound.asLiteral()
        if (valueCase in listOf(UINT32_VALUE, UINT64_VALUE)) {
            unsignedIntegerWarning(view.file, field.span)
        }
        return when (valueCase) {
            UINT32_VALUE -> Expression(
                "$IntegerClass.compareUnsigned($value, $literal) $operator 0"
            )
            UINT64_VALUE -> Expression(
                "$LongClass.compareUnsigned($value, $literal) $operator 0"
            )
            else -> Expression("$value $operator $literal")
        }
    }

    private fun violation(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<TypeName>,
        fieldValue: Expression<Number>,
    ): Expression<ConstraintViolation> {
        val qualifiedName = field.qualifiedName
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(fieldPath, typeNameStr, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, RANGE, qualifiedName)
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
        MAX_VALUE to StringLiteral(view.max),
        MAX_OPERATOR to StringLiteral(operator)
    )
}
