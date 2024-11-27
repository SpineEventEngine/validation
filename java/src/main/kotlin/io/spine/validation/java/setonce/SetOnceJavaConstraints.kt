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
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiStatement
import io.spine.base.FieldPath
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.InitVar
import io.spine.protodata.java.JavaElement
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.javaCase
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.listExpression
import io.spine.protodata.java.newBuilder
import io.spine.protodata.java.packToAny
import io.spine.protodata.java.render.findClass
import io.spine.protodata.java.toPsi
import io.spine.protodata.render.SourceFile
import io.spine.protodata.type.TypeSystem
import io.spine.string.camelCase
import io.spine.tools.code.Java
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.execute
import io.spine.validate.ConstraintViolation
import io.spine.validation.IF_SET_AGAIN
import io.spine.validation.java.setonce.SetOnceToken.CURRENT_VALUE
import io.spine.validation.java.setonce.SetOnceToken.Companion.TokenRegex
import io.spine.validation.java.setonce.SetOnceToken.Companion.supportedTokens
import io.spine.validation.java.setonce.SetOnceToken.FIELD_NAME
import io.spine.validation.java.setonce.SetOnceToken.PROPOSED_VALUE

/**
 * Renders Java code to support `(set_once)` option for the given [field].
 *
 * The rendered Java constraints are specific to the field type. This class
 * serves as an abstract base, providing common methods and the skeleton implementation
 * of [render] method. Inheritors should perform actual rendering in [renderConstraints].
 *
 * @param T The field data type. See docs to an abstract [defaultOrSame] method for usage details.
 *
 * @property field The field that declared the option.
 * @property typeSystem The type system to resolve types.
 * @property errorMessage The error message pattern to use in case of the violation.
 */
