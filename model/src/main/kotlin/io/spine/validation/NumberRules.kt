/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.validation

import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import io.spine.option.MaxOption
import io.spine.option.MinOption
import io.spine.option.OptionsProto.max
import io.spine.option.OptionsProto.min
import io.spine.option.OptionsProto.range
import io.spine.protobuf.unpack
import io.spine.protodata.FieldName
import io.spine.protodata.Option
import io.spine.protodata.Value
import io.spine.validation.ComparisonOperator.GREATER_OR_EQUAL
import io.spine.validation.ComparisonOperator.GREATER_THAN
import io.spine.validation.ComparisonOperator.LESS_OR_EQUAL
import io.spine.validation.ComparisonOperator.LESS_THAN
import io.spine.validation.LogicalOperator.AND
import io.spine.protodata.Value.KindCase.DOUBLE_VALUE
import io.spine.protodata.Value.KindCase.INT_VALUE

/**
 * A factory of validation rules for number fields.
 */
internal class NumberRules
private constructor(
    private val upperBound: Value? = null,
    private val lowerBound: Value? = null,
    private val uppedInclusive: Boolean = false,
    private val lowerInclusive: Boolean = false,
    customErrorMessage: String? = null
) {

    private val customErrorMessage: String? = customErrorMessage?.ifEmpty { null }

    /**
     * Creates a [SimpleRule] which states that the value must be greater than a threshold.
     */
    fun minRule(field: FieldName): SimpleRule = field.toMinRule()

    /**
     * Creates a [SimpleRule] which states that the value must be less than a threshold.
     */
    fun maxRule(field: FieldName): SimpleRule = field.toMaxRule()

    /**
     * Creates a [CompositeRule] which states that the value must lie within a range.
     */
    fun rangeRule(field: FieldName): CompositeRule = field.toRangeRule()

    private fun FieldName.toMinRule(): SimpleRule =
        simpleRule(lowerBound!!, lowerInclusive, GREATER_OR_EQUAL, GREATER_THAN, "greater")

    private fun FieldName.toMaxRule(): SimpleRule =
        simpleRule(upperBound!!, uppedInclusive, LESS_OR_EQUAL, LESS_THAN, "less")

    private fun FieldName.simpleRule(
        threshold: Value,
        inclusive: Boolean,
        inclusiveOperator: ComparisonOperator,
        exclusiveOperator: ComparisonOperator,
        adjective: String
    ): SimpleRule {
        val fieldName = this
        return simpleRule {
            field = fieldName
            operator = if (inclusive) inclusiveOperator else exclusiveOperator
            otherValue = threshold
            errorMessage = compileErrorMessage(adjective, inclusive)
            distribute = true
        }
    }

    private fun compileErrorMessage(adjective: String, inclusive: Boolean): String =
        if (customErrorMessage != null) {
            customErrorMessage
        } else {
            val orEqual = if (inclusive) "or equal to " else ""
            "The number must be $adjective than $orEqual{other}, but was {value}."
        }

    private fun FieldName.toRangeRule() = compositeRule {
        left = toMinRule().wrap()
        right = toMaxRule().wrap()
        operator = AND
        field = this@toRangeRule
        errorMessage = rangeErrorMessage
    }

    private val rangeErrorMessage: String
        get() {
            fun Boolean.str(): String = if (this) "inclusive" else "exclusive"

            fun Value.str(): String = when (kindCase) {
                DOUBLE_VALUE -> doubleValue.toString()
                INT_VALUE -> intValue.toString()
                else -> error("Unexpected Value: `$this`.")
            }

            return "The number must be between ${lowerBound!!.str()} " +
                    "(${lowerInclusive.str()}) and ${upperBound!!.str()} " +
                    "(${uppedInclusive.str()}), but was {value}."
        }

    companion object {

        /**
         * Creates a new `NumberRules` from the given option.
         *
         * The option must be the `(min)`, the `(max)`, or the `(range)`.
         *
         * @throws IllegalArgumentException upon an unsupported option
         */
        @JvmStatic
        fun from(option: Option): NumberRules = option.toRules()

        private fun Option.toRules(): NumberRules = when {
            isA(range) -> forRange()
            isA(min) -> forMin()
            isA(max) -> forMax()
            else -> throw IllegalArgumentException(
            "Option $name is not a number range option."
            )
        }
        @Suppress("FunctionNaming") // backticked name is necessary here.
        private fun Option.isA(generated: GeneratedExtension<*, *>) =
            name == generated.descriptor.name && number == generated.number

        private fun Option.forMin(): NumberRules {
            val optionValue = value<MinOption>()
            val threshold = optionValue.value.parseToNumber()
            return NumberRules(
                lowerBound = threshold,
                lowerInclusive = !optionValue.exclusive,
                customErrorMessage = optionValue.errorMsg
            )
        }

        private fun Option.forMax(): NumberRules {
            val optionValue = value<MaxOption>()
            val threshold = optionValue.value.parseToNumber()
            return NumberRules(
                upperBound = threshold,
                uppedInclusive = !optionValue.exclusive,
                customErrorMessage = optionValue.errorMsg
            )
        }

        private fun Option.forRange(): NumberRules {
            val optionValue = value<StringValue>().value
            val notation = RangeNotation.parse(optionValue)
            return NumberRules(
                upperBound = notation.max,
                lowerBound = notation.min,
                uppedInclusive = notation.maxInclusive,
                lowerInclusive = notation.minInclusive
            )
        }
    }
}

/**
 * Unpacks the value of this option into a message of the given type `T`.
 */
private inline fun <reified T : Message> Option.value() = value.unpack<T>()
