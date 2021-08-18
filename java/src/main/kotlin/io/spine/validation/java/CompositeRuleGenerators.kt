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
import io.spine.protobuf.Messages
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.validation.ErrorMessage
import io.spine.validation.LogicalOperator.AND
import io.spine.validation.LogicalOperator.OR
import io.spine.validation.LogicalOperator.XOR

/**
 * Java code comparing two boolean values.
 *
 * This map describes correspondence between the [io.spine.validation.LogicalOperator]s and
 * the way they are rendered in Java.
 */
private val BOOLEAN_OPS = mapOf(
    AND to { left: String, right: String -> "($left) && ($right)" },
    OR to { left: String, right: String -> "($left) || ($right)" },
    XOR to { left: String, right: String -> "($left) ^ ($right)" }
)

/**
 * A generator for code which checks a composite rule.
 *
 * A composite rule may consist of several simple rules applied to one or many fields.
 */
internal class CompositeRuleGenerator(ctx: GenerationContext) : CodeGenerator(ctx) {

    private fun left(ctx: GenerationContext) = generatorFor(ctx.left())
    private fun right(ctx: GenerationContext) = generatorFor(ctx.right())

    private fun GenerationContext.left() = copy(rule = rule.composite.left)
    private fun GenerationContext.right() = copy(rule = rule.composite.right)

    override fun canGenerate(): Boolean =
        left(ctx).canGenerate() && right(ctx).canGenerate()

    override fun condition(): Expression = with(ctx) {
        val composite = rule.composite
        val left = left(ctx).condition()
        val right = right(ctx).condition()
        val binaryOp = BOOLEAN_OPS[composite.operator]!!
        return Literal(binaryOp(left.toCode(), right.toCode()))
    }

    override fun error(): ErrorMessage {
        val composite = ctx.rule.composite
        val format = composite.errorMessage
        val operation = composite.operator
        val commonField = composite.field
        val fieldAccessor = if (Messages.isNotDefault(commonField)) {
            val found = ctx.lookUpField(commonField)
            ctx.getterFor(found).toCode()
        } else {
            ""
        }
        return ErrorMessage.forComposite(
            format,
            left(ctx).error(),
            right(ctx).error(),
            operation,
            fieldAccessor
        )
    }

    override fun createViolation(): CodeBlock =
        error().createCompositeViolation(ctx.declaringType, ctx.violationsList)

    override fun supportingMembers(): CodeBlock =
        CodeBlock.builder()
            .add(left(ctx).supportingMembers())
            .add(right(ctx).supportingMembers())
            .build()
}
