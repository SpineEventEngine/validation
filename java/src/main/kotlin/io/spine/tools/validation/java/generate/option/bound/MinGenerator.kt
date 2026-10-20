/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.validation.java.generate.option.bound

import io.spine.base.FieldPath
import io.spine.server.query.select
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.jvm.call
import io.spine.tools.validation.bound.MinField
import io.spine.tools.validation.bound.NumericBound.ValueCase
import io.spine.tools.validation.bound.NumericBound.ValueCase.UINT32_VALUE
import io.spine.tools.validation.bound.NumericBound.ValueCase.UINT64_VALUE
import io.spine.tools.validation.java.expression.IntegerClass
import io.spine.tools.validation.java.expression.LongClass
import io.spine.tools.validation.java.expression.StringClass
import io.spine.tools.validation.java.expression.joinToString
import io.spine.tools.validation.java.generate.OptionGenerator
import io.spine.tools.validation.java.generate.SingleOptionCode
import io.spine.tools.validation.option.MIN
import io.spine.string.Placeholder
import io.spine.tools.validation.sortedByDeclaration
import io.spine.validation.StandardPlaceholder.FIELD_PATH
import io.spine.validation.StandardPlaceholder.FIELD_TYPE
import io.spine.validation.StandardPlaceholder.FIELD_VALUE
import io.spine.validation.StandardPlaceholder.MIN_OPERATOR
import io.spine.validation.StandardPlaceholder.MIN_VALUE
import io.spine.validation.StandardPlaceholder.PARENT_TYPE

/**
 * The generator for `(min)` option.
 */
internal class MinGenerator : OptionGenerator() {

    /**
     * All `(min)` fields in the current compilation process.
     *
     * The fields of each message go in the order of declaration.
     */
    private val allMinFields by lazy {
        querying.select<MinField>()
            .all()
            .sortedByDeclaration()
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

    override val boundPrimitive: ValueCase = bound.valueCase

    /**
     * Returns a boolean expression that checks if the given [value] falls back
     * the minimum [bound].
     */
    @Suppress("MaxLineLength") // Easier to read.
    override fun isOutOfBounds(value: Expression<Number>): Expression<Boolean> {
        val lowerBound = bound.asNumberExpression()
        val operator = if (isExclusive) "<=" else "<"
        return when (boundPrimitive) {
            UINT32_VALUE -> Expression("$IntegerClass.compareUnsigned($value, $lowerBound) $operator 0")
            UINT64_VALUE -> Expression("$LongClass.compareUnsigned($value, $lowerBound) $operator 0")
            else -> Expression("$value $operator $lowerBound")
        }
    }

    override fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<String>,
        fieldValue: Expression<*>,
    ): Map<Placeholder, Expression<String>> = mapOf(
        FIELD_PATH.value to fieldPath.joinToString(),
        FIELD_VALUE.value to StringClass.call("valueOf", fieldValue),
        FIELD_TYPE.value to StringLiteral(fieldType.name),
        PARENT_TYPE.value to typeName,
        MIN_VALUE.value to view.min.withFieldValue(bound),
        MIN_OPERATOR.value to StringLiteral(if (isExclusive) ">" else ">=")
    )
}
