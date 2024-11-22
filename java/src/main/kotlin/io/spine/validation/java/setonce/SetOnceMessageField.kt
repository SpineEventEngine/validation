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

package io.spine.validation.java.setonce

import com.google.protobuf.Message
import com.intellij.psi.PsiClass
import io.spine.protodata.ast.Field
import io.spine.protodata.java.AnElement
import io.spine.protodata.java.Expression
import io.spine.protodata.java.javaClassName
import io.spine.protodata.type.TypeSystem
import io.spine.tools.psi.java.method

/**
 * Renders Java code to support `(set_once)` option for the given message [field].
 *
 * @param field The message field that declared the option.
 * @param typeSystem The type system to resolve types.
 */
internal class SetOnceMessageField(
    field: Field,
    typeSystem: TypeSystem
) : SetOnceJavaConstraints<Message>(field, typeSystem) {

    init {
        check(field.type.isMessage) {
            "`${javaClass.simpleName}` handles only message fields. " +
                    "The passed field: `$field`."
        }
    }

    private val fieldTypeClass = field.type.message.javaClassName(typeSystem)

    @Suppress("MaxLineLength") // Easier to read the expression.
    override fun defaultOrSame(
        currentValue: Expression<Message>,
        newValue: Expression<Message>
    ): Expression<Boolean> = Expression(
        "$currentValue.equals($fieldTypeClass.getDefaultInstance()) || $currentValue.equals($newValue)"
    )

    override fun PsiClass.renderConstraints() {
        alterSetter()
        alterBuilderSetter()
        alterFieldMerge()
        alterBytesMerge(
            currentValue = Expression(fieldGetter),
            readerStartsWith = AnElement("input.readMessage"),
            readerContains = AnElement("${fieldGetterName}FieldBuilder().getBuilder()"),
        )
    }

    /**
     * Alters a setter that accepts a message.
     *
     * For example:
     *
     * ```
     * public Builder setMyMessage(MyMessage value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression(fieldGetter),
            newValue = Expression("value")
        )
        val setter = methodWithSignature(
            "public Builder $fieldSetterName($fieldTypeClass value)"
        ).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * Alters a setter that accepts a message builder.
     *
     * For example:
     *
     * ```
     * public Builder setMyMessage(MyMessage.Builder builderForValue)
     * ```
     */
    private fun PsiClass.alterBuilderSetter() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression(fieldGetter),
            newValue = Expression("builderForValue.build()")
        )
        val setter = methodWithSignature(
            "public Builder $fieldSetterName($fieldTypeClass.Builder builderForValue)"
        ).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * Alters a field merge method that accepts a message.
     *
     * For example:
     *
     * ```
     * public Builder mergeMyMessage(MyMessage value)
     * ```
     */
    private fun PsiClass.alterFieldMerge() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression(fieldGetter),
            newValue = Expression("value")
        )
        val merge = method("merge$fieldNameCamel").body!!
        merge.addAfter(precondition, merge.lBrace)
    }
}
