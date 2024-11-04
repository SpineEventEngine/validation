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
 * The rendered Java constraints are specific to the field type. This class
 * serves as an abstract base, providing common methods and the skeleton implementation
 * of [render] method. Inheritors should perform actual rendering in [renderConstraints].
 *
 * @property field The field that declared the option.
 * @property messageWithHeader The message that contains the [field].
 */
internal sealed class SetOnceJavaConstraints(
    private val field: Field,
    private val messageWithHeader: MessageWithFile,
) {

    private companion object {
        const val THROW_VALIDATION_EXCEPTION =
            "throw new io.spine.validate.ValidationException(" +
                    "io.spine.validate.ConstraintViolation.getDefaultInstance());"

        /**
         * The signature of `mergeFrom(CodedInputStream)` method.
         *
         * The signature of this method is independent of the processed field and its type.
         * It is present in every generated message with the same signature.
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
    protected val declaringMessage = messageWithHeader.message
        .javaClassName(messageWithHeader.fileHeader)

    /**
     * Renders Java constraints in the given [sourceFile] to make sure that the [field]
     * can be assigned only once.
     *
     * The [field] can be assigned a new value only if the current value is default
     * for the field type OR if the assigned value is the same with the current one.
     *
     * @param sourceFile Protobuf-generated Java source code of the [message][messageWithHeader]
     *  that declared the [field].
     *
     * @see defaultOrSamePredicate
     * @see defaultOrSameStatement
     */
    fun render(sourceFile: SourceFile<Java>) {
        val messageBuilder = ClassName(
            packageName = declaringMessage.packageName,
            simpleNames = declaringMessage.simpleNames + "Builder"
        )
        val psiFile = sourceFile.psi() as PsiJavaFile
        val psiClass = psiFile.findClass(messageBuilder)

        execute {
            psiClass.renderConstraints()
        }

        sourceFile.overwrite(psiFile.text)
    }

    /**
     * Renders Java constraints in this [PsiClass] to make sure the [field] can be assigned
     * only once.
     *
     * This [PsiClass] represents a Java builder for [messageWithHeader], which declared
     * the [field].
     */
    protected abstract fun PsiClass.renderConstraints()

    /**
     * Alters [MergeFromBytesSignature] method to make sure that a set-once field
     * is not overridden during the merge from a byte array.
     *
     * Such a merge is done field-by-field. This method finds the place, where
     * the given [field] is processed. It adds a statement to remember the current field value,
     * before reading of a new one. After the reading statement, it adds [defaultOrSameStatement]
     * to check that the just read value is legal to be assigned.
     *
     * Implementation of this method is common for all field types. Inheritors should
     * invoke it within [renderConstraints], passing the necessary parameters.
     *
     * The [currentValue] and [readerStartsWith] are mandatory properties. Pass [readerContains]
     * in cases when [readerStartsWith] is not sufficient. For example, for message fields,
     * the beginning of the reading block is the same for all fields because it doesn't include
     * the field name. Differentiation is done way deeper.
     *
     * @param currentValue an expression to read the current field value.
     * @param readerStartsWith an expression representing the beginning of the field reading block.
     * @param readerContains an arbitrary expression within the field reading block.
     */
    protected fun PsiClass.alterBytesMerge(
        currentValue: String,
        readerStartsWith: String,
        readerContains: String = readerStartsWith,
    ) {

        val mergeFromBytes = getMethodBySignature(MergeFromBytesSignature).body!!
        val fieldReading = mergeFromBytes.deepSearch(readerStartsWith, readerContains)
        val fieldProcessing = fieldReading.parent

        val rememberCurrent = elementFactory.createStatement("var previous = $currentValue;")
        fieldProcessing.addBefore(rememberCurrent, fieldReading)

        val postcondition = defaultOrSameStatement(
            currentValue = "previous",
            newValue = currentValue,
        )
        fieldProcessing.addAfter(postcondition, fieldReading)
    }

    /**
     * Creates an `if` statement, which checks that the current field value is default
     * OR if the proposed value is the same as the current.
     *
     * Otherwise, it throws the validation exception.
     *
     * @param currentValue an expression denoting the current field value.
     * @param newValue an expression denoting the proposed value.
     */
    protected fun defaultOrSameStatement(currentValue: String, newValue: String): PsiStatement =
        elementFactory.createStatement(
            """
            if (${defaultOrSamePredicate(currentValue, newValue)}) {
                $THROW_VALIDATION_EXCEPTION
            }""".trimIndent()
        )

    /**
     * Returns an expression representing a predicate upon [currentValue] and [newValue].
     *
     * The provided predicate should return `true` if [currentValue] is NOT default
     * for its type AND if [newValue] is not equal to [currentValue].
     *
     * In pseudocode: `currentValue != default && currentValue != newValue`.
     *
     * @param currentValue an expression denoting the current field value.
     * @param newValue an expression denoting the proposed value.
     */
    protected abstract fun defaultOrSamePredicate(currentValue: String, newValue: String): String

    /**
     * Looks for the first child of this [PsiElement], the text representation of which
     * satisfies both [startsWith] and [contains] conditions.
     *
     * This method performs a depth-first search of the PSI hierarchy. So, the second direct
     * child of this [PsiElement] is checked only when the first child and all its descendants
     * are checked.
     */
    protected fun PsiElement.deepSearch(
        startsWith: String,
        contains: String = startsWith
    ): PsiStatement = children.firstNotNullOf { element ->
        val text = element.text
        when {
            !text.contains(contains) -> null
            text.startsWith(startsWith) -> element
            else -> element.deepSearch(startsWith, contains)
        }
    } as PsiStatement
}
