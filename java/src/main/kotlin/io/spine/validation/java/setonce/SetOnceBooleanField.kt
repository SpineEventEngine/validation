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
import io.spine.protodata.ast.PrimitiveType.TYPE_BOOL
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.method
import io.spine.validation.java.MessageWithFile

/**
 * Renders Java code to support `(set_once)` option for the boolean number [field].
 *
 * @property field The boolean field that declared the option.
 * @property message The message that contains the [field].
 * @param sourceFile The source file that contains the [message].
 */
internal class SetOnceBooleanField(
    field: Field,
    message: MessageWithFile,
    sourceFile: SourceFile<Java>,
) : SetOnceJavaCode(field, message, sourceFile) {

    init {
        check(field.type.primitive == TYPE_BOOL) {
            "`${javaClass.simpleName}` handles only boolean fields. " +
                    "The passed field: `$field`. The declaring message: `${message.message}`."
        }
    }

    override fun PsiClass.doRender() {
        alterSetter()
        alterBytesMerge()
    }

    /**
     * ```
     * public Builder setHasMedals(boolean value)
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
            startsWith = "${fieldName}_ = input.readBool()"
        ) as PsiStatement
        val fieldProcessing = fieldReading.parent
        fieldProcessing.addBefore(rememberCurrent, fieldReading)
        fieldProcessing.addAfter(postcondition, fieldReading)
    }

    private fun checkDefaultOrSame(currentValue: String, newValue: String): PsiStatement =
        elementFactory.createStatement(
            """
            if ($currentValue != false && $currentValue != $newValue) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        )
}
