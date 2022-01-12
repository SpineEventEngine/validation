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
import io.spine.protodata.Field
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.MessageReference
import io.spine.protodata.codegen.java.MethodCall
import io.spine.protodata.qualifiedName
import io.spine.validate.ValidationError
import java.util.*

/**
 * Generates code for the [RecursiveValidation] operator.
 */
internal class ValidateGenerator(ctx: GenerationContext) : SimpleRuleGenerator(ctx) {

    private val violationsVariable =
        Literal("generated_validationError_${ctx.fieldFromSimpleRule!!.name.value}")

    init {
        val field = ctx.fieldFromSimpleRule!!
        val fieldType = field.type
        check(fieldType.hasMessage()) {
            "(validate) only supports `Message` types but field " +
                    "`${field.declaringType.qualifiedName()}.${field.name.value}` " +
                    "has type `$fieldType`."
        }
    }

    override fun prologue(): CodeBlock {
        val violations = MethodCall(ctx.fieldOrElement!!, "validate")
        return CodeBlock.builder()
            .addStatement(
                "\$T<\$T> \$L = \$L",
                Optional::class.java, ValidationError::class.java, violationsVariable, violations
            ).build()
    }

    override fun condition(): Expression =
        Literal("!" + MethodCall(violationsVariable, "isPresent"))


    override fun createViolation(): CodeBlock {
        val validationError = MethodCall(violationsVariable, "get")
        val violations = MessageReference(validationError.toCode())
            .field("constraint_violation", Field.CardinalityCase.LIST)
            .getter
        return error().createParentViolation(ctx, violations)
    }
}
