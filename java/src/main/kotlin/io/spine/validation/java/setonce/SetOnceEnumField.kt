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
import io.spine.protodata.ast.Field
import io.spine.tools.psi.java.method
import io.spine.validation.java.MessageWithFile

/**
 * Renders Java code to support `(set_once)` option for the given enum [field].
 *
 * @param field The enum field that declared the option.
 * @param declaredIn The message that contains the [field].
 */
internal class SetOnceEnumField(
    field: Field,
    declaredIn: MessageWithFile
) : SetOnceJavaConstraints(field, declaredIn) {

    init {
        check(field.type.isEnum) {
            "`${javaClass.simpleName}` handles only enum fields. " +
                    "The passed field: `$field`."
        }
    }

    override fun defaultOrSamePredicate(currentValue: String, newValue: String): String =
        "$currentValue != 0 && $currentValue != $newValue"

    override fun PsiClass.renderConstraints() {
        alterSetter()
        alterEnumValueSetter()
        alterBytesMerge(
            currentValue = "${fieldName}_",
            readerStartsWith = "${fieldName}_ = input.readEnum();"
        )
    }

    /**
     * Alters setter that accepts an enum constant.
     *
     * For example:
     *
     * ```
     * public Builder setMyEnum(MyEnum value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = defaultOrSameStatement(
            currentValue = "${fieldName}_",
            newValue = "value.getNumber()"
        )
        val setter = method(fieldSetterName).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * Alters setter that accepts an ordinal number.
     *
     * For example:
     *
     * ```
     * public Builder setMyEnumValue(int value)
     * ```
     */
    private fun PsiClass.alterEnumValueSetter() {
        val precondition = defaultOrSameStatement(
            currentValue = "${fieldName}_",
            newValue = "value"
        )
        val setter = method("${fieldSetterName}Value").body!!
        setter.addAfter(precondition, setter.lBrace)
    }
}
