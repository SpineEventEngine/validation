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
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.name
import io.spine.protodata.java.Expression
import io.spine.protodata.java.StringLiteral
import io.spine.server.query.select
import io.spine.validation.ErrorPlaceholder
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.MAX_OPERATOR
import io.spine.validation.ErrorPlaceholder.MAX_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.MAX
import io.spine.validation.api.expression.IntegerClass
import io.spine.validation.api.expression.LongClass
import io.spine.validation.api.expression.joinToString
import io.spine.validation.api.expression.stringValueOf
import io.spine.validation.api.generate.OptionGenerator
import io.spine.validation.api.generate.SingleOptionCode
import io.spine.validation.bound.MaxField
import io.spine.validation.bound.NumericBound.ValueCase
import io.spine.validation.bound.NumericBound.ValueCase.UINT32_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.UINT64_VALUE

/**
 * The generator for the `(max)` option.
 */
internal class MaxGenerator : OptionGenerator() {

    /**
     * All `(max)` fields in the current compilation process.
     */
    private val allMaxFields by lazy {
        querying.select<MaxField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allMaxFields
            .filter { it.id.type == type }
            .map { GenerateMax(it).code() }
}

/**
 * Generates code for a single application of the `(max)` option
 * represented by the [view].
 */
private class GenerateMax(private val view: MaxField) : BoundedFieldGenerator(view, MAX) {

    private val bound = view.bound
    private val isExclusive = bound.exclusive

    override val boundPrimitive: ValueCase = bound.valueCase

    /**
     * Returns a boolean expression that checks if the given [value] exceeds
     * the maximum [bound].
     */
    @Suppress("MaxLineLength") // Easier to read.
    override fun isOutOfBounds(value: Expression<Number>): Expression<Boolean> {
        val upperBound = bound.asNumberExpression()
        val operator = if (isExclusive) ">=" else ">"
        return when (boundPrimitive) {
            UINT32_VALUE -> Expression("$IntegerClass.compareUnsigned($value, $upperBound) $operator 0")
            UINT64_VALUE -> Expression("$LongClass.compareUnsigned($value, $upperBound) $operator 0")
            else -> Expression("$value $operator $upperBound")
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
        MAX_VALUE to bound.stringify(view.max),
        MAX_OPERATOR to StringLiteral(if (isExclusive) "<" else "<=")
    )
}
