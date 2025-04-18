/*
 * Copyright 2025, TeamDev. All rights reserved.
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

@file:JvmName("JavaCodeGeneration")

package io.spine.validation.java.rule

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import io.spine.logging.WithLogging
import io.spine.protodata.java.Expression
import io.spine.tools.java.codeBlock
import io.spine.validation.ErrorMessage
import io.spine.validation.Rule.KindCase.COMPOSITE
import io.spine.validation.Rule.KindCase.MESSAGE_WIDE
import io.spine.validation.Rule.KindCase.SIMPLE

/**
 * A Java code generator for a validation rule.
 */
internal abstract class CodeGenerator(
    protected val ctx: GenerationContext
) : WithLogging {

    /**
     * Tells whether this code generator is capable of generating validation code for
     * the associated validation rule.
     *
     * If the rule has a custom operator, the generator might not support it. Otherwise,
     * the generator must be able to generate code.
     */
    open val canGenerate: Boolean = true

    /**
     * Obtains the code checking the rule.
     *
     * Implementations report any found violations.
     */
    open fun code(): CodeBlock {
        if (!canGenerate) {
            logger().atDebug().log {
                "Standard Java renderer cannot generate rule: ${ctx.rule}." +
                        System.lineSeparator() +
                        "Skipping..."
            }
            return noCode()
        }
        val binaryCondition = condition()
        return codeBlock {
            add(prologue())
            beginControlFlow("if (!(\$L))", binaryCondition)
            add(createViolation())
            endControlFlow()
        }
    }

    /**
     * Fields to be inserted into the message class scope.
     *
     * Such fields may cache intermediate validation results, etc.
     */
    open fun supportingFields(): List<FieldSpec> = emptyList()

    /**
     * Methods to be inserted into the message class scope.
     *
     * Such methods may contain validation subroutines, etc.
     */
    open fun supportingMethods(): List<MethodSpec> = emptyList()

    /**
     * Generated code which does preparations before the validation checks can be performed.
     */
    open fun prologue(): CodeBlock = noCode()

    /**
     * Obtains an expression checking if the rule is violated.
     *
     * The expression evaluates to `true` if there is a violation and to `false` otherwise.
     */
    abstract fun condition(): Expression<Boolean>

    /**
     * Forms an error message for the found violation.
     */
    abstract fun error(): ErrorMessage

    /**
     * Constructs code which creates a `ConstrainViolation` and puts it into a list of violations.
     *
     * Later the violations will be reported to the caller.
     */
    protected abstract fun createViolation(): CodeBlock
}

/**
 * Creates a code generator for a validation rule.
 */
internal fun generatorFor(ctx: GenerationContext): CodeGenerator =
    when (ctx.rule.kindCase) {
        SIMPLE -> generatorForSimple(ctx)
        COMPOSITE -> CompositeRuleGenerator(ctx)
        MESSAGE_WIDE -> generatorForCustom(ctx)
        else -> throw IllegalArgumentException(
            "Unsupported kind detected in the rule: `${ctx.rule}`."
        )
    }

/**
 * Obtains a code block with an empty string.
 */
internal fun noCode(): CodeBlock = CodeBlock.of("")
