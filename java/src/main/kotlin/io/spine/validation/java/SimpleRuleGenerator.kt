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
import io.spine.protodata.Type
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.MethodCall
import io.spine.validation.ComparisonOperator.EQUAL
import io.spine.validation.ComparisonOperator.GREATER_OR_EQUAL
import io.spine.validation.ComparisonOperator.GREATER_THAN
import io.spine.validation.ComparisonOperator.LESS_OR_EQUAL
import io.spine.validation.ComparisonOperator.LESS_THAN
import io.spine.validation.ComparisonOperator.NOT_EQUAL
import io.spine.validation.ErrorMessage
import io.spine.validation.SimpleRule.OperatorKindCase.CUSTOM_OPERATOR
import io.spine.validation.SimpleRule.OperatorKindCase.OPERATOR

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
internal open class SimpleRuleGenerator(
    ctx: GenerationContext
) : CodeGenerator(ctx) {

    protected val rule = ctx.rule.simple
    protected val field = ctx.fieldFromSimpleRule!!

    protected val fieldValue: MethodCall by lazy { ctx.msg.field(field).getter }
    private val otherValue: Expression by lazy { ctx.typeSystem.valueToJava(rule.otherValue) }

    override fun condition(): Expression {
        val type = field.type
        val signs = if (type.kindCase == Type.KindCase.PRIMITIVE) {
            PRIMITIVE_COMPARISON_OPS
        } else {
            OBJECT_COMPARISON_OPS
        }
        val compare = signs[rule.operator] ?: throw IllegalStateException(
            "Unsupported operation `${rule.operator}` for type `$type`."
        )
        return Literal(compare(fieldValue.toCode(), otherValue.toCode()))
    }

    override fun error(): ErrorMessage {
        val actualValue = ClassName(String::class).call("valueOf", listOf(fieldValue))
        return ErrorMessage.forRule(
            rule.errorMessage,
            actualValue.toCode(),
            otherValue.toCode()
        )
    }

    override fun createViolation(): CodeBlock =
        error().createViolation(field, fieldValue, ctx.violationsList)
}

internal fun generatorForSimple(ctx: GenerationContext): CodeGenerator {
    val rule = ctx.rule.simple
    return when (rule.operatorKindCase) {
        OPERATOR -> SimpleRuleGenerator(ctx)
        CUSTOM_OPERATOR -> generatorForCustom(ctx)
        else -> throw IllegalStateException(
            "Invalid rule: `$rule`. No operator is set."
        )
    }
}
