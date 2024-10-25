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

package io.spine.validation.java

import com.intellij.psi.PsiJavaFile
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.TYPE_BOOL
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.ast.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_INT64
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.field
import io.spine.protodata.ast.isEnum
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.isPrimitive
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.java.render.findClass
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.tools.code.Java
import io.spine.tools.psi.java.execute
import io.spine.validation.SetOnceField
import io.spine.validation.java.setonce.SetOnceBooleanField
import io.spine.validation.java.setonce.SetOnceBytesField
import io.spine.validation.java.setonce.SetOnceEnumField
import io.spine.validation.java.setonce.SetOnceMessageField
import io.spine.validation.java.setonce.SetOnceNumberField
import io.spine.validation.java.setonce.SetOnceStringField

internal class SetOnceValidationRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        // TODO:2024-10-21:yevhenii.nadtochii: Modify the view to keep the `MessageType`.
        val messages = findMessageTypes().associateBy { it.message.name }
        val fields = select<SetOnceField>().all()
        val fieldsToMessages = fields.associateWith {
            messages[it.id.type] ?: error("Messages `${it.id.name}` not found.")
        }

        fieldsToMessages.forEach {
            val message = it.value.message
            val file = sources.javaFileOf(message)
            val declaringMessage = message.javaClassName(it.value.fileHeader!!)
            val declaringMessageBuilder = ClassName(
                declaringMessage.packageName,
                declaringMessage.simpleNames + "Builder"
            )
            val psiFile = file.psi() as PsiJavaFile
            val psiClass = psiFile.findClass(declaringMessageBuilder)
            execute {
                try {
                    render(file, it.key, it.value)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            file.overwrite(psiFile.text)
        }
    }

//    /**
//     * Quires language-agnostic [SetOnceField] view states, and maps them to [SetOnceField],
//     * appending the information required to generate Java code for the option support.
//     */
//    private fun javaViews(): List<SetOnceJavaCode> {
//        val allMessages = findMessageTypes().associateBy { it.message.name }
//        val setOnceViews = select<SetOnceField>().all()
//        return setOnceViews.map { view ->
//            val field: Field = view.subject
//            val declaringMessage = allMessages[field.declaringType]
//                ?: error(
//                    "Metadata for `${field.declaringType}` message, which declares `(set_once)` " +
//                            "option for its `${field.name}` field was not found."
//                )
//            setOnceJavaView(view.subject, declaringMessage)
//        }
//    }
//
//    private fun setOnceJavaView(field: Field, message: MessageWithFile): SetOnceJavaCode {
//        val fieldType = field.type
//        return when {
//            fieldType.isMessage -> SetOnceMessageField(field, message)
//            else -> error("Unsupported `(set_once)` field type: `$fieldType`")
//        }
//    }

    private fun render(file: SourceFile<Java>, setOnce: SetOnceField, message: MessageWithFile) {
        val fieldName = setOnce.id.name.value
        val fieldType = message.message.field(fieldName).type
        when {

            fieldType.isPrimitive -> renderPrimitive(
                file,
                setOnce,
                fieldType.primitive,
                message
            )

            fieldType.isMessage -> {
                SetOnceMessageField(setOnce.subject, message, file)
                    .render()
            }

            fieldType.isEnum -> {
                SetOnceEnumField(setOnce.subject, message, file)
                    .render()
            }

            else -> error("Unsupported `(set_once)` field type: `$fieldType`")
        }
    }

    private fun renderPrimitive(
        file: SourceFile<Java>,
        field: SetOnceField,
        fieldType: PrimitiveType,
        message: MessageWithFile
    ) {
        when (fieldType) {

            TYPE_STRING -> {
                SetOnceStringField(field.subject, message, file)
                    .render()
            }

            TYPE_DOUBLE, TYPE_FLOAT,
            TYPE_INT32, TYPE_INT64 -> {
                SetOnceNumberField(field.subject, message, file)
                    .render()
            }

            TYPE_BOOL -> {
                SetOnceBooleanField(field.subject, message, file)
                    .render()
            }

            TYPE_BYTES -> {
                SetOnceBytesField(field.subject, message, file)
                    .render()
            }

            else -> error("Unsupported `(set_once)` field type: `$fieldType`")

        }
    }
}
