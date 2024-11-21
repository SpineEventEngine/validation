/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.validation.java

import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import com.google.protobuf.Message
import com.squareup.javapoet.CodeBlock
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.extractPrimitiveType
import io.spine.protodata.ast.extractTypeName
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.backend.SecureRandomString
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.This
import io.spine.protodata.java.toJavaClass
import io.spine.string.titleCase
import io.spine.tools.java.codeBlock
import io.spine.tools.java.methodSpec
import io.spine.validate.ConstraintViolation
import io.spine.validation.ErrorMessage
import javax.lang.model.element.Modifier

/**
 * A code generator that distributes the associated simple validation rule to elements of
 * a repeated field.
 *
 * A rule applied to repeated fields, i.e., a list or a map, may be applied to the fields as
 * a whole, or to individual elements of the field.
 *
 * In the first case, no extra modifications to code generation are required.
 *
 * In the second case, this generator creates the code that iterates over
 * the elements and applies the validation rule to them.
 */
internal class DistributingGenerator(
    ctx: GenerationContext,
    delegateCtor: (GenerationContext) -> CodeGenerator
) : CodeGenerator(ctx) {

    private val element = ReadVar<Any>("element")
    private val elementContext = ctx.copy(elementReference = element)
    private val field = ctx.simpleRuleField
    private val delegate = delegateCtor(elementContext)
    private val ruleId = field.ruleId()
    private val methodName = "validate$ruleId"
    private val violationsName = ReadVar<ImmutableList<ConstraintViolation>>("violationsOf$ruleId")
    private val violationsType = object : TypeToken<ImmutableList<ConstraintViolation>>() {}.type

    override fun supportingMembers(): CodeBlock {
        val typeName = typeName()
        val collection = iterableExpression()
        val body = codeBlock {
            addStatement(
                "\$T<\$T> \$L = \$T.builder()",
                ImmutableList.Builder::class.java,
                ConstraintViolation::class.java,
                ctx.violationList,
                ImmutableList::class.java
            )
            beginControlFlow("for (\$L \$L : \$L)", typeName, element, collection)
            add(delegate.code())
            endControlFlow()
            addStatement("return \$L.build()", elementContext.violationList)
        }
        val otherMembers = delegate.supportingMembers()

        val groupingMethod = methodSpec(methodName) {
            addModifiers(Modifier.PRIVATE)
            returns(violationsType)
            addCode(body)
        }.toString()

        return otherMembers.toBuilder()
            .add(groupingMethod)
            .build()
    }

    private fun typeName(): ClassName {
        val name = field.type.extractTypeName()
        val typeName = if (name != null) {
            ctx.typeConvention.declarationFor(name).name
        } else {
            val primitiveType = field.type.extractPrimitiveType()
            check(primitiveType != null) {
                "The field `${field.qualifiedName}` is not of" +
                        " a primitive type (`${field.type.name}`)."
            }
            primitiveType.toJavaClass()
        }
        return typeName
    }

    private fun iterableExpression(): Expression<*> {
        val fieldAccessor = ctx.fieldOrElement!!
        return if (field.isMap) {
            MethodCall<Collection<*>>(fieldAccessor, "values")
        } else {
            fieldAccessor
        }
    }

    override fun prologue(): CodeBlock = codeBlock {
        val violations = MethodCall<ImmutableList<ConstraintViolation>>(This<Message>(), methodName)
        addStatement("\$T \$L = \$L", violationsType, violationsName, violations)
    }

    override fun condition(): Expression<Boolean> {
        return MethodCall(violationsName, "isEmpty")
    }

    override fun error(): ErrorMessage =
        delegate.error()

    override fun createViolation(): CodeBlock = codeBlock {
        val methodCall = MethodCall<Boolean>(ctx.violationList, "addAll", violationsName)
        addStatement(methodCall.code)
    }
}

private const val RANDOM_STR_BYTES = 10

private fun Field.ruleId(): String {
    val random = SecureRandomString.generate(RANDOM_STR_BYTES).filter { it.isJavaIdentifierPart() }
    val fieldRef = name.value.titleCase()
    return fieldRef + "_" + random
}