internal sealed class SetOnceJavaConstraints<T>(
    private val field: Field,
    private val typeSystem: TypeSystem,
    private val errorMessage: String
) {

    private companion object {

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

    private val declaringMessage: TypeName = field.declaringType

    protected val fieldName = field.name.javaCase()
    protected val fieldNameCamel = fieldName.camelCase()
    protected val fieldGetterName = "get$fieldNameCamel"
    protected val fieldSetterName = "set$fieldNameCamel"
    protected val fieldGetter = "$fieldGetterName()"
    protected val declaringMessageClass = declaringMessage.javaClassName(typeSystem)

    /**
     * Renders Java constraints in the given [sourceFile] to make sure that the [field]
     * can be assigned only once.
     *
     * The [field] can be assigned a new value only if the current value is default
     * for the field type OR if the assigned value is the same with the current one.
     *
     * @param sourceFile Protobuf-generated Java source code of the message declared the [field].
     *
     * @see defaultOrSame
     * @see throwIfNotDefaultAndNotSame
     */
    fun render(sourceFile: SourceFile<Java>) {
        val messageBuilder = ClassName(
            packageName = declaringMessageClass.packageName,
            simpleNames = declaringMessageClass.simpleNames + "Builder"
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
     * This [PsiClass] represents a Java builder for the message declared the [field].
     */
    protected abstract fun PsiClass.renderConstraints()

    /**
     * Alters [MergeFromBytesSignature] method to make sure that a set-once field
     * is not overridden during the merge from a byte array.
     *
     * Such a merge is done field-by-field. In the method body, each field is handled
     * in a separate `case` block of the `switch` statement. Such a block reads a new value
     * and assigns it to the field.
     *
     * This method finds the place, where the given [field] is processed. It adds a statement
     * to remember the current field value before reading a new one. After the reading,
     * it adds [throwIfNotDefaultAndNotSame] statement to be sure the just read value can
     * be assigned.
     *
     * Implementation of this method is common for all field types. Inheritors should
     * invoke it within [renderConstraints], passing the necessary parameters.
     *
     * The [currentValue] and [readerStartsWith] are mandatory properties. Pass [readerContains]
     * in cases when [readerStartsWith] is not sufficient. For example, for message fields,
     * the beginning of the reading block is the same for all message fields because it doesn't
     * include the field name. Differentiation is done way further.
     *
     * @param currentValue The current field value.
     * @param readerStartsWith The beginning of the field reading block.
     * @param readerContains An arbitrary code that must be present within the reading block.
     */
    protected fun PsiClass.alterBytesMerge(
        currentValue: Expression<T>,
        readerStartsWith: JavaElement,
        readerContains: JavaElement = readerStartsWith,
    ) {

        val mergeFromBytes = methodWithSignature(MergeFromBytesSignature).body!!
        val fieldReading = mergeFromBytes.deepSearch(readerStartsWith, readerContains)
        val fieldCaseBlock = fieldReading.parent

        val previousValue = InitVar("previous", currentValue)
        fieldCaseBlock.addBefore(previousValue.toPsi(), fieldReading)

        val postcondition = throwIfNotDefaultAndNotSame(
            currentValue = previousValue.read(),
            newValue = currentValue,
        )
        fieldCaseBlock.addAfter(postcondition, fieldReading)
    }

    /**
     * Creates an `if` statement, which checks that the current field value is default
     * OR if the proposed [newValue] is the same as the current.
     *
     * Otherwise, it throws the validation exception.
     *
     * @param currentValue The current field value.
     * @param newValue The proposed new value.
     */
    protected fun throwIfNotDefaultAndNotSame(
        currentValue: Expression<T>,
        newValue: Expression<T>
    ): PsiStatement {
        val (format, tokens) = extractTokens(errorMessage)
        val params = tokens.toParams(currentValue, newValue)

        val fieldPath = ClassName(FieldPath::class).newBuilder()
            .chainAdd("field_name", StringLiteral(fieldName))
            .chainBuild<FieldPath>()
        val violation = ClassName(ConstraintViolation::class).newBuilder()
            .chainSet("msg_format", StringLiteral(format))
            .chainAddAll("param", listExpression(params))
            .chainSet("type_name", StringLiteral(declaringMessage.qualifiedName))
            .chainSet("field_path", fieldPath)
            .chainSet("field_value", newValue.packToAny())
            .chainBuild<ConstraintViolation>()

        return elementFactory.createStatement(
            """
            if (!(${defaultOrSame(currentValue, newValue)})) {
                throw new io.spine.validate.ValidationException($violation);
            }""".trimIndent()
        )
    }

    private fun extractTokens(errorMessage: String): Pair<String, List<SetOnceToken>> {
        val foundTokens = mutableListOf<SetOnceToken>()
        val formatted = TokenRegex.replace(errorMessage) { matchResult ->
            val tokenName = matchResult.groupValues[1]
            val token = SetOnceToken.forSimpleName(tokenName)
            if (token != null) {
                "%s".also { foundTokens.add(token) }
            } else {
                throw IllegalArgumentException(
                    "The `($IF_SET_AGAIN)` option doesn't support the token: `{${tokenName}}`. " +
                            "The supported tokens: `${supportedTokens()}`. " +
                            "The declared field: `${field.qualifiedName}`.")
            }
        }
        return formatted to foundTokens
    }

    private fun List<SetOnceToken>.toParams(
        currentValue: Expression<T>,
        newValue: Expression<T>
    ): List<Expression<String>> = map {
        when (it) {
            FIELD_NAME -> StringLiteral(fieldName)
            CURRENT_VALUE -> StringLiteral("$currentValue")
            PROPOSED_VALUE -> StringLiteral("$newValue")
        }
    }

    /**
     * Returns a boolean expression upon the field's [currentValue] and the proposed [newValue].
     *
     * The provided expression should return `true` if any of the conditions is met:
     *
     * 1. The [currentValue] is default for its type.
     * 2. The [newValue] is equal to the [currentValue].
     *
     * In pseudocode: `currentValue == default || currentValue == newValue`.
     *
     * @param currentValue An expression denoting the current field value.
     * @param newValue An expression denoting the proposed new value.
     */
    protected abstract fun defaultOrSame(
        currentValue: Expression<T>,
        newValue: Expression<T>
    ): Expression<Boolean>

    /**
     * Looks for the first child of this [PsiElement], the text representation of which
     * satisfies both [startsWith] and [contains] criteria.
     *
     * This method performs a depth-first search of the PSI hierarchy. So, the second direct
     * child of this [PsiElement] is checked only when the first child and all its descendants
     * are checked.
     */
    protected fun PsiElement.deepSearch(
        startsWith: JavaElement,
        contains: JavaElement = startsWith
    ): PsiStatement = children.firstNotNullOf { element ->
        val text = element.text
        when {
            !text.contains("$contains") -> null
            text.startsWith("$startsWith") -> element
            else -> element.deepSearch(startsWith, contains)
        }
    } as PsiStatement

    /**
     * Creates a new [PsiStatement] from the given [text].
     */
    private fun PsiElementFactory.createStatement(text: String) =
        createStatementFromText(text, null)

}

private enum class SetOnceToken(val simpleName: String) {

    FIELD_NAME("fieldName"),
    CURRENT_VALUE("currentValue"),
    PROPOSED_VALUE("proposedValue");

    companion object {

        val TokenRegex = Regex("""\{(.*?)}""")

        fun forSimpleName(simpleName: String): SetOnceToken? =
            values().firstOrNull { it.simpleName == simpleName }

        fun supportedTokens(): String = values().joinToString { "{${it.simpleName}}" }
    }
}
