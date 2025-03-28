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
import io.spine.protodata.ast.name
import io.spine.protodata.java.Expression
import io.spine.protodata.java.StringLiteral
import io.spine.validation.bound.NumericBound
import io.spine.validation.bound.NumericBound.ValueCase.UINT32_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.UINT64_VALUE
import io.spine.validation.RANGE
import io.spine.validation.bound.RangeField
import io.spine.validation.java.expression.IntegerClass
import io.spine.validation.java.expression.LongClass
import io.spine.validation.java.expression.joinToString
import io.spine.validation.java.expression.stringValueOf
import io.spine.validation.java.violation.ErrorPlaceholder
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.violation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.java.violation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.java.violation.ErrorPlaceholder.RANGE_VALUE

/**
 * The generator for `(range)` option.
 *
 * Generates code for a single field represented by the provided [view].
 */
internal class RangeFieldGenerator(
    private val view: RangeField
) : BoundedFieldGenerator(view, RANGE) {

    private val lower = view.lowerBound
    private val upper = view.upperBound

    override val boundPrimitive: NumericBound.ValueCase = lower.valueCase

    /**
     * Returns a boolean expression that checks if the given [value] is within
     * the [lower] and [upper] bounds.
     */
    override fun isOutOfBounds(value: Expression<Number>): Expression<Boolean>  {
        val lowerLiteral = lower.asLiteral()
        val lowerOperator = if (lower.exclusive) "<=" else "<"
        val upperLiteral = upper.asLiteral()
        val upperOperator = if (upper.exclusive) ">=" else ">"
        return when (boundPrimitive) {
            UINT32_VALUE -> Expression(
                "$IntegerClass.compareUnsigned($value, $lowerLiteral) $lowerOperator 0 ||" +
                        "$IntegerClass.compareUnsigned($value, $upperLiteral) $upperOperator 0"
            )
            UINT64_VALUE -> Expression(
                "$LongClass.compareUnsigned($value, $lowerLiteral) $lowerOperator 0 ||" +
                        "$LongClass.compareUnsigned($value, $upperLiteral) $upperOperator 0"
            )
            else -> Expression(
                "$value $lowerOperator $lowerLiteral ||" +
                        " $value $upperOperator $upperLiteral"
            )
        }
    }

    override fun supportedPlaceholders(
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
