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

import com.intellij.psi.PsiClass
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.isMessage
import io.spine.protodata.java.javaClassName
import io.spine.tools.psi.java.method
import io.spine.validation.java.MessageWithFile

/**
 * Renders Java code to support `(set_once)` option for the given message [field].
 *
 * @property field The message field that declared the option.
 * @property message The message that contains the [field].
 */
internal class SetOnceMessageField(
    field: Field,
    message: MessageWithFile
) : SetOnceJavaConstraints(field, message) {

    init {
        check(field.type.isMessage) {
            "`${javaClass.simpleName}` handles only message fields. " +
                    "The passed field: `$field`. The declaring message: `${message.message}`."
        }
    }

    private val fieldTypeClass = field.type.message
        .javaClassName(message.fileHeader)
        .canonical

    override fun PsiClass.doRender() {
        alterSetter()
        alterBuilderSetter()
        alterFieldMerge()
        alterBytesMerge(
            currentValue = fieldGetter,
            getFieldReading = {
                deepSearch(
                    startsWith = "input.readMessage",
                    contains = "${fieldGetterName}FieldBuilder().getBuilder()",
                )
            }
        )
    }

    /**
     * ```
     * public Builder setName(io.spine.test.tools.validate.Name value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = checkDefaultOrSame(currentValue = fieldGetter, newValue = "value")
        val setter = getMethodBySignature(
            "public Builder $fieldSetterName($fieldTypeClass value)"
        ).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * ```
     * public Builder setName(io.spine.test.tools.validate.Name.Builder builderForValue)
     * ```
     */
    private fun PsiClass.alterBuilderSetter() {
        val precondition = checkDefaultOrSame(
            currentValue = fieldGetter,
            newValue = "builderForValue.build()"
        )
        val setter = getMethodBySignature(
            "public Builder $fieldSetterName($fieldTypeClass.Builder builderForValue)"
        ).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * ```
     * public Builder mergeName(io.spine.test.tools.validate.Name value)
     * ```
     */
    private fun PsiClass.alterFieldMerge() {
        val precondition = checkDefaultOrSame(currentValue = fieldGetter, newValue = "value")
        val merge = method("merge$fieldNameCamel").body!!
        merge.addAfter(precondition, merge.lBrace)
    }

    override fun defaultOrSame(currentValue: String, newValue: String): String =
        "!$currentValue.equals($fieldTypeClass.getDefaultInstance()) && !$currentValue.equals($newValue)"
}
