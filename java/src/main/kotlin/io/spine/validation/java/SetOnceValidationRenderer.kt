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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import io.spine.protodata.ast.field
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.isPrimitive
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.java.render.findClass
import io.spine.protodata.render.SourceFileSet
import io.spine.string.camelCase
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.method
import io.spine.validation.SetOnceField

internal class SetOnceValidationRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output roots here. Now we do only `java`.
        if (!sources.hasJavaRoot) {
            return
        }

        // TODO:2024-10-21:yevhenii.nadtochii: Modify the view to preserve the `MessageType`.
        val messages = findMessageTypes().associateBy { it.message.name }
        val fields = select<SetOnceField>().all()
        val fieldsToMessages = fields.associateWith {
            messages[it.id.type] ?: error("Messages `${it.id.name}` not found.")
        }


        fieldsToMessages.forEach {
            val message = it.value.message
            val file = sources.javaFileOf(message)
            val className = message.javaClassName(it.value.fileHeader!!)
            val builderClass = ClassName(className.packageName, className.simpleNames + "Builder")
            val psiFile = file.psi() as PsiJavaFile
            val psiClass = psiFile.findClass(builderClass)
            execute {
                psiClass.render(it.key, it.value)
            }
            file.overwrite(psiFile.text)
        }
    }

    private fun PsiClass.render(setOnce: SetOnceField, message: MessageWithFile) {
        val fieldName = setOnce.id.name.value
        val field = message.message.field(fieldName)
        val fieldType = field.type
        when {

            fieldType.isMessage -> {
                alertMessageSetter(fieldName, fieldType.message.javaClassName(message.fileHeader))
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_STRING" -> {
                alertStringSetter(fieldName)
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_DOUBLE" -> {
                alertFloatingPoint(fieldName)
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_FLOAT" -> {
                alertFloatingPoint(fieldName)
            }

            else -> error("Unsupported `(set_once)` field type: `$fieldType`")
        }
    }

    private fun PsiClass.alertMessageSetter(fieldName: String, fieldType: ClassName) {
        val preconditionCheck =
            """
            if (!${fieldName.javaGetter()}.equals(${fieldType.canonical}.getDefaultInstance())) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val messageSetterSig = elementFactory.createMethodFromText(
            """
            public Builder ${fieldName.javaSetter()}(${fieldType.canonical} value) {}
            """.trimIndent(), null
        )
        val messageSetter = findMethodBySignature(messageSetterSig, false)!!.body!!
        val builderSetterSig = elementFactory.createMethodFromText(
            """
            public Builder ${fieldName.javaSetter()}(${fieldType.canonical}.Builder builderForValue) {}
            """.trimIndent(), null
        )
        val builderSetter = findMethodBySignature(builderSetterSig, false)!!.body!!
        messageSetter.addAfter(statement, messageSetter.lBrace)
        builderSetter.addAfter(statement, builderSetter.lBrace)
    }

    private fun PsiClass.alertFloatingPoint(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.javaGetter()} != 0) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetter()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alertStringSetter(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.javaGetter()} != "") {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetter()).body!!
        val bytesSetter = method("${fieldName.javaSetter()}Bytes").body!!
        setter.addAfter(statement, setter.lBrace)
        bytesSetter.addAfter(statement, bytesSetter.lBrace)
    }
}

// TODO:2024-10-21:yevhenii.nadtochii: Already exists in `mc-java`.
//  Move to ProtoData?
private fun String.javaGetter() = "get${camelCase()}()"

private fun String.javaSetter() = "set${camelCase()}"
