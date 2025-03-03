/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import com.google.protobuf.ByteString
import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiIfStatement
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.java.Expression
import io.spine.protodata.java.MethodCall
import io.spine.protodata.type.TypeSystem
import io.spine.tools.psi.java.getFirstByText
import io.spine.tools.psi.java.method
import io.spine.tools.psi.java.methodWithSignature

/**
 * A type that can be either [String] or [ByteString].
 *
 * In the generated Java code, Protobuf uses [ByteString] to represent strings.
 * Though, it has setters, which accept just a [String]. For example, the field's
 * direct setter accepts exactly [String], but we have to compare it with [ByteString]
 * because the internal representation of the current value is [ByteString].
 *
 * It doesn't lead to compiler or runtime errors because `isEmpty()`, `equals()` and
 * `toString()` methods are present in both, and equality check between [String]
 * and [ByteString] doesn't take into account the class, only the content it holds.
 */
internal object StringOrByteString

/**
 * Renders Java code to support `(set_once)` option for the given string [field].
 *
 * @param field The string field that declared the option.
 * @param typeSystem The type system to resolve types.
 * @param errorMessage The error message pattern to use in case of the violation.
 */
internal class SetOnceStringField(
    field: Field,
    typeSystem: TypeSystem,
    errorMessage: String
) : SetOnceJavaConstraints<StringOrByteString>(field, typeSystem, errorMessage) {

    init {
        check(field.type.primitive == PrimitiveType.TYPE_STRING) {
            "`${javaClass.simpleName}` handles only string fields. " +
                    "The passed field: `$field`."
        }
    }

    override fun defaultOrSame(
        currentValue: Expression<StringOrByteString>,
        newValue: Expression<StringOrByteString>
    ): Expression<Boolean> = Expression(
        "$currentValue.isEmpty() || $currentValue.equals($newValue)"
    )

    override fun PsiClass.renderConstraints() {
        alterSetter()
        alterStringBytesSetter()
        alterMessageMerge()
        alterBytesMerge(
            currentValue = Expression(fieldGetter),
            readerStartsWith = "${fieldName}_ = input.readStringRequireUtf8();"
        )
    }

    /**
     * Alters a setter that accepts a value.
     *
     * For example:
     *
     * ```
     * public Builder setMyString(String value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression(fieldGetter),
            newValue = Expression("value")
        )
        val setter = method(fieldSetterName).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * Alters a string-specific setter that accepts a `ByteString`.
     *
     * For example:
     *
     * ```
     * public Builder setMyStringBytes(com.google.protobuf.ByteString value)
     * ```
     */
    private fun PsiClass.alterStringBytesSetter() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression("${fieldGetterName}Bytes()"),
            newValue = Expression("value")
        )
        val bytesSetter = method("${fieldSetterName}Bytes").body!!
        bytesSetter.addAfter(precondition, bytesSetter.lBrace)
    }

    /**
     * Alters the message-based merge method to modify merging of a string field.
     *
     * During the message merge, all primitives delegate updating the field value
     * to their simple setters. Exception here is `string` type, which does it in-place.
     * So, we have to add a check there.
     */
    private fun PsiClass.alterMessageMerge() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression(fieldGetter),
            newValue = Expression("other.$fieldGetter")
        )
        val mergeFromMessage = methodWithSignature(
            "public Builder mergeFrom(${declaringMessageClass.canonical} other)"
        ).body!!
        val fieldCheck = mergeFromMessage.getFirstByText(
            "if (!other.$fieldGetter.isEmpty())"
        ) as PsiIfStatement
        val fieldProcessing = (fieldCheck.thenBranch!! as PsiBlockStatement).codeBlock
        fieldProcessing.addAfter(precondition, fieldProcessing.lBrace)
    }

    /**
     * Converts the given [fieldValue] to string for a diagnostics message.
     *
     * We have to override this method because within the builder, a string is stored
     * as [StringOrByteString], but the [field] type is just [String]. This confuses
     * the default implementation of [asString], which yields an expression to convert
     * to [String] depending on the [field] type.
     */
    override fun asString(fieldValue: Expression<StringOrByteString>): Expression<String> =
        MethodCall(fieldValue, "toString")
}
