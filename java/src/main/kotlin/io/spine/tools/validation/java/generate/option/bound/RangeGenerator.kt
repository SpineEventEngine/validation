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
import io.spine.tools.compiler.jvm.plus
import io.spine.tools.validation.bound.NumericBound.ValueCase
import io.spine.tools.validation.bound.NumericBound.ValueCase.UINT32_VALUE
import io.spine.tools.validation.bound.NumericBound.ValueCase.UINT64_VALUE
import io.spine.tools.validation.bound.RangeField
import io.spine.tools.validation.java.expression.IntegerClass
import io.spine.tools.validation.java.expression.LongClass
import io.spine.tools.validation.java.expression.StringClass
import io.spine.tools.validation.java.expression.joinToString
import io.spine.tools.validation.java.generate.OptionGenerator
import io.spine.tools.validation.java.generate.SingleOptionCode
import io.spine.tools.validation.option.RANGE
import io.spine.string.Placeholder
import io.spine.tools.validation.sortedByDeclaration
import io.spine.validation.StandardPlaceholder.FIELD_PATH
import io.spine.validation.StandardPlaceholder.FIELD_TYPE
import io.spine.validation.StandardPlaceholder.FIELD_VALUE
import io.spine.validation.StandardPlaceholder.PARENT_TYPE
import io.spine.validation.StandardPlaceholder.RANGE_VALUE

/**
 * The generator for `(range)` option.
 */
internal class RangeGenerator : OptionGenerator() {

    /**
     * All `(range)` fields in the current compilation process.
     *
     * The fields of each message go in the order of declaration.
     */
    private val allRangeFields by lazy {
        querying.select<RangeField>()
            .all()
            .sortedByDeclaration()
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
    ): Map<Placeholder, Expression<String>> = mapOf(
        FIELD_PATH.value to fieldPath.joinToString(),
        FIELD_VALUE.value to StringClass.call("valueOf", fieldValue),
        FIELD_TYPE.value to StringLiteral(fieldType.name),
        PARENT_TYPE.value to typeName,
        RANGE_VALUE.value to withFieldValue()
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
