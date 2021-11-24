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

package io.spine.validation.java

import com.squareup.javapoet.CodeBlock
import io.spine.protodata.Field
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.isRepeated
import io.spine.validation.ComparisonOperator.EQUAL
import io.spine.validation.ComparisonOperator.GREATER_OR_EQUAL
import io.spine.validation.ComparisonOperator.GREATER_THAN
import io.spine.validation.ComparisonOperator.LESS_OR_EQUAL
import io.spine.validation.ComparisonOperator.LESS_THAN
import io.spine.validation.ComparisonOperator.NOT_EQUAL
import io.spine.validation.ErrorMessage
import io.spine.validation.SimpleRule.OperatorKindCase.CUSTOM_OPERATOR
import io.spine.validation.SimpleRule.OperatorKindCase.OPERATOR
import io.spine.validation.UnsetValue

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

    protected val rule = ctx.rule.simple
    private val ignoreIfNotSet  = rule.ignoredIfUnset
    protected val field = ctx.fieldFromSimpleRule!!
    private val otherValue = ctx.typeSystem.valueToJava(rule.otherValue)

    override fun code(): CodeBlock {
        val check = super.code()
        val defaultValue = with(ctx) {
            val field = fieldFromSimpleRule!!
            if (isElement) {
                UnsetValue.singular(field.type)
            } else {
                UnsetValue.forField(field)
            }
        }
        if (!ignoreIfNotSet || !defaultValue.isPresent) {
            return check
        }
        val sign = selectSigns()[NOT_EQUAL]!!
        val defaultValueExpression = ctx.typeSystem.valueToJava(defaultValue.get())
        val condition = sign(ctx.fieldOrElement!!.toCode(), defaultValueExpression.toCode())
        return CodeBlock
            .builder()
            .beginControlFlow("if ($condition)")
            .add(check)
            .endControlFlow()
            .build()
    }

    override fun condition(): Expression {
        val type = field.type
        val signs = selectSigns()
        val compare = signs[rule.operator] ?: throw IllegalStateException(
            "Unsupported operation `${rule.operator}` for type `$type`."
        )
        checkNotNull(ctx.fieldOrElement) { "There is no field value for rule: $rule" }
        return Literal(compare(ctx.fieldOrElement!!.toCode(), otherValue.toCode()))
    }

    private fun fieldIssJavaObject(): Boolean =
        !field.isJavaPrimitive() || (field.isRepeated() && !ctx.isElement)

    private fun selectSigns() = if (fieldIssJavaObject()) {
        OBJECT_COMPARISON_OPS
    } else {
        PRIMITIVE_COMPARISON_OPS
    }

    override fun error(): ErrorMessage {
        val actualValue = ClassName(String::class).call("valueOf", listOf(ctx.fieldOrElement!!))
        return ErrorMessage.forRule(
            rule.errorMessage,
            actualValue.toCode(),
            otherValue.toCode()
        )
    }

    override fun createViolation(): CodeBlock =
        error().createViolation(ctx)
}

internal fun generatorForSimple(ctx: GenerationContext): CodeGenerator {
    val distribute = ctx.rule.simple.distribute
    val field = ctx.fieldFromSimpleRule!!
    return if (distribute && field.isRepeated()) {
        DistributingGenerator(ctx) { generatorForSingular(it) }
    } else {
        generatorForSingular(ctx)
    }
}

private fun generatorForSingular(ctx: GenerationContext): CodeGenerator {
    val rule = ctx.rule.simple
    return when (rule.operatorKindCase) {
        OPERATOR -> SimpleRuleGenerator(ctx)
        CUSTOM_OPERATOR -> generatorForCustom(ctx)
        else -> throw IllegalStateException(
            "Invalid rule: `$rule`. No operator is set."
        )
    }
}

private fun Field.isJavaPrimitive(): Boolean {
    if (!type.hasPrimitive()) {
        return false
    }
    return when (type.primitive) {
        TYPE_STRING, TYPE_BYTES -> false
        else -> true
    }
}
