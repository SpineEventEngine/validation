/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.validation.java

import com.squareup.javapoet.CodeBlock
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.Type
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.Literal
import io.spine.protodata.java.call
import io.spine.protodata.value.Value
import io.spine.string.shortly
import io.spine.tools.java.codeBlock
import io.spine.validation.ComparisonOperator.EQUAL
import io.spine.validation.ComparisonOperator.GREATER_OR_EQUAL
import io.spine.validation.ComparisonOperator.GREATER_THAN
import io.spine.validation.ComparisonOperator.LESS_OR_EQUAL
import io.spine.validation.ComparisonOperator.LESS_THAN
import io.spine.validation.ComparisonOperator.NOT_EQUAL
import io.spine.validation.ErrorMessage
import io.spine.validation.SimpleRule
import io.spine.validation.SimpleRule.OperatorKindCase.CUSTOM_OPERATOR
import io.spine.validation.SimpleRule.OperatorKindCase.OPERATOR
import io.spine.validation.UnsetValue
import io.spine.validation.extractType

/**
 * Java code comparing two objects.
 *
 * This map describes correspondence between the [io.spine.validation.ComparisonOperator]s and
 * the way they are rendered in Java for reference types.
 */
private val OBJECT_COMPARISON_OPS = mapOf(
    EQUAL to { left: String, right: String -> "$left.equals($right)" },
    NOT_EQUAL to { left: String, right: String -> "!$left.equals($right)" },
)

/**
 * Java code comparing two primitive (numeric) types.
 *
 * This map describes correspondence between the [io.spine.validation.ComparisonOperator]s and
 * the way they are rendered in Java for Java primitive types.
 */
private val PRIMITIVE_COMPARISON_OPS = mapOf(
    EQUAL to { left: String, right: String -> "$left == $right" },
    NOT_EQUAL to { left: String, right: String -> "$left != $right" },
    GREATER_THAN to { left: String, right: String -> "$left > $right" },
    LESS_THAN to { left: String, right: String -> "$left < $right" },
    GREATER_OR_EQUAL to { left: String, right: String -> "$left >= $right" },
    LESS_OR_EQUAL to { left: String, right: String -> "$left <= $right" }
)

/**
 * A generator for code which checks a simple one-field rule.
 */
internal open class SimpleRuleGenerator(ctx: GenerationContext) : CodeGenerator(ctx) {

    protected val rule: SimpleRule = ctx.rule.simple
    private val ignoreIfNotSet  = rule.ignoredIfUnset
    protected val field = ctx.simpleRuleField
    private val otherValue: Expression<*>? = ctx.otherValueAsCode

    override fun code(): CodeBlock {
        val check = super.code()
        val defaultValue = defaultFieldValue()
        if (!ignoreIfNotSet || defaultValue == null) {
            return check
        }
        return codeBlock {
            val condition = createCondition(defaultValue)
            beginControlFlow("if ($condition)")
            add(check)
            endControlFlow()
        }
    }

    private fun defaultFieldValue(): Value? = with(ctx) {
        val field = simpleRuleField
        return if (isElement) {
            UnsetValue.singular(field.directOrElementType())
        } else {
            UnsetValue.forField(field)
        }
    }

    private fun createCondition(defaultValue: Value): String {
        val sign = selectSigns()[NOT_EQUAL]!!
        val defaultValueExpression = ctx.valueConverter.valueToCode(defaultValue)
        val condition = sign(ctx.fieldOrElement!!.toCode(), defaultValueExpression.toCode())
        return condition
    }

    override fun condition(): Expression<*> {
        checkNotNull(otherValue) {
            "Expected the rule to specify `simple.other_value`, but was: $rule"
        }
        val type = field.directOrElementType()
        val signs = selectSigns()
        val compare = signs[rule.operator] ?: error(
            "Unsupported operation `${rule.operator}` for the type `$type`."
        )
        checkNotNull(ctx.fieldOrElement) {
            "There is no field value for the rule: `${rule.shortly()}`."
        }
        return Literal(compare(ctx.fieldOrElement!!.toCode(), otherValue.toCode()))
    }

    private fun fieldIsJavaObject(): Boolean =
        !field.refersToJavaPrimitive() || ((field.isList || field.isMap) && !ctx.isElement)

    private fun selectSigns() = if (fieldIsJavaObject()) {
        OBJECT_COMPARISON_OPS
    } else {
        PRIMITIVE_COMPARISON_OPS
    }

    override fun error(): ErrorMessage {
        val actualValue = ClassName(String::class)
            .call<Any>("valueOf", listOf(ctx.fieldOrElement!!))
        return ErrorMessage.forRule(
            rule.errorMessage,
            actualValue.toCode(),
            otherValue?.toCode()
        )
    }

    override fun createViolation(): CodeBlock =
        error().createViolation(ctx)
}

internal fun generatorForSimple(ctx: GenerationContext): CodeGenerator {
    val distribute = ctx.rule.simple.distribute
    val field = ctx.simpleRuleField
    return if (distribute && (field.isList || field.isMap)) {
        DistributingGenerator(ctx) {
            generatorForSingular(it)
        }
    } else {
        generatorForSingular(ctx)
    }
}

private fun generatorForSingular(ctx: GenerationContext): CodeGenerator {
    val rule = ctx.rule.simple
    return when (rule.operatorKindCase) {
        OPERATOR -> SimpleRuleGenerator(ctx)
        CUSTOM_OPERATOR -> generatorForCustom(ctx)
        else -> error("Invalid rule: `$rule`. No operator is set.")
    }
}

@Suppress("ReturnCount")
private fun Field.refersToJavaPrimitive(): Boolean {
    if (type.isList) {
        return type.list.isPrimitive
    }
    if (type.isMap) {
        return type.map.valueType.isPrimitive
    }
    if (!type.isPrimitive) {
        return false
    }
    return when (type.primitive) {
        TYPE_STRING, TYPE_BYTES -> false
        else -> true
    }
}

private fun Field.directOrElementType(): Type = type.extractType()
