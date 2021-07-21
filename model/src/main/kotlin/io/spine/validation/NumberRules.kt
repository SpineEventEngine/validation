/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.protobuf.GeneratedMessage
import com.google.protobuf.StringValue
import io.spine.option.MaxOption
import io.spine.option.MinOption
import io.spine.option.OptionsProto
import io.spine.protobuf.AnyPacker
import io.spine.protodata.FieldName
import io.spine.protodata.Option
import io.spine.validation.ComparisonOperator.GREATER_OR_EQUAL
import io.spine.validation.ComparisonOperator.GREATER_THAN
import io.spine.validation.ComparisonOperator.LESS_OR_EQUAL
import io.spine.validation.ComparisonOperator.LESS_THAN
import io.spine.validation.LogicalOperator.AND

internal class NumberRules
private constructor(
    private val upperBound: Value? = null,
    private val lowerBound: Value? = null,
    private val uppedInclusive: Boolean = false,
    private val lowerInclusive: Boolean = false
) {

    fun minRule(field: FieldName): SimpleRule =
        simpleRule(field, lowerBound!!, lowerInclusive, GREATER_OR_EQUAL, GREATER_THAN, "greater")

    fun maxRule(field: FieldName): SimpleRule =
        simpleRule(field, upperBound!!, uppedInclusive, LESS_OR_EQUAL, LESS_THAN, "less")

    private fun simpleRule(
        field: FieldName,
        threshold: Value,
        inclusive: Boolean,
        inclusiveOperator: ComparisonOperator,
        exclusiveOperator: ComparisonOperator,
        adjective: String
    ) =
        SimpleRule.newBuilder()
            .setField(field)
            .setSign(if (inclusive) inclusiveOperator else exclusiveOperator)
            .setOtherValue(threshold)
            .setErrorMessage(compileErrorMessage(adjective, inclusive))
            .build()

    private fun compileErrorMessage(adjective: String, inclusive: Boolean): String {
        val orEqual = if (inclusive) "or equal to " else ""
        return "The number must be $adjective than $orEqual{other}, but was {value}."
    }

    fun rangeRule(field: FieldName): CompositeRule =
        CompositeRule.newBuilder()
            .setLeft(minRule(field).wrap())
            .setRight(maxRule(field).wrap())
            .setOperator(AND)
            .build()

    private fun SimpleRule.wrap(): Rule =
        Rule.newBuilder()
            .setSimple(this)
            .build()

    override fun toString(): String {
        return "NumberRules(upperBound=$upperBound, lowerBound=$lowerBound, uppedInclusive=$uppedInclusive, lowerInclusive=$lowerInclusive)"
    }


    companion object {

        @JvmStatic
        fun from(option: Option): NumberRules {
            when {
                option.isRange() -> {
                    val optionValue = AnyPacker.unpack(option.value, StringValue::class.java).value
                    val notation = RangeNotation.parse(optionValue)
                    return NumberRules(
                        upperBound = notation.max,
                        lowerBound = notation.min,
                        uppedInclusive = notation.maxInclusive,
                        lowerInclusive = notation.minInclusive
                    )
                }
                option.isMin() -> {
                    val optionValue = AnyPacker.unpack(option.value, MinOption::class.java)
                    val threshold = optionValue.value.parseToNumber()
                    return NumberRules(
                        lowerBound = threshold,
                        lowerInclusive = !optionValue.exclusive
                    )
                }
                option.isMax() -> {
                    val optionValue = AnyPacker.unpack(option.value, MaxOption::class.java)
                    val threshold = optionValue.value.parseToNumber()
                    return NumberRules(
                        upperBound = threshold,
                        uppedInclusive = !optionValue.exclusive
                    )
                }
                else -> {
                    throw IllegalArgumentException(
                        "Option ${option.name} is not a number range option."
                    )
                }
            }
        }
    }
}

private fun Option.isMin(): Boolean {
    return isOption(this, OptionsProto.min)
}

private fun Option.isMax(): Boolean {
    return isOption(this, OptionsProto.max)
}

private fun Option.isRange(): Boolean {
    return isOption(this, OptionsProto.range)
}

private fun isOption(
    option: Option,
    generatedOpt: GeneratedMessage.GeneratedExtension<*, *>
): Boolean {
    val descriptor = generatedOpt.descriptor
    return option.name == descriptor.name && option.number == descriptor.number
}
