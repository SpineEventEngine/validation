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

import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiIfStatement
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.TYPE_BOOL
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
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
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT64
import io.spine.string.camelCase
import io.spine.tools.psi.java.method
import io.spine.validation.java.MessageWithFile

internal fun interface DefaultOrSamePredicate : (String, String) -> String

/**
 * Renders Java code to support `(set_once)` option for the given primitive [field].
 *
 * @param field The primitive field that declared the option.
 * @param declaredIn The message that contains the [field].
 */
internal open class SetOncePrimitiveField(
    field: Field,
    declaredIn: MessageWithFile
) : SetOnceJavaConstraints(field, declaredIn) {

    companion object {
        private val CustomFieldReaders = mapOf(
            TYPE_UINT32 to "readUInt32", TYPE_UINT64 to "readUInt64",
            TYPE_SINT32 to "readSInt32", TYPE_SINT64 to "readSInt64",
            TYPE_SFIXED32 to "readSFixed32", TYPE_SFIXED64 to "readSFixed64",
            TYPE_STRING to "readStringRequireUtf8"
        )
        private val NumberPrimitives = listOf(
            TYPE_DOUBLE, TYPE_FLOAT, TYPE_INT32, TYPE_INT64, TYPE_UINT32, TYPE_UINT64,
            TYPE_SINT32, TYPE_SINT64, TYPE_FIXED32, TYPE_FIXED64, TYPE_SFIXED32, TYPE_SFIXED64
        )
        val SupportedPrimitives = buildMap<PrimitiveType, DefaultOrSamePredicate> {
            put(TYPE_STRING) { currentValue: String, newValue: String ->
                "!$currentValue.isEmpty() && !$currentValue.equals($newValue)"
            }
            put(TYPE_BOOL) { currentValue: String, newValue: String ->
                "$currentValue != false && $currentValue != $newValue"
            }
            put(TYPE_BYTES) { currentValue: String, newValue: String ->
                "$currentValue != com.google.protobuf.ByteString.EMPTY " +
                        "&& !$currentValue.equals($newValue)"
            }
            NumberPrimitives.forEach { type ->
                put(type) { currentValue: String, newValue: String ->
                    "$currentValue != 0 && $currentValue != $newValue"
                }
            }
        }
    }

    private val fieldReader: String
    private val defaultOrSame: DefaultOrSamePredicate
    private val fieldType: PrimitiveType = field.type.primitive

    init {
        check(SupportedPrimitives.contains(fieldType)) {
            "`${javaClass.simpleName}` handles only primitive fields. " +
                    "The passed field: `$field`."
        }

        val javaTypeName = fieldType.name
            .substringAfter("_")
            .lowercase()
            .camelCase()

        fieldReader = "${CustomFieldReaders[fieldType] ?: "read$javaTypeName"}()"
        defaultOrSame = SupportedPrimitives[fieldType]!!
    }

    override fun defaultOrSamePredicate(currentValue: String, newValue: String): String =
        defaultOrSame(currentValue, newValue)

    override fun PsiClass.renderConstraints() {
        alterSetter()
        alterBytesMerge(
            currentValue = fieldGetter,
            readerStartsWith = "${fieldName}_ = input.$fieldReader;"
        )
        if (fieldType == TYPE_STRING) {
            alterStringBytesSetter()
            alterMessageMerge()
        }
    }

    /**
     * Alters setter that accepts a value.
     *
     * For example:
     *
     * ```
     * public Builder setMyInt(int value)
     * ```
     */
    private fun PsiClass.alterSetter() {
        val precondition = defaultOrSameStatement(currentValue = fieldGetter, newValue = "value")
        val setter = method(fieldSetterName).body!!
        setter.addAfter(precondition, setter.lBrace)
    }

    /**
     * Alters a string-specific setter that accepts a `ByteString`.
     *
     * For example:
     *
     * ```
     * public Builder setMyStingBytes(com.google.protobuf.ByteString value)
     * ```
     */
    private fun PsiClass.alterStringBytesSetter() {
        val precondition = defaultOrSameStatement(
            currentValue = "${fieldGetterName}Bytes()",
            newValue = "value"
        )
        val bytesSetter = method("${fieldSetterName}Bytes").body!!
        bytesSetter.addAfter(precondition, bytesSetter.lBrace)
    }

    /**
     * Alters the message-based merge to modify merging of a string field.
     *
     * During the message merge, all primitives delegate updating the field value
     * to their simple setters. Exception here is `string` type, which does it in-place.
     * So, we have to add a check there.
     */
    private fun PsiClass.alterMessageMerge() {
        val precondition = defaultOrSameStatement(
            currentValue = fieldGetter,
            newValue = "other.$fieldGetter"
        )
        val mergeFromMessage = getMethodBySignature(
            "public Builder mergeFrom(${declaringMessage.canonical} other)"
        ).body!!
        val fieldCheck = mergeFromMessage.deepSearch(
            "if (!other.$fieldGetter.isEmpty())"
        ) as PsiIfStatement
        val fieldProcessing = (fieldCheck.thenBranch!! as PsiBlockStatement).codeBlock
        fieldProcessing.addAfter(precondition, fieldProcessing.lBrace)
    }
}
