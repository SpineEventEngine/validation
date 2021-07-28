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

import com.google.common.collect.ImmutableSet
import com.squareup.javapoet.CodeBlock
import io.spine.protobuf.AnyPacker.unpack
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.MethodCall
import io.spine.validate.ValidationError
import io.spine.validation.DistinctCollection
import io.spine.validation.RecursiveValidation

private class DistinctGenerator(ctx: GenerationContext) : SimpleRuleGenerator(ctx) {

    override fun condition(): Expression {
        return equalsOperator(
            fieldValue.chain("size"),
            ClassName(ImmutableSet::class)
                .call("copyOf", listOf(fieldValue))
                .chain("size")
        )
    }

    private fun equalsOperator(left: Expression, right: Expression): Expression {
        return Literal(left.toCode() + " == " + right.toCode())
    }
}

private class ValidateGenerator(ctx: GenerationContext) : SimpleRuleGenerator(ctx) {

    private val violationsVariable: Literal
        get() = Literal("generated_validationError_${ctx.fieldFromSimpleRule!!.name.value}")

    private val childViolations: MethodCall
        get() = MethodCall(violationsVariable, "getAllConstraintViolation")

    override fun condition(): Expression =
        childViolations.chain("isEmpty")

    override fun prologue(): CodeBlock =
        CodeBlock.builder()
            .addStatement(
                "\$T \$L = ${fieldValue.chain("validate")}",
                ValidationError::class.java, violationsVariable
            ).build()

    override fun createViolation(): CodeBlock =
        error().createParentViolation(
            field,
            fieldValue,
            ctx.violationsList,
            childViolations
        )
}

private class UnsupportedRuleGenerator(
    ctx: GenerationContext,
    private val ruleName: String
) : CodeGenerator(ctx) {

    override val canGenerate: Boolean
        get() = false

    override fun condition(): Nothing = unsupported()

    override fun error(): Nothing = unsupported()

    override fun createViolation(): Nothing = unsupported()

    private fun unsupported(): Nothing {
        throw UnsupportedOperationException("Rule `$ruleName` is not supported.")
    }
}

internal fun generatorForCustom(ctx: GenerationContext): CodeGenerator {
    @Suppress("MoveVariableDeclarationIntoWhen") // For better readability.
    val feature = unpack(ctx.rule.simple.customOperator.feature)
    return when (feature) {
        is DistinctCollection -> DistinctGenerator(ctx)
        is RecursiveValidation -> ValidateGenerator(ctx)
        else -> UnsupportedRuleGenerator(ctx, feature::class.simpleName!!)
    }
}
