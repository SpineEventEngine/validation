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
import com.intellij.psi.PsiStatement
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.isEnum
import io.spine.string.lowerCamelCase
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.method
import io.spine.validation.java.MessageWithFile

/**
 * Renders Java code to support `(set_once)` option for the given enum [field].
 *
 * @property field The enum field that declared the option.
 * @property message The message that contains the [field].
 */
internal class SetOnceEnumField(
    field: Field,
    message: MessageWithFile
) : SetOnceJavaCode(field, message) {

    init {
        check(field.type.isEnum) {
            "`${javaClass.simpleName}` handles only enum fields. " +
                    "The passed field: `$field`. The declaring message: `${message.message}`."
        }
    }

    override fun PsiClass.doRender() {
        alterSetter()
        alterEnumValueSetter()
        alterBytesMerge()
    }

    /**
     * ```
     * public Builder setYearOfStudy(io.spine.test.tools.validate.YearOfStudy value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = checkDefaultOrSame(
            currentValue = "${fieldName}_",
            newValue = "value.getNumber()"
        )
        val setter = method(fieldSetterName).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * ```
     * public Builder setYearOfStudyValue(int value)
     * ```
     */
    private fun PsiClass.alterEnumValueSetter() {
        val precondition = checkDefaultOrSame(
            currentValue = "${fieldName}_",
            newValue = "value"
        )
        val setter = method("${fieldSetterName}Value").body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * ```
     * public Builder mergeFrom(
     *     com.google.protobuf.CodedInputStream input,
     *     com.google.protobuf.ExtensionRegistryLite extensionRegistry
     * ) throws java.io.IOException;
     * ```
     */
    private fun PsiClass.alterBytesMerge() {
        val rememberCurrent = elementFactory.createStatement("var previous = ${fieldName}_;")
        val defaultOrSameCheck = checkDefaultOrSame(
            currentValue = "previous",
            newValue = "${fieldName}_"
        )
        val mergeFromBytes = getMethodBySignature(MergeFromBytesSignature).body!!
        val fieldReading = mergeFromBytes.deepSearch(
            startsWith = "${fieldName.lowerCamelCase()}_ = input.readEnum()"
        ) as PsiStatement
        val fieldProcessing = fieldReading.parent
        fieldProcessing.addBefore(rememberCurrent, fieldReading)
        fieldProcessing.addAfter(defaultOrSameCheck, fieldReading)
    }

    private fun checkDefaultOrSame(currentValue: String, newValue: String): PsiStatement =
        elementFactory.createStatement(
            """
            if ($currentValue != 0 && $currentValue != $newValue) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        )
}
