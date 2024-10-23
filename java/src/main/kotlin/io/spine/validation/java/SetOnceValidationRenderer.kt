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

import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiStatement
import io.spine.protodata.ast.field
import io.spine.protodata.ast.isEnum
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.isPrimitive
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.java.render.findClass
import io.spine.protodata.render.SourceFileSet
import io.spine.string.camelCase
import io.spine.string.lowerCamelCase
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
                try {
                    psiClass.render(it.key, it.value)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
                val fieldClassName = fieldType.message.javaClassName(message.fileHeader)
                alertMessageSetter(fieldName, fieldClassName)
                alertMessageBuilderSetter(fieldName, fieldClassName)
                alertMessageFieldMerge(fieldName, fieldClassName)
                alertMessageBytesMerge(fieldName, fieldClassName)
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_STRING" -> {
                val fieldClassName = message.message.javaClassName(message.fileHeader)
                alertStringSetter(fieldName)
                alertStringBytesSetter(fieldName)
                alertStringMessageMerge(fieldName, fieldClassName)
                alertStringBytesMerge(fieldName)
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_DOUBLE" -> {
                alertNumberSetter(fieldName)
                alertNumberBytesMerge(fieldName, "readDouble()")
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_FLOAT" -> {
                alertNumberSetter(fieldName)
                alertNumberBytesMerge(fieldName, "readFloat()")
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_INT32" -> {
                alertNumberSetter(fieldName)
                alertNumberBytesMerge(fieldName, "readInt32()")
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_INT64" -> {
                alertNumberSetter(fieldName)
                alertNumberBytesMerge(fieldName, "readInt64()")
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_BOOL" -> {
                alertBooleanSetter(fieldName)
                alertBooleanBytesMerge(fieldName)
            }

            fieldType.isPrimitive && fieldType.primitive.name == "TYPE_BYTES" -> {
                alertBytesSetter(fieldName)
                alertBytesMerge(fieldName)
            }

            fieldType.isEnum -> {
                alertEnumSetter(fieldName)
                alertEnumValueSetter(fieldName)
                alertEnumBytesMerge(fieldName)
            }

            else -> error("Unsupported `(set_once)` field type: `$fieldType`")
        }
    }

    private fun PsiClass.alertMessageSetter(fieldName: String, fieldType: ClassName) {
        val preconditionCheck =
            """
            if (!(${fieldName.javaGetter()}.equals(${fieldType.canonical}.getDefaultInstance()) || ${fieldName.javaGetter()}.equals(value))) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val messageSetterSig = elementFactory.createMethodFromText(
            """
            public Builder ${fieldName.javaSetter()}(${fieldType.canonical} value) {}
            """.trimIndent(), null
        )
        val messageSetter = findMethodBySignature(messageSetterSig, false)!!.body!!
        messageSetter.addAfter(statement, messageSetter.lBrace)
    }

    private fun PsiClass.alertMessageBuilderSetter(fieldName: String, fieldType: ClassName) {
        val preconditionCheck =
            """
            if (!(${fieldName.javaGetter()}.equals(${fieldType.canonical}.getDefaultInstance()) || ${fieldName.javaGetter()}.equals(builderForValue.build()))) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val builderSetterSig = elementFactory.createMethodFromText(
            """
            public Builder ${fieldName.javaSetter()}(${fieldType.canonical}.Builder builderForValue) {}
            """.trimIndent(), null
        )
        val builderSetter = findMethodBySignature(builderSetterSig, false)!!.body!!
        builderSetter.addAfter(statement, builderSetter.lBrace)
    }

    private fun PsiClass.alertMessageFieldMerge(fieldName: String, fieldType: ClassName) {
        val preconditionCheck =
            """
            if (!(${fieldName.javaGetter()}.equals(${fieldType.canonical}.getDefaultInstance()) || ${fieldName.javaGetter()}.equals(value))) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val fieldMerge = method("merge${fieldName.camelCase()}").body!!
        fieldMerge.addAfter(statement, fieldMerge.lBrace)
    }

    private fun PsiClass.alertMessageBytesMerge(fieldName: String, fieldType: ClassName) {
        val currentFieldValue = fieldName.javaGetter()
        val keepPrevious = elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val defaultOrSameCheck = elementFactory.createStatementFromText(
            """
            if (!(previous.equals(${fieldType.canonical}.getDefaultInstance()) || previous.equals($currentFieldValue))) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent(), null
        )
        val mergeFromBytesSig = elementFactory.createMethodFromText(
            """
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, 
                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException { }
            """.trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(mergeFromBytesSig, false)!!.body!!
        val fieldReading = mergeFromBytes.children.deepSearch(
            whole = { it.contains("get${fieldName.camelCase()}FieldBuilder().getBuilder()") },
            strict = { it.startsWith("input.readMessage") }
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addAfter(defaultOrSameCheck, fieldReading)
    }

    private fun PsiClass.alertStringSetter(fieldName: String) {
        val fieldValue = fieldName.javaGetter()
        val preconditionCheck =
            """
            if (!($fieldValue.isEmpty() || $fieldValue.equals(value))) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetter()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alertStringBytesSetter(fieldName: String) {
        val fieldValue = "get${fieldName.camelCase()}Bytes()"
        val preconditionCheck =
            """
            if (!($fieldValue.isEmpty() || $fieldValue.equals(value))) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val bytesSetter = method("${fieldName.javaSetter()}Bytes").body!!
        bytesSetter.addAfter(statement, bytesSetter.lBrace)
    }

    private fun PsiClass.alertStringMessageMerge(fieldName: String, fieldType: ClassName) {
        val fieldValue = fieldName.javaGetter()
        val preconditionCheck =
            """
            if (!($fieldValue.isEmpty() || $fieldValue.equals(other.$fieldValue))) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val mergeFromMessageSig = elementFactory.createMethodFromText(
            """
            public Builder mergeFrom(${fieldType.canonical} other) {}
            """.trimIndent(), null
        )
        val mergeFromMessage = findMethodBySignature(mergeFromMessageSig, false)!!.body!!
        val checkFieldNotDefault = mergeFromMessage.statements.find {
            it.text.startsWith("if (!other.${fieldName.javaGetter()}.isEmpty())")
        } as PsiIfStatement
        val thenBranch = (checkFieldNotDefault.thenBranch!! as PsiBlockStatement).codeBlock
        thenBranch.addAfter(statement, thenBranch.lBrace)
    }

    private fun PsiClass.alertStringBytesMerge(fieldName: String) {
        val currentFieldValue = fieldName.javaGetter()
        val keepPrevious = elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val defaultOrSameCheck = elementFactory.createStatementFromText(
            """
            if (!(previous.isEmpty() || previous.equals($currentFieldValue))) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent(), null
        )
        val mergeFromBytesSig = elementFactory.createMethodFromText(
            """
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, 
                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException { }
            """.trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(mergeFromBytesSig, false)!!.body!!
        val fieldReading = mergeFromBytes.children.deepSearch(
            whole = { it.contains("${fieldName.lowerCamelCase()}_ = input.readStringRequireUtf8()") },
            strict = { it.startsWith("${fieldName.lowerCamelCase()}_ = input.readStringRequireUtf8()") }
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addAfter(defaultOrSameCheck, fieldReading)
    }

    private fun PsiClass.alertNumberSetter(fieldName: String) {
        val currentFieldValue = fieldName.javaGetter()
        val preconditionCheck =
            """
            if ($currentFieldValue != 0 && $currentFieldValue != value) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetter()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alertNumberBytesMerge(fieldName: String, fieldReader: String) {
        val currentFieldValue = fieldName.javaGetter()
        val keepPrevious = elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val defaultOrSameCheck = elementFactory.createStatementFromText(
            """
            if (previous != 0 && previous != $currentFieldValue) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent(), null
        )
        val mergeFromBytesSig = elementFactory.createMethodFromText(
            """
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, 
                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException { }
            """.trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(mergeFromBytesSig, false)!!.body!!
        val fieldReading = mergeFromBytes.children.deepSearch(
            whole = { it.contains("${fieldName.lowerCamelCase()}_ = input.$fieldReader") },
            strict = { it.startsWith("${fieldName.lowerCamelCase()}_ = input.$fieldReader") }
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addAfter(defaultOrSameCheck, fieldReading)
    }

    private fun PsiClass.alertBooleanSetter(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.javaGetter()} != false) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetter()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alertBooleanBytesMerge(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.javaGetter()} != false) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val mergeFromBytesSig = elementFactory.createMethodFromText(
            """
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, 
                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException { }
            """.trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(mergeFromBytesSig, false)!!.body!!
        val fieldReading = mergeFromBytes.children.deepSearch(
            whole = { it.contains("${fieldName.lowerCamelCase()}_ = input.readBool()") },
            strict = { it.startsWith("${fieldName.lowerCamelCase()}_ = input.readBool()") }
        ) as PsiStatement
        fieldReading.parent.addBefore(statement, fieldReading)
    }

    private fun PsiClass.alertBytesSetter(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.javaGetter()} != com.google.protobuf.ByteString.EMPTY) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetter()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alertBytesMerge(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.javaGetter()} != com.google.protobuf.ByteString.EMPTY) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val mergeFromBytesSig = elementFactory.createMethodFromText(
            """
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, 
                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException { }
            """.trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(mergeFromBytesSig, false)!!.body!!
        val fieldReading = mergeFromBytes.children.deepSearch(
            whole = { it.contains("${fieldName.lowerCamelCase()}_ = input.readBytes()") },
            strict = { it.startsWith("${fieldName.lowerCamelCase()}_ = input.readBytes()") }
        ) as PsiStatement
        fieldReading.parent.addBefore(statement, fieldReading)
    }

    private fun PsiClass.alertEnumSetter(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.lowerCamelCase()}_ != 0) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetter()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alertEnumBytesMerge(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.lowerCamelCase()}_ != 0) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val mergeFromBytesSig = elementFactory.createMethodFromText(
            """
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, 
                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException { }
            """.trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(mergeFromBytesSig, false)!!.body!!
        val fieldReading = mergeFromBytes.children.deepSearch(
            whole = { it.contains("${fieldName.lowerCamelCase()}_ = input.readEnum()") },
            strict = { it.startsWith("${fieldName.lowerCamelCase()}_ = input.readEnum()") }
        ) as PsiStatement
        fieldReading.parent.addBefore(statement, fieldReading)
    }

    private fun PsiClass.alertEnumValueSetter(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.lowerCamelCase()}_ != 0) {
                throw new io.spine.validate.ValidationException(io.spine.validate.ConstraintViolation.getDefaultInstance());
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method("${fieldName.javaSetter()}Value").body!!
        setter.addAfter(statement, setter.lBrace)
    }
}

private fun Array<PsiElement>.deepSearch(
    strict: (String) -> Boolean,
    whole: (String) -> Boolean
): PsiElement? =
    asSequence().mapNotNull { element ->
        val text = element.text
        when {
            !whole(text) -> null
            strict(text) -> element
            else -> element.children.deepSearch(strict, whole)
        }
    }.firstOrNull()

// TODO:2024-10-21:yevhenii.nadtochii: Already exists in `mc-java`.
//  Move to ProtoData?
private fun String.javaGetter() = "get${camelCase()}()"

private fun String.javaSetter() = "set${camelCase()}"
