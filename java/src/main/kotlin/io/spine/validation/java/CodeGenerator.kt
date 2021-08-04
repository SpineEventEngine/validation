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

package io.spine.validation.java

import com.squareup.javapoet.CodeBlock
import io.spine.logging.Logging
import io.spine.protodata.codegen.java.Expression
import io.spine.validation.ErrorMessage
import io.spine.validation.Rule.KindCase.COMPOSITE
import io.spine.validation.Rule.KindCase.SIMPLE

/**
 * A Java code generator for a validation rule.
 */
internal abstract class CodeGenerator(
    protected val ctx: GenerationContext
) : Logging {

    /**
     * Whether or not this code generator is capable of generating validation code for
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
            _debug().log("Standard Java renderer cannot generate rule: ${ctx.rule}")
            _debug().log("Skipping...")
            return CodeBlock.of("")
        }
        val binaryCondition = condition()
        return CodeBlock
            .builder()
            .add(prologue())
            .beginControlFlow("if (!(\$L))", binaryCondition)
            .add(createViolation())
            .endControlFlow()
            .build()
    }

    /**
     * Generated code which does preparations before the validation checks can be performed.
     */
    open fun prologue(): CodeBlock =
        CodeBlock.of("")

    /**
     * Obtains an expression checking if the rule is violated.
     *
     * The expression evaluates to `true` if there is a violation and to `false` otherwise.
     */
    abstract fun condition(): Expression

    /**
     * Forms an error message for the found violation.
     */
    abstract fun error(): ErrorMessage

    /**
     * Constructs code which creates a `ConstrainViolation` and puts it into a list of violations.
     *
     * Later, the violations will be reported to the caller.
     */
    protected abstract fun createViolation(): CodeBlock
}

/**
 * Creates a code generator for a validation rule.
 */
internal fun generatorFor(ctx: GenerationContext): CodeGenerator = with(ctx) {
    when (rule.kindCase) {
        SIMPLE -> generatorForSimple(this)
        COMPOSITE -> CompositeRuleGenerator(ctx)
        else -> throw IllegalArgumentException("Empty rule.")
    }
}
