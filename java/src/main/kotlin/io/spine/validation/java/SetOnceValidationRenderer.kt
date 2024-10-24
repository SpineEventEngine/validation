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
import io.spine.protodata.render.SourceFileSet
import io.spine.string.camelCase
import io.spine.string.lowerCamelCase
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.method
import io.spine.validation.SetOnceField

internal class SetOnceValidationRenderer : JavaRenderer() {

    private companion object {
        const val DEFAULT_CONSTRAINT_VIOLATION =
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
        val fieldType = message.message.field(fieldName).type
        when {

            fieldType.isMessage -> {
                val fieldClassName = fieldType.message.javaClassName(message.fileHeader)
                alterMessageSetter(fieldName, fieldClassName)
                alterMessageBuilderSetter(fieldName, fieldClassName)
                alterMessageFieldMerge(fieldName, fieldClassName)
                alterMessageBytesMerge(fieldName, fieldClassName)
            }

            fieldType.isPrimitive -> when (fieldType.primitive) {

                TYPE_STRING -> {
                    val fieldClassName = message.message.javaClassName(message.fileHeader)
                    alterStringSetter(fieldName)
                    alterStringBytesSetter(fieldName)
                    alterStringMessageMerge(fieldName, fieldClassName)
                    alterStringBytesMerge(fieldName)
                }

                TYPE_DOUBLE -> {
                    alterNumberSetter(fieldName)
                    alterNumberBytesMerge(fieldName, "readDouble()")
                }

                TYPE_FLOAT -> {
                    alterNumberSetter(fieldName)
                    alterNumberBytesMerge(fieldName, "readFloat()")
                }

                TYPE_INT32 -> {
                    alterNumberSetter(fieldName)
                    alterNumberBytesMerge(fieldName, "readInt32()")
                }

                TYPE_INT64 -> {
                    alterNumberSetter(fieldName)
                    alterNumberBytesMerge(fieldName, "readInt64()")
                }

                TYPE_BOOL -> {
                    alterBooleanSetter(fieldName)
                    alterBooleanBytesMerge(fieldName)
                }

                TYPE_BYTES -> {
                    alterBytesSetter(fieldName)
                    alterBytesMerge(fieldName)
                }

                else -> error("Unsupported `(set_once)` field type: `$fieldType`")

            }

            fieldType.isEnum -> {
                alterEnumSetter(fieldName)
                alterEnumValueSetter(fieldName)
                alterEnumBytesMerge(fieldName)
            }

            else -> error("Unsupported `(set_once)` field type: `$fieldType`")
        }
    }

