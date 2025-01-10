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

package io.spine.validation.java

import com.squareup.javapoet.CodeBlock
import io.spine.protodata.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.protodata.ast.OneofName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.field

/**
 * A code generator for the `(is_required)` constraint.
 *
 * The constraint applies to a `oneof` group and enforces an alternative to be set.
 * The generated code checks that the `oneof`'s case is one of the alternatives,
 * i.e., the `oneof` is initialized with an option.
 */
internal class RequiredOneofGenerator(
    private val name: OneofName,
    ctx: GenerationContext
) : CodeGenerator(ctx) {

    private val rule = ctx.rule.messageWide

    override fun condition(): Expression<Boolean> {
        val casePropertyName = "${name.value}_case"
        val pseudoField = ctx.msg.field(casePropertyName, CARDINALITY_SINGLE)
        val getter = pseudoField.getter<Any>()
        val numberGetter = getter.chain<Number>("getNumber")
        return Expression("$numberGetter != 0")
    }

    override fun error() = rule.errorMessage to emptyMap<Expression<*>, Expression<*>>()

    override fun createViolation(): CodeBlock {
        val error = error()
        val violation = buildViolation(
            error.first,
            ctx.validatedType,
            ctx.fieldFromSimpleRule,
            ctx.fieldOrElement,
            ignoreCardinality = ctx.isElement,
            error.second
        )
        return addViolation(violation, ctx.violationList)
    }
}
