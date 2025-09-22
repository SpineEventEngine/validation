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

package io.spine.validation.java.generate.option.bound

import io.spine.base.FieldPath
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.jvm.call
import io.spine.tools.compiler.jvm.plus
import io.spine.server.query.select
import io.spine.validation.ErrorPlaceholder
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.ErrorPlaceholder.RANGE_VALUE
import io.spine.validation.RANGE
import io.spine.validation.api.expression.IntegerClass
import io.spine.validation.api.expression.LongClass
import io.spine.validation.api.expression.StringClass
import io.spine.validation.api.expression.joinToString
import io.spine.validation.api.generate.OptionGenerator
import io.spine.validation.api.generate.SingleOptionCode
import io.spine.validation.bound.NumericBound.ValueCase
import io.spine.validation.bound.NumericBound.ValueCase.UINT32_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.UINT64_VALUE
import io.spine.validation.bound.RangeField

/**
 * The generator for `(range)` option.
 */
internal class RangeGenerator : OptionGenerator() {

    /**
     * All `(range)` fields in the current compilation process.
     */
    private val allRangeFields by lazy {
        querying.select<RangeField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allRangeFields
            .filter { it.id.type == type }
            .map { GenerateRange(it).code() }
}

/**
 * Generates code for a single application of the `(range)` option
 * represented by the [view].
 */
private class GenerateRange(
    private val view: RangeField
) : BoundedFieldGenerator(view, RANGE) {

    private val lower = view.lowerBound
    private val upper = view.upperBound

    override val boundPrimitive: ValueCase = lower.valueCase

    /**
     * Returns a boolean expression that checks if the given [value] is within
     * the [lower] and [upper] bounds.
     */
    override fun isOutOfBounds(value: Expression<Number>): Expression<Boolean> {
        val lowerBound = lower.asNumberExpression()
        val lowerOperator = if (lower.exclusive) "<=" else "<"
        val upperBound = upper.asNumberExpression()
        val upperOperator = if (upper.exclusive) ">=" else ">"
        return when (boundPrimitive) {
            UINT32_VALUE -> Expression(
                "$IntegerClass.compareUnsigned($value, $lowerBound) $lowerOperator 0 ||" +
                        "$IntegerClass.compareUnsigned($value, $upperBound) $upperOperator 0"
            )
            UINT64_VALUE -> Expression(
                "$LongClass.compareUnsigned($value, $lowerBound) $lowerOperator 0 ||" +
                        "$LongClass.compareUnsigned($value, $upperBound) $upperOperator 0"
            )
            else -> Expression(
                "$value $lowerOperator $lowerBound ||" +
                        " $value $upperOperator $upperBound"
            )
        }
    }

    override fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<String>,
        fieldValue: Expression<*>,
    ): Map<ErrorPlaceholder, Expression<String>> = mapOf(
        FIELD_PATH to fieldPath.joinToString(),
        FIELD_VALUE to StringClass.call("valueOf", fieldValue),
        FIELD_TYPE to StringLiteral(fieldType.name),
        PARENT_TYPE to typeName,
        RANGE_VALUE to withFieldValue()
    )

    /**
     * Constructs a string [Expression] for the range literal, inserting bound
     * field values, if any.
     *
     * The method splits a range string into its lower and upper parts, appending any
     * referenced field’s actual values in parentheses, and then concatenates back
     * these parts using the original delimiter.
     */
    private fun withFieldValue(): Expression<String> {
        val (left, right) = view.range.split(DELIMITER).map { it.trim() }
        val leftValue = left.withFieldValue(lower)
        val rightBrace = right.last().toString()
        val rightValue = right.dropLast(1).withFieldValue(upper) + rightBrace
        return leftValue + " $DELIMITER " + rightValue
    }

    private companion object {

        /**
         * The range delimiter.
         */
        const val DELIMITER = ".."
    }
}
