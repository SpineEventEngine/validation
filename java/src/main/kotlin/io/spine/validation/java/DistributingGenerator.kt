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

import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import io.spine.base.Identifier.newUuid
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.MethodCall
import io.spine.protodata.codegen.java.This
import io.spine.protodata.isMap
import io.spine.validate.ConstraintViolation
import io.spine.validation.ErrorMessage
import javax.lang.model.element.Modifier

/**
 * A code generator that distributes the associated simple validation rule to elements of
 * a repeated field.
 *
 * A rule applied to a repeated fields, i.e. a list or a map, may be applied to the fields as
 * a whole, or to individual elements of the field. In the first case, no extra modifications to
 * code generation are required. In the second case, this generator creates code that iterated over
 * the elements and applies the validation rule to them.
 */
internal class DistributingGenerator(
    ctx: GenerationContext,
    delegateCtor: (GenerationContext) -> CodeGenerator
) : CodeGenerator(ctx) {

    private val element = Literal("element")
    private val elementContext = ctx.copy(elementReference = element)
    private val field = ctx.fieldFromSimpleRule!!
    private val delegate = delegateCtor(elementContext)
    private val ruleId = "${field.name.value}_${newUuid().filter { it.isJavaIdentifierPart() }}"
    private val methodName = "validate_$ruleId"
    private val violationsName = "violations_of_$ruleId"
    private val violationsType = object : TypeToken<ImmutableList<ConstraintViolation>>() {}.type

    override fun supportingMembers(): CodeBlock {
        val typeName = ctx.typeSystem.javaTypeName(field.type)
        val fieldAccessor = ctx.fieldOrElement!!
        val collection = if (field.isMap()) {
            MethodCall(fieldAccessor, "values")
        } else {
            fieldAccessor
        }
        val body = CodeBlock
            .builder()
            .addStatement(
                "\$T<\$T> \$L = \$T.builder()",
                ImmutableList.Builder::class.java,
                ConstraintViolation::class.java,
                ctx.violationsList,
                ImmutableList::class.java
            ).beginControlFlow("for (\$L \$L : \$L)", typeName, element, collection)
            .add(delegate.code())
            .endControlFlow()
            .addStatement("return \$L.build()", elementContext.violationsList)
            .build()
        val otherMembers = delegate.supportingMembers()
        val groupingMethod = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PRIVATE)
            .returns(violationsType)
            .addCode(body)
            .build()
            .toString()
        return otherMembers
            .toBuilder()
            .add(groupingMethod)
            .build()
    }

    override fun prologue(): CodeBlock {
        val methodCall = MethodCall(This, methodName)
        return CodeBlock.builder()
            .addStatement("\$T \$L = \$L", violationsType, violationsName, methodCall)
            .build()
    }

    override fun condition(): Expression {
        return MethodCall(Literal(violationsName), "isEmpty")
    }

    override fun error(): ErrorMessage =
        delegate.error()

    override fun createViolation(): CodeBlock {
        val violations = Literal(ctx.violationsList)
        val methodCall = MethodCall(violations, "addAll", listOf(Literal(violationsName)))
        return CodeBlock
            .builder()
            .addStatement(methodCall.toCode())
            .build()
    }
}
