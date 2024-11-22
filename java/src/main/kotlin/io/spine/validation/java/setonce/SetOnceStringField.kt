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

import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiIfStatement
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.java.AnElement
import io.spine.protodata.java.Expression
import io.spine.protodata.type.TypeSystem
import io.spine.tools.psi.java.method

/**
 * Renders Java code to support `(set_once)` option for the given string [field].
 *
 * @param field The string field that declared the option.
 * @param typeSystem The type system to resolve types.
 */
internal class SetOnceStringField(
    field: Field,
    typeSystem: TypeSystem
) : SetOnceJavaConstraints<String>(field, typeSystem) {

    init {
        check(field.type.primitive == PrimitiveType.TYPE_STRING) {
            "`${javaClass.simpleName}` handles only number fields. " +
                    "The passed field: `$field`."
        }
    }

    override fun defaultOrSame(
        currentValue: Expression<String>,
        newValue: Expression<String>
    ): Expression<Boolean> = Expression(
        "$currentValue.isEmpty() || $currentValue.equals($newValue)"
    )

    override fun PsiClass.renderConstraints() {
        alterSetter()
        alterStringBytesSetter()
        alterMessageMerge()
        alterBytesMerge(
            currentValue = Expression(fieldGetter),
            readerStartsWith = AnElement("${fieldName}_ = input.readStringRequireUtf8();")
        )
    }

    /**
     * Alters a setter that accepts a value.
     *
     * For example:
     *
     * ```
     * public Builder setMyString(String value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression(fieldGetter),
            newValue = Expression("value")
        )
        val setter = method(fieldSetterName).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * Alters a string-specific setter that accepts a `ByteString`.
     *
     * For example:
     *
     * ```
     * public Builder setMyStingBytes(com.google.protobuf.ByteString value)
     * ```
     */
    private fun PsiClass.alterStringBytesSetter() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression("${fieldGetterName}Bytes()"),
            newValue = Expression("value")
        )
        val bytesSetter = method("${fieldSetterName}Bytes").body!!
        bytesSetter.addAfter(precondition, bytesSetter.lBrace)
    }

    /**
     * Alters the message-based merge method to modify merging of a string field.
     *
     * During the message merge, all primitives delegate updating the field value
     * to their simple setters. Exception here is `string` type, which does it in-place.
     * So, we have to add a check there.
     */
    private fun PsiClass.alterMessageMerge() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression(fieldGetter),
            newValue = Expression("other.$fieldGetter")
        )
        val mergeFromMessage = methodWithSignature(
            "public Builder mergeFrom(${declaringMessage.canonical} other)"
        ).body!!
        val fieldCheck = mergeFromMessage.deepSearch(
            AnElement("if (!other.$fieldGetter.isEmpty())")
        ) as PsiIfStatement
        val fieldProcessing = (fieldCheck.thenBranch!! as PsiBlockStatement).codeBlock
        fieldProcessing.addAfter(precondition, fieldProcessing.lBrace)
    }
}
