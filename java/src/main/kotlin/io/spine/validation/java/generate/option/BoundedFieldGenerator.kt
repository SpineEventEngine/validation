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
import io.spine.protodata.Compilation
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.File
import io.spine.protodata.ast.Span
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isSingular
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.Literal
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.field
import io.spine.type.TypeName
import io.spine.validate.ConstraintViolation
import io.spine.validation.BoundedFieldView
import io.spine.validation.bound.NumericBound
import io.spine.validation.bound.NumericBound.ValueCase.DOUBLE_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.FLOAT_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.INT32_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.INT64_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.UINT32_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.UINT64_VALUE
import io.spine.validation.java.expression.IntegerClass
import io.spine.validation.java.expression.LongClass
import io.spine.validation.java.expression.orElse
import io.spine.validation.java.expression.resolve
import io.spine.validation.java.expression.stringify
import io.spine.validation.java.generate.OptionCode
import io.spine.validation.java.generate.FieldOptionGenerator
import io.spine.validation.java.generate.ValidationCodeInjector.MessageScope.message
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentName
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentPath
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.violations
import io.spine.validation.java.generate.option.Docs.SCALAR_TYPES
import io.spine.validation.java.generate.option.Docs.UNSIGNED_API
import io.spine.validation.java.violation.ErrorPlaceholder
import io.spine.validation.java.violation.constraintViolation
import io.spine.validation.java.violation.templateString

/**
 * An abstract base for field generators that restrict the range of numeric fields.
 *
 * @see RangeFieldGenerator
 * @see MinFieldGenerator
 * @see MaxFieldGenerator
 */
internal abstract class BoundedFieldGenerator(
    private val view: BoundedFieldView,
    private val option: String
) : FieldOptionGenerator {

    private val field = view.subject
    private val declaringType = field.declaringType
    private val getter = message.field(field).getter<Any>()

    /**
     * The type of the field to which the option is applied.
     */
    protected val fieldType: FieldType = field.type

    /**
     * Generates code for a field represented by the [view].
     */
    @Suppress("UNCHECKED_CAST") // The cast is guaranteed due to the field type checks.
    override fun generate(): OptionCode = when {
        fieldType.isSingular -> checkWithinBounds(getter as Expression<Number>)

        fieldType.isList ->
            CodeBlock(
                """
                for (var element : $getter) {
                    ${checkWithinBounds(ReadVar("element"))}
                }
                """.trimIndent()
            )

        else -> error(
            "The field type `${fieldType.name}` is not supported by `${this::class.simpleName}`." +
                    " Please ensure that the supported field types in this generator match those" +
                    " used by the policy, which verified `${view::class.simpleName}`."
        )
    }.run { OptionCode(this) }

    /**
     * Returns a [CodeBlock] that checks that the given [value] is within the bounds.
     *
     * If the passed value is out of the allowed range, the block creates an instance
     * of [ConstraintViolation] and adds it to the [violations] list.
     */
    private fun checkWithinBounds(value: Expression<Number>): CodeBlock {
        if (boundPrimitive in listOf(UINT32_VALUE, UINT64_VALUE)) {
            unsignedIntegerWarning(view.file, field.span)
        }
        return CodeBlock(
            """
            if (${isOutOfBounds(value)}) {
                var fieldPath = ${parentPath.resolve(field.name)};
                var typeName =  ${parentName.orElse(declaringType)};
                var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), value)};
                $violations.add(violation);
            }    
            """.trimIndent()
        )
    }

    /**
     * The number type of the bound.
     *
     * It is checked for unsigned primitives, for which the generator reports
     * a compilation warning. Such primitives are not natively supported in Java.
     */
    protected abstract val boundPrimitive: NumericBound.ValueCase

    /**
     * Returns a boolean expression that checks if the given [value] is within the bounds.
     *
     * Note that unsigned values must be handled in a special way because Java does not
     * support them natively. For this, `java.lang.Integer` and `java.lang.Long` classes
     * provide static methods to treat signed types as unsigned, including parsing, printing,
     * comparison and math operations.
     */
    protected abstract fun isOutOfBounds(value: Expression<Number>): Expression<Boolean>

    private fun violation(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<TypeName>,
        fieldValue: Expression<Number>,
    ): Expression<ConstraintViolation> {
        val qualifiedName = field.qualifiedName
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(fieldPath, typeNameStr, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, option, qualifiedName)
        return constraintViolation(errorMessage, typeNameStr, fieldPath, fieldValue)
    }

    protected abstract fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<String>,
        fieldValue: Expression<*>,
    ): Map<ErrorPlaceholder, Expression<String>>

    /**
     * Returns a string representation of this [NumericBound].
     *
     * Note that `int` and `long` values that represent unsigned primitives are printed as is.
     * In the rendered Java code, they can become negative number constants due to overflow,
     * which is expected.
     */
    protected fun NumericBound.asLiteral() =
        when (valueCase) {
            FLOAT_VALUE -> Literal("${floatValue}F")
            DOUBLE_VALUE -> Literal("$doubleValue")
            INT32_VALUE -> Literal("$int32Value")
            INT64_VALUE -> Literal("${int64Value}L")
            UINT32_VALUE -> Literal("$uint32Value")
            UINT64_VALUE -> Literal("$uint64Value")
            else -> error(
                "Unexpected field type `$valueCase` when converting range bounds to Java literal." +
                        " Make sure the policy, which verified `${view::class.simpleName}`," +
                        " correctly filtered out unsupported field types."
            )
        }
}

private fun unsignedIntegerWarning(file: File, span: Span) =
    Compilation.warning(file, span) {
        "Unsigned integer types are not supported in Java. The Protobuf compiler uses" +
                " signed integers to represent unsigned types in Java ($SCALAR_TYPES)." +
                " Operations on unsigned values rely on static utility methods from" +
                " `$IntegerClass` and `$LongClass` classes ($UNSIGNED_API). Be cautious" +
                " when dealing with unsigned values outside of these methods, as Java" +
                " treats all primitive integers as signed."
    }

@Suppress("MaxLineLength") // Long links.
private object Docs {
    const val SCALAR_TYPES = "https://protobuf.dev/programming-guides/proto3/#scalar"
    const val UNSIGNED_API =
        "https://www.baeldung.com/java-unsigned-arithmetic#the-unsigned-integer-api"
}