    /**
     * Modifies a setter that assigns a message value for the field.
     *
     * The method adds a precondition check that makes sure the current field value
     * is either the default value or identical to the new value being set.
     *
     * An example of the modified setter:
     *
     * ```
     * public Builder setName(my.proto.message.Name value);
     * ```
     */
    private fun PsiClass.alterMessageSetter(fieldName: String, fieldType: ClassName) {
        val currentFieldValue = fieldName.javaGetter()
        val precondition = elementFactory.createStatementFromText(
            """
            if (!($currentFieldValue.equals(${fieldType.canonical}.getDefaultInstance()) || $currentFieldValue.equals(value))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val expectedSetter = elementFactory.createMethodFromText(
            """
            public Builder ${fieldName.javaSetterName()}(${fieldType.canonical} value) {}
            """.trimIndent(), null
        )
        val setter = findMethodBySignature(expectedSetter, false)!!.body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * An example of the modified setter:
     *
     * ```
     * public Builder setName(my.proto.Name.Builder builderForValue);
     * ```
     */
    private fun PsiClass.alterMessageBuilderSetter(fieldName: String, fieldType: ClassName) {
        val currentFieldValue = fieldName.javaGetter()
        val newValue = "builderForValue.build()"
        val precondition = elementFactory.createStatementFromText(
            """
            if (!($currentFieldValue.equals(${fieldType.canonical}.getDefaultInstance()) || $currentFieldValue.equals($newValue))) {
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
     * An example of the modified setter:
     *
     * ```
     * public Builder mergeName(my.proto.Name value);
     * ```
     */
    private fun PsiClass.alterMessageFieldMerge(fieldName: String, fieldType: ClassName) {
        val currentFieldValue = fieldName.javaGetter()
        val precondition = elementFactory.createStatementFromText(
            """
            if (!($currentFieldValue.equals(${fieldType.canonical}.getDefaultInstance()) || $currentFieldValue.equals(value))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val merge = method("merge${fieldName.camelCase()}").body!!
        merge.addAfter(precondition, merge.lBrace)
    }

    /**
     * Modifies the message-level [ExpectedMergeFromBytes] method to add a precondition
     * to a statement that handles a particular field denoted by [fieldName].
     */
    private fun PsiClass.alterMessageBytesMerge(fieldName: String, fieldType: ClassName) {
        val currentFieldValue = fieldName.javaGetter()
        val keepPrevious =
            elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val postcondition = elementFactory.createStatementFromText(
            """
            if (!(previous.equals(${fieldType.canonical}.getDefaultInstance()) || previous.equals($currentFieldValue))) {
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

    private fun PsiClass.alterStringSetter(fieldName: String) {
        val fieldValue = fieldName.javaGetter()
        val preconditionCheck =
            """
            if (!($fieldValue.isEmpty() || $fieldValue.equals(value))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetterName()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alterStringBytesSetter(fieldName: String) {
        val fieldValue = "get${fieldName.camelCase()}Bytes()"
        val preconditionCheck =
            """
            if (!($fieldValue.isEmpty() || $fieldValue.equals(value))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val bytesSetter = method("${fieldName.javaSetterName()}Bytes").body!!
        bytesSetter.addAfter(statement, bytesSetter.lBrace)
    }

    private fun PsiClass.alterStringMessageMerge(fieldName: String, fieldType: ClassName) {
        val fieldValue = fieldName.javaGetter()
        val preconditionCheck =
            """
            if (!($fieldValue.isEmpty() || $fieldValue.equals(other.$fieldValue))) {
                $THROW_VALIDATION_EXCEPTION
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

    private fun PsiClass.alterStringBytesMerge(fieldName: String) {
        val currentFieldValue = fieldName.javaGetter()
        val keepPrevious =
            elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val defaultOrSameCheck = elementFactory.createStatementFromText(
            """
            if (!(previous.isEmpty() || previous.equals($currentFieldValue))) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(ExpectedMergeFromBytes, false)!!.body!!
        val fieldReading = mergeFromBytes.deepSearch(
            startsWith = "${fieldName.lowerCamelCase()}_ = input.readStringRequireUtf8()"
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addAfter(defaultOrSameCheck, fieldReading)
    }

    private fun PsiClass.alterNumberSetter(fieldName: String) {
        val currentFieldValue = fieldName.javaGetter()
        val precondition = elementFactory.createStatementFromText(
            """
            if ($currentFieldValue != 0 && $currentFieldValue != value) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val setter = method(fieldName.javaSetterName()).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    private fun PsiClass.alterNumberBytesMerge(fieldName: String, fieldReader: String) {
        val currentFieldValue = fieldName.javaGetter()
        val keepPrevious =
            elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val defaultOrSameCheck = elementFactory.createStatementFromText(
            """
            if (previous != 0 && previous != $currentFieldValue) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(ExpectedMergeFromBytes, false)!!.body!!
        val fieldReading = mergeFromBytes.deepSearch(
            startsWith = "${fieldName.lowerCamelCase()}_ = input.$fieldReader"
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addAfter(defaultOrSameCheck, fieldReading)
    }

    private fun PsiClass.alterBooleanSetter(fieldName: String) {
        val currentFieldValue = fieldName.javaGetter()
        val preconditionCheck =
            """
            if ($currentFieldValue != false && $currentFieldValue != value) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetterName()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alterBooleanBytesMerge(fieldName: String) {
        val currentFieldValue = fieldName.javaGetter()
        val keepPrevious =
            elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val defaultOrSameCheck = elementFactory.createStatementFromText(
            """
            if (previous != false && previous != $currentFieldValue) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(ExpectedMergeFromBytes, false)!!.body!!
        val fieldReading = mergeFromBytes.deepSearch(
            startsWith = "${fieldName.lowerCamelCase()}_ = input.readBool()"
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addBefore(defaultOrSameCheck, fieldReading)
    }

    private fun PsiClass.alterBytesSetter(fieldName: String) {
        val currentFieldValue = fieldName.javaGetter()
        val preconditionCheck =
            """
            if ($currentFieldValue != com.google.protobuf.ByteString.EMPTY && !$currentFieldValue.equals(value)) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetterName()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alterBytesMerge(fieldName: String) {
        val currentFieldValue = fieldName.javaGetter()
        val keepPrevious =
            elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val defaultOrSameCheck = elementFactory.createStatementFromText(
            """
            if (previous != com.google.protobuf.ByteString.EMPTY && !previous.equals($currentFieldValue)) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(ExpectedMergeFromBytes, false)!!.body!!
        val fieldReading = mergeFromBytes.deepSearch(
            startsWith = "${fieldName.lowerCamelCase()}_ = input.readBytes()"
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addAfter(defaultOrSameCheck, fieldReading)
    }

    private fun PsiClass.alterEnumSetter(fieldName: String) {
        val preconditionCheck =
            """
            if (${fieldName.lowerCamelCase()}_ != 0 && ${fieldName.javaGetter()} != value) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method(fieldName.javaSetterName()).body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alterEnumValueSetter(fieldName: String) {
        val fieldValue = fieldName.lowerCamelCase()
        val preconditionCheck =
            """
            if (${fieldValue}_ != 0 && ${fieldValue}_ != value) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        val statement = elementFactory.createStatementFromText(preconditionCheck, null)
        val setter = method("${fieldName.javaSetterName()}Value").body!!
        setter.addAfter(statement, setter.lBrace)
    }

    private fun PsiClass.alterEnumBytesMerge(fieldName: String) {
        val currentFieldValue = "${fieldName.lowerCamelCase()}_"
        val keepPrevious = elementFactory.createStatementFromText("var previous = $currentFieldValue;", null)
        val defaultOrSameCheck = elementFactory.createStatementFromText(
            """
            if (previous != 0 && previous != $currentFieldValue) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent(), null
        )
        val mergeFromBytes = findMethodBySignature(ExpectedMergeFromBytes, false)!!.body!!
        val fieldReading = mergeFromBytes.deepSearch(
            startsWith = "${fieldName.lowerCamelCase()}_ = input.readEnum()"
        ) as PsiStatement
        fieldReading.parent.addBefore(keepPrevious, fieldReading)
        fieldReading.parent.addAfter(defaultOrSameCheck, fieldReading)
    }
}

/**
 * Looks for the first child of this [PsiElement], the text representation of which satisfies
 * both [startsWith] and [contains] conditions.
 *
 * This method performs a depth-first search of the PSI hierarchy. So, the second direct child
 * of this [PsiElement] is checked only when the first child and all its descendants are checked.
 */
private fun PsiElement.deepSearch(
    startsWith: String,
    contains: String = startsWith
): PsiElement? = children.asSequence()
    .mapNotNull { element ->
        val text = element.text
        when {
            !text.contains(contains) -> null
            text.startsWith(startsWith) -> element
            else -> element.deepSearch(startsWith, contains)
        }
    }.firstOrNull()

private fun String.javaGetter() = "get${camelCase()}()"

private fun String.javaSetterName() = "set${camelCase()}"
