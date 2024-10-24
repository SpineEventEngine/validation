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
import com.intellij.psi.PsiJavaFile
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaCase
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.findClass
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.execute
import io.spine.validation.java.MessageWithFile

/**
 * Data required to render Java code for `(set_once)` option.
 *
 * @property field The field that declared the option.
 * @property message The message that contains the [field].
 */
internal sealed class SetOnceJavaView(
    val field: Field,
    val message: MessageWithFile,
    private val sourceFile: SourceFile<Java>
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
         * of fields and their outer messages. It is present in every generated message,
         * and with the same signature.
         */
        val ExpectedMergeFromBytes = elementFactory.createMethodFromText(
            """
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, 
                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException { }
            """.trimIndent(), null
        )
    }

    private val fieldName: FieldName = field.name

    protected val fieldGetterName = "get${fieldName.javaCase()}"
    protected val fieldSetterName = "set${fieldName.javaCase()}"
    protected val fieldGetter = "$fieldGetterName()"
    protected val fieldSetter = "$fieldSetterName()"

    fun render() {
        val declaringMessage = message.message.javaClassName(message.fileHeader)
        val declaringMessageBuilder = ClassName(
            declaringMessage.packageName,
            declaringMessage.simpleNames + "Builder"
        )

        val psiFile = sourceFile.psi() as PsiJavaFile
        val psiClass = psiFile.findClass(declaringMessageBuilder)

        execute {
            try {
                psiClass.doRender()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        sourceFile.overwrite(psiFile.text)
    }

    protected abstract fun PsiClass.doRender()
}
