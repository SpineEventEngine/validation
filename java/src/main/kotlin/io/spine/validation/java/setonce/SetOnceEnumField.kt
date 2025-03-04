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

import com.google.protobuf.Enum
import com.google.protobuf.ProtocolMessageEnum
import com.intellij.psi.PsiClass
import io.spine.protodata.ast.Field
import io.spine.protodata.type.TypeSystem
import io.spine.protodata.java.Expression
import io.spine.protodata.java.call
import io.spine.protodata.java.javaClassName
import io.spine.tools.psi.java.method

/**
 * Renders Java code to support `(set_once)` option for the given enum [field].
 *
 * Please note, in the generated Java code, Protobuf uses an ordinal number
 * to represent the currently set enum constant.
 *
 * @param field The enum field that declared the option.
 * @param typeSystem The type system to resolve types.
 * @param errorMessage The error message pattern to use in case of the violation.
 */
internal class SetOnceEnumField(
    field: Field,
    typeSystem: TypeSystem,
    errorMessage: String
) : SetOnceJavaConstraints<Int>(field, typeSystem, errorMessage) {

    init {
        check(field.type.isEnum) {
            "`${javaClass.simpleName}` handles only enum fields. " +
                    "The passed field: `$field`."
        }
    }

    private val fieldTypeClass = field.type.enumeration.javaClassName(typeSystem)

    override fun defaultOrSame(
        currentValue: Expression<Int>,
        newValue: Expression<Int>
    ): Expression<Boolean> = Expression("$currentValue == 0 || $currentValue == $newValue")

    override fun PsiClass.renderConstraints() {
        alterSetter()
        alterEnumValueSetter()
        alterBytesMerge(
            currentValue = Expression("${fieldName}_"),
            readerStartsWith = "${fieldName}_ = input.readEnum();"
        )
    }

    /**
     * Alters a setter that accepts an enum constant.
     *
     * For example:
     *
     * ```
     * public Builder setMyEnum(MyEnum value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression("${fieldName}_"),
            newValue = Expression("value.getNumber()"),
        )
        val setter = method(fieldSetterName).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * Alters a setter that accepts an ordinal number.
     *
     * For example:
     *
     * ```
     * public Builder setMyEnumValue(int value)
     * ```
     */
    private fun PsiClass.alterEnumValueSetter() {
        val precondition = throwIfNotDefaultAndNotSame(
            currentValue = Expression("${fieldName}_"),
            newValue = Expression("value")
        )
        val setter = method("${fieldSetterName}Value").body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * Returns the corresponding Java enum constant for the given ordinal number.
     */
    override fun asPayload(fieldValue: Expression<Int>): Expression<ProtocolMessageEnum> =
        asEnumConstant(fieldValue)

    /**
     * Converts the given [fieldValue] to string for a diagnostics message.
     *
     * We have to override this method because within the builder, an enum constant
     * is stored as [Int], but the [field] type is enum. This confuses the default
     * implementation of [asString], which yields an expression to convert to [String]
     * depending on the [field] type.
     */
    override fun asString(fieldValue: Expression<Int>): Expression<String> =
        fieldTypeClass
            .call<Enum>("forNumber", fieldValue)
            .chain("toString")

    /**
     * Converts an enum ordinal number to the corresponding constant.
     *
     * Enums are represented with numbers within message builders, but enum constants
     * seem more useful in diagnostics messages.
     */
    private fun asEnumConstant(fieldValue: Expression<Int>): Expression<ProtocolMessageEnum> =
        fieldTypeClass.call("forNumber", fieldValue)
}
