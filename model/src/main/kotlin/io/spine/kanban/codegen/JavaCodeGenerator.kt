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

@file:JvmName("JavaCodeGeneration")

package io.spine.kanban.codegen

import com.squareup.javapoet.CodeBlock
import io.spine.protodata.Type.KindCase.PRIMITIVE
import io.spine.protodata.TypeName
import io.spine.validation.ComparisonOperator.EQUAL
import io.spine.validation.ComparisonOperator.GREATER_OR_EQUAL
import io.spine.validation.ComparisonOperator.GREATER_THAN
import io.spine.validation.ComparisonOperator.LESS_OR_EQUAL
import io.spine.validation.ComparisonOperator.LESS_THAN
import io.spine.validation.ComparisonOperator.NOT_EQUAL
import io.spine.validation.LogicalOperator.AND
import io.spine.validation.LogicalOperator.OR
import io.spine.validation.LogicalOperator.XOR
import io.spine.validation.Rule
import io.spine.validation.Rule.KindCase.COMPOSITE
import io.spine.validation.Rule.KindCase.SIMPLE

/**
 * Java code comparing two objects.
 */
private val OBJECT_COMPARISON_OPS = mapOf(
    EQUAL to { left: String, right: String -> "$left.equals($right)" },
    NOT_EQUAL to { left: String, right: String -> "!$left.equals($right)" }
)

/**
 * Java code comparing two primitive (numeric) types.
 */
private val PRIMITIVE_COMPARISON_OPS = mapOf(
    EQUAL to { left: String, right: String -> "$left == $right" },
    NOT_EQUAL to { left: String, right: String -> "$left != $right" },
    GREATER_THAN to { left: String, right: String -> "$left > $right" },
    LESS_THAN to { left: String, right: String -> "$left < $right" },
    GREATER_OR_EQUAL to { left: String, right: String -> "$left < $right" },
    LESS_OR_EQUAL to { left: String, right: String -> "$left < $right" }
)

/**
 * Java code comparing two boolean values.
 */
private val BOOLEAN_OPS = mapOf(
    AND to { left: String, right: String -> "($left) && ($right)" },
    OR to { left: String, right: String -> "($left) || ($right)" },
    XOR to { left: String, right: String -> "($left) ^ ($right)" }
)

/**
 * A Java code generator for a validation rule.
 */
internal sealed interface JavaCodeGenerator {

    /**
     * Obtains the code checking the rule.
     *
     * Implementations report any found violations.
     */
    fun code(): CodeBlock

    /**
     * Obtains an expression checking if the rule is violated.
     *
     * The expression evaluates to `true` if there is a violation and to `false` otherwise.
     */
    fun condition(): Expression

    /**
     * Forms an error message for the found violation.
     */
    fun error(): ErrorMessage
}

/**
 * A generator for code which checks a simple one-field rule.
 */
private class SimpleRuleGenerator(
    private val ctx: GenerationContext
) : JavaCodeGenerator {

    private val rule = ctx.rule.simple
    private val field = rule.field

    private val fieldValue: Expression by lazy { ctx.msg.field(field).getter }
    private val otherValue: Expression by lazy {  ctx.typeSystem.valueToJava(rule.otherValue) }

    override fun code(): CodeBlock {
        val binaryCondition = condition()
        val errorMsg = error()
        return CodeBlock
            .builder()
            .beginControlFlow("if (!(\$L))", binaryCondition)
            .add(errorMsg.createViolation(field, fieldValue, ctx.violationsList))
            .endControlFlow()
            .build()
    }

    override fun condition(): Expression {
        val field = rule.field
        val type = field.type
        val signs = if (type.kindCase == PRIMITIVE) {
            PRIMITIVE_COMPARISON_OPS
        } else {
            OBJECT_COMPARISON_OPS
        }
        val compare = signs[rule.sign]!!
        return Literal(compare(fieldValue.toCode(), otherValue.toCode()))
    }

    override fun error(): ErrorMessage {
        return ErrorMessage.forRule(rule.errorMessage, fieldValue.toCode(), otherValue.toCode())
    }
}

/**
 * A generator for code which checks a composite rule.
 *
 * A composite rule may consist of several simple rules applied to one or many fields.
 */
private class CompositeRuleGenerator(
    private val ctx: GenerationContext,
) : JavaCodeGenerator {

    private val left = generatorFor(ctx.withRule(ctx.rule.composite.left))
    private val right = generatorFor(ctx.withRule(ctx.rule.composite.right))

    override fun code(): CodeBlock {
        val binaryCondition = condition()
        val error = error()
        return CodeBlock
            .builder()
            .beginControlFlow("if (!(%s))", binaryCondition)
            .add(error.createCompositeViolation(ctx.declaringType, ctx.violationsList))
            .endControlFlow()
            .build()
    }

    override fun condition(): Expression = with(ctx) {
        val composite = rule.composite
        val left = left.condition()
        val right = right.condition()
        val binaryOp = BOOLEAN_OPS[composite.operator]!!
        return Literal(binaryOp(left.toCode(), right.toCode()))
    }

    override fun error(): ErrorMessage {
        val composite = ctx.rule.composite
        val format = composite.errorMessage
        val operation = composite.operator
        return ErrorMessage.forComposite(format, left.error(), right.error(), operation)
    }
}

/**
 * Creates a code generator for a validation rule.
 */
internal fun generatorFor(ctx: GenerationContext): JavaCodeGenerator = with(ctx) {
    when (rule.kindCase) {
        SIMPLE -> SimpleRuleGenerator(ctx)
        COMPOSITE -> CompositeRuleGenerator(ctx)
        else -> throw IllegalArgumentException("Empty rule.")
    }
}

/**
 * Context of a [JavaCodeGenerator].
 */
internal data class GenerationContext(
    /**
     * The rule for which the code is generated.
     */
    val rule: Rule,

    /**
     * A reference to the validated message.
     */
    val msg: MessageReference,

    /**
     * The Protobuf types known to the application.
     */
    val typeSystem: TypeSystem,

    /**
     * The type of the validated message.
     */
    val declaringType: TypeName,

    /**
     * A reference to the mutable violations list, which accumulates all the constraint violations.
     */
    val violationsList: String
) {

    /**
     * Obtains the same context but with the given validation [rule].
     */
    fun withRule(rule: Rule): GenerationContext = copy(rule = rule)
}
