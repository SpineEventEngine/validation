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
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.java.javaClassName
import io.spine.tools.psi.java.method
import io.spine.validation.java.MessageWithFile

/**
 * Renders Java code to support `(set_once)` option for the given string [field].
 *
 * @property field The string field that declared the option.
 * @property message The message that contains the [field].
 */
internal class SetOnceStringField(
    field: Field,
    message: MessageWithFile
) : SetOncePrimitiveField(field, message) {

    private val messageTypeClass = message.message
        .javaClassName(message.fileHeader)
        .canonical

    init {
        check(field.type.primitive == TYPE_STRING) {
            "`${javaClass.simpleName}` handles only string fields. " +
                    "The passed field: `$field`. The declaring message: `${message.message}`."
        }
    }

    override fun PsiClass.doRender() {
        alterSetter()
        alterBytesSetter()
        alterMessageMerge()
        alterBytesMerge()
    }

    /**
     * ```
     * public Builder setIdBytes(com.google.protobuf.ByteString value)
     * ```
     */
    private fun PsiClass.alterBytesSetter() {
        val precondition = checkDefaultOrSame(
            currentValue = "${fieldGetterName}Bytes()",
            newValue = "value"
        )
        val bytesSetter = method("${fieldSetterName}Bytes").body!!
        bytesSetter.addAfter(precondition, bytesSetter.lBrace)
    }

    /**
     * ```
     * public Builder mergeFrom(io.spine.test.tools.validate.Student other)
     * ```
     */
    private fun PsiClass.alterMessageMerge() {
        val precondition = checkDefaultOrSame(
            currentValue = fieldGetter,
            newValue = "other.$fieldGetter"
        )
        val mergeFromMessage = getMethodBySignature(
            "public Builder mergeFrom($messageTypeClass other) {}"
        ).body!!
        val fieldCheck = mergeFromMessage.deepSearch(
            "if (!other.$fieldGetter.isEmpty())"
        ) as PsiIfStatement
        val fieldProcessing = (fieldCheck.thenBranch!! as PsiBlockStatement).codeBlock
        fieldProcessing.addAfter(precondition, fieldProcessing.lBrace)
    }
}
