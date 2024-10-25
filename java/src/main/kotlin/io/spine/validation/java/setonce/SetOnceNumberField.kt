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
import io.spine.protodata.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.ast.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_INT64
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.method
import io.spine.validation.java.MessageWithFile

/**
 * Renders Java code to support `(set_once)` option for the given number [field].
 *
 * @property field The number field that declared the option.
 * @property message The message that contains the [field].
 */
internal class SetOnceNumberField(
    field: Field,
    message: MessageWithFile
) : SetOnceJavaCode(field, message) {

    private companion object {
        val SupportedNumberTypes = mapOf(
            TYPE_DOUBLE to "readDouble()",
            TYPE_FLOAT to "readFloat()",
            TYPE_INT32 to "readInt32()",
            TYPE_INT64 to "readInt64()",
        )
    }

    private val fieldReader: String

    init {
        val fieldReader = SupportedNumberTypes[field.type.primitive]
        check(fieldReader != null) {
            "`${javaClass.simpleName}` handles only number fields. " +
                    "The passed field: `$field`. The declaring message: `${message.message}`."
        }
        this.fieldReader = fieldReader
    }

    override fun PsiClass.doRender() {
        alterSetter()
        alterBytesMerge()
    }

    /**
     * ```
     * public Builder setAge(int value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = checkDefaultOrSame(currentValue = fieldGetter, newValue = "value")
        val setter = method(fieldSetterName).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * ```
     * public Builder mergeFrom(
     *     com.google.protobuf.CodedInputStream input,
     *     com.google.protobuf.ExtensionRegistryLite extensionRegistry
     * ) throws java.io.IOException
     * ```
     */
    private fun PsiClass.alterBytesMerge() {
        val rememberCurrent = elementFactory.createStatement("var previous = $fieldGetter;")
        val postcondition = checkDefaultOrSame(
            currentValue = "previous",
            newValue = fieldGetter
        )
        val mergeFromBytes = getMethodBySignature(MergeFromBytesSignature).body!!
        val fieldReading = mergeFromBytes.deepSearch(
            startsWith = "${fieldName}_ = input.$fieldReader"
        ) as PsiStatement
        val fieldProcessing = fieldReading.parent
        fieldProcessing.addBefore(rememberCurrent, fieldReading)
        fieldProcessing.addAfter(postcondition, fieldReading)
    }

    private fun checkDefaultOrSame(currentValue: String, newValue: String): PsiStatement =
        elementFactory.createStatement(
            """
            if ($currentValue != 0 && $currentValue != $newValue) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        )
}
