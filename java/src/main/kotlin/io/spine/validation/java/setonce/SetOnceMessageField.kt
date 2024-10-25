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
import io.spine.protodata.java.javaClassName
import io.spine.protodata.render.SourceFile
import io.spine.string.camelCase
import io.spine.tools.code.Java
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.method
import io.spine.validation.java.MessageWithFile

internal class SetOnceMessageField(
    field: Field,
    message: MessageWithFile,
    sourceFile: SourceFile<Java>,
) : SetOnceJavaCode(field, message, sourceFile) {

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
        alterBytesMerge()
    }

    /**
     * ```
     * public Builder setName(my.proto.message.Name value);
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = checkDefaultOrSame(currentValue = fieldGetter, newValue = "value")
        val setterSignature = elementFactory.createMethodFromText(
            """
            public Builder $fieldSetterName($fieldTypeClass value) {}
            """.trimIndent(), null
        )
        val setter = findMethodBySignature(setterSignature, false)!!.body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * ```
     * public Builder setName(my.proto.Name.Builder builderForValue);
     * ```
     */
    private fun PsiClass.alterBuilderSetter() {
        val newValue = "builderForValue.build()"
        val precondition = elementFactory.createStatementFromText(
            """
            if (!($currentValue.equals($fieldTypeClass.getDefaultInstance()) || $currentValue.equals($newValue))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val expectedSetter = elementFactory.createMethodFromText(
            """
            public Builder ${fieldName.javaSetterName()}(${fieldType.canonical}.Builder builderForValue) {}
            """.trimIndent(), null
        )
        val setter = findMethodBySignature(expectedSetter, false)!!.body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * ```
     * public Builder mergeName(my.proto.Name value);
     * ```
     */
    private fun PsiClass.alterFieldMerge() {
        val precondition = elementFactory.createStatementFromText(
            """
            if (!($currentValue.equals($fieldTypeClass.getDefaultInstance()) || $currentValue.equals(value))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val merge = method("merge${fieldName.camelCase()}").body!!
        merge.addAfter(precondition, merge.lBrace)
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
        val keepPrevious =
            elementFactory.createStatementFromText("var previous = $currentValue;", null)
        val postcondition = elementFactory.createStatementFromText(
            """
            if (!(previous.equals($fieldTypeClass.getDefaultInstance()) || previous.equals($currentValue))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(ExpectedMergeFromBytes, false)!!.body!!
        val fieldReading = mergeFromBytes.deepSearch(
            startsWith = "input.readMessage",
            contains = "get${fieldName.camelCase()}FieldBuilder().getBuilder()",
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addAfter(postcondition, fieldReading)
    }

    private fun checkDefaultOrSame(currentValue: String, newValue: String): PsiStatement =
        elementFactory.createStatementFromText(
            """
            if (!($currentValue.equals($fieldTypeClass.getDefaultInstance()) || $currentValue.equals($newValue))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
}
