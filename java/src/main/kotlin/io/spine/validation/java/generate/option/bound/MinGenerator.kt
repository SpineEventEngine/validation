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
import io.spine.validation.MIN
import io.spine.validation.bound.MinField
import io.spine.validation.bound.NumericBound
import io.spine.validation.bound.NumericBound.ValueCase.UINT32_VALUE
import io.spine.validation.bound.NumericBound.ValueCase.UINT64_VALUE
import io.spine.validation.api.expression.IntegerClass
import io.spine.validation.api.expression.LongClass
import io.spine.validation.api.expression.joinToString
import io.spine.validation.api.expression.stringValueOf
import io.spine.validation.api.generate.SingleOptionCode
import io.spine.validation.api.generate.OptionGenerator
import io.spine.validation.ErrorPlaceholder
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.MIN_OPERATOR
import io.spine.validation.ErrorPlaceholder.MIN_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE

/**
 * The generator for `(min)` option.
 */
internal class MinGenerator : OptionGenerator() {

    /**
     * All `(min)` fields in the current compilation process.
     */
    private val allMinFields by lazy {
        querying.select<MinField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allMinFields
            .filter { it.id.type == type }
            .map { GenerateMin(it).code() }
}

/**
 * Generates code for a single application of the `(min)` option
 * represented by the [view].
 */
private class GenerateMin(private val view: MinField) : BoundedFieldGenerator(view, MIN) {

    private val bound = view.bound
    private val isExclusive = bound.exclusive

    override val boundPrimitive: NumericBound.ValueCase = bound.valueCase

    /**
     * Returns a boolean expression that checks if the given [value] falls back
     * the minimum [bound].
     */
    @Suppress("MaxLineLength") // Easier to read.
    override fun isOutOfBounds(value: Expression<Number>): Expression<Boolean> {
        val boundExpr = bound.asNumberExpression()
        val operator = if (isExclusive) "<=" else "<"
        return when (boundPrimitive) {
            UINT32_VALUE -> Expression("$IntegerClass.compareUnsigned($value, $boundExpr) $operator 0")
            UINT64_VALUE -> Expression("$LongClass.compareUnsigned($value, $boundExpr) $operator 0")
            else -> Expression("$value $operator $boundExpr")
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
        MIN_VALUE to StringLiteral(view.min),
        MIN_OPERATOR to StringLiteral(if (isExclusive) ">" else ">=")
    )
}
