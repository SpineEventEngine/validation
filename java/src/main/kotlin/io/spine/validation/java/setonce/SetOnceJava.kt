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
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiStatement
import io.spine.protodata.ast.Field
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaCase
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.findClass
import io.spine.protodata.render.SourceFile
import io.spine.string.camelCase
import io.spine.tools.code.Java
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.execute
import io.spine.validation.java.MessageWithFile

/**
 * Renders Java code to support `(set_once)` option for the given [field].
 *
 * @property field The field that declared the option.
 * @property message The message that contains the [field].
 */
internal sealed class SetOnceJava(
    private val field: Field,
    private val message: MessageWithFile,
) {

    protected companion object {
        private const val DEFAULT_CONSTRAINT_VIOLATION =
            "io.spine.validate.ConstraintViolation.getDefaultInstance()"

        const val THROW_VALIDATION_EXCEPTION =
            "throw new io.spine.validate.ValidationException($DEFAULT_CONSTRAINT_VIOLATION);"

        /**
         * Defines the signature of the expected base `mergeFrom(...)` method,
         * upon which all bytes-related overloadings rely.
         *
         * Formally, its signature says about an input stream, but anyway, it is
         * a stream of bytes. So, the simpler name is kept. This method is called
         * indirectly via `mergeFrom(byte[] data)` overloading as well.
         *
         * Please note, it is a message-level method, the signature of which is independent
         * of fields and their outer messages. It is present in every generated message
         * with the same signature.
         */
        val MergeFromBytesSignature =
            """
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, 
                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException { }
            """.trimIndent()
    }

    protected val fieldName = field.name.javaCase()
    protected val fieldNameCamel = fieldName.camelCase()
    protected val fieldGetterName = "get$fieldNameCamel"
    protected val fieldSetterName = "set$fieldNameCamel"
    protected val fieldGetter = "$fieldGetterName()"

    protected abstract fun PsiClass.doRender()

    fun render(sourceFile: SourceFile<Java>) {
        val declaringMessage = message.message.javaClassName(message.fileHeader)
        val declaringMessageBuilder = ClassName(
            declaringMessage.packageName,
            declaringMessage.simpleNames + "Builder"
        )

        val psiFile = sourceFile.psi() as PsiJavaFile
        val psiClass = psiFile.findClass(declaringMessageBuilder)

        execute {
            // TODO:2024-10-25:yevhenii.nadtochii: Remove try-catch.
            try {
                psiClass.doRender()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        sourceFile.overwrite(psiFile.text)
    }

    protected fun PsiClass.alterBytesMerge(currentValue: String, getFieldReading: (PsiCodeBlock) -> PsiStatement) {
        val rememberCurrent = elementFactory.createStatement("var previous = $currentValue;")
        val postcondition = checkDefaultOrSame(
            currentValue = "previous",
            newValue = currentValue,
        )
        val mergeFromBytes = getMethodBySignature(MergeFromBytesSignature).body!!
        val fieldReading = getFieldReading(mergeFromBytes)
        val fieldProcessing = fieldReading.parent
        fieldProcessing.addBefore(rememberCurrent, fieldReading)
        fieldProcessing.addAfter(postcondition, fieldReading)
    }

    protected abstract fun checkDefaultOrSame(currentValue: String, newValue: String): PsiStatement

    /**
     * Looks for the first child of this [PsiElement], the text representation of which
     * satisfies both [startsWith] and [contains] conditions.
     *
     * This method performs a depth-first search of the PSI hierarchy. So, the second direct
     * child of this [PsiElement] is checked only when the first child and all its descendants
     * are checked.
     */
    // Kept in the class because it doesn't look like a general-purpose extension.
    protected fun PsiElement.deepSearch(
        startsWith: String,
        contains: String = startsWith
    ): PsiStatement = children.asSequence()
        .mapNotNull { element ->
            val text = element.text
            when {
                !text.contains(contains) -> null
                text.startsWith(startsWith) -> element
                else -> element.deepSearch(startsWith, contains)
            }
        }.first() as PsiStatement
}
