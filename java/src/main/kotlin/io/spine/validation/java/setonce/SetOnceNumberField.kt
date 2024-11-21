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
import io.spine.protodata.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_INT64
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT64
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT64
import io.spine.protodata.java.AnElement
import io.spine.protodata.java.Expression
import io.spine.protodata.type.TypeSystem
import io.spine.tools.psi.java.method

/**
 * Renders Java code to support `(set_once)` option for the given primitive [field].
 *
 * @param field The primitive field that declared the option.
 * @param typeSystem The type system to resolve types.
 */
internal class SetOnceNumberField(
    field: Field,
    typeSystem: TypeSystem
) : SetOnceJavaConstraints<Number>(field, typeSystem) {

    companion object {
        private val FieldReaders = mapOf(
            TYPE_DOUBLE to "readDouble", TYPE_FLOAT to "readFloat",
            TYPE_INT32 to "readInt32", TYPE_INT64 to "readInt64",
            TYPE_UINT32 to "readUInt32", TYPE_UINT64 to "readUInt64",
            TYPE_SINT32 to "readSInt32", TYPE_SINT64 to "readSInt64",
            TYPE_FIXED32 to "readFixed32", TYPE_FIXED64 to "readFixed64",
            TYPE_SFIXED32 to "readSFixed32", TYPE_SFIXED64 to "readSFixed64",
        )
        val SupportedNumbers = FieldReaders.keys
    }

    private val fieldType = field.type.primitive
    private val fieldReader = FieldReaders[fieldType]!!

    override fun defaultOrSame(
        currentValue: Expression<Number>,
        newValue: Expression<Number>
    ): Expression<Boolean> = Expression("$currentValue == 0 || $currentValue == $newValue")

    override fun PsiClass.renderConstraints() {
        alterSetter()
        alterBytesMerge(
            currentValue = Expression(fieldGetter),
            readerStartsWith = AnElement("${fieldName}_ = input.$fieldReader();")
        )
    }

    /**
     * Alters a setter that accepts a value.
     *
     * For example:
     *
     * ```
     * public Builder setMyInt(int value)
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
}
