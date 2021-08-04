/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.ByteString
import io.spine.protodata.EnumType
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.PrimitiveType.PT_UNKNOWN
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.PrimitiveType.TYPE_INT32
import io.spine.protodata.PrimitiveType.TYPE_INT64
import io.spine.protodata.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.PrimitiveType.TYPE_SINT32
import io.spine.protodata.PrimitiveType.TYPE_SINT64
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.PrimitiveType.TYPE_UINT32
import io.spine.protodata.PrimitiveType.TYPE_UINT64
import io.spine.protodata.PrimitiveType.UNRECOGNIZED
import io.spine.protodata.Type
import io.spine.protodata.Type.KindCase.ENUMERATION
import io.spine.protodata.Type.KindCase.MESSAGE
import io.spine.protodata.Type.KindCase.PRIMITIVE
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.LiteralBytes
import io.spine.protodata.codegen.java.LiteralString
import io.spine.protodata.codegen.java.MethodCall
import io.spine.protodata.codegen.java.Null
import io.spine.protodata.codegen.java.javaClassName
import io.spine.protodata.codegen.java.listExpression
import io.spine.protodata.codegen.java.mapExpression
import io.spine.protodata.typeUrl
import io.spine.validation.Value
import io.spine.validation.Value.KindCase.BOOL_VALUE
import io.spine.validation.Value.KindCase.BYTES_VALUE
import io.spine.validation.Value.KindCase.DOUBLE_VALUE
import io.spine.validation.Value.KindCase.ENUM_VALUE
import io.spine.validation.Value.KindCase.INT_VALUE
import io.spine.validation.Value.KindCase.LIST_VALUE
import io.spine.validation.Value.KindCase.MAP_VALUE
import io.spine.validation.Value.KindCase.MESSAGE_VALUE
import io.spine.validation.Value.KindCase.NULL_VALUE
import io.spine.validation.Value.KindCase.STRING_VALUE

/**
 * A type system of an application.
 *
 * Includes all the types known to the app at runtime.
 */
internal class TypeSystem
private constructor(
    private val knownTypes: Map<String, ClassName>
) {

    /**
     * Converts the given [Value] of a Java expression which creates that value.
     *
     * For different types produces different expressions:
     *  - for a primitive type, a literal;
     *  - for byte strings, a construction of a [ByteString] out of a byte array literal;
     *  - for message, a builder invocation;
     *  - for an enum, a call of the `forNumber` static method;
     *  - for lists and maps, construction of a Guava `ImmutableList` or `ImmutableMap`.
     */
    fun valueToJava(value: Value): Expression {
        return when (value.kindCase) {
            NULL_VALUE -> Null
            BOOL_VALUE -> Literal(value.boolValue)
            DOUBLE_VALUE -> Literal(value.doubleValue)
            INT_VALUE -> Literal(value.intValue)
            STRING_VALUE -> LiteralString(value.stringValue)
            BYTES_VALUE -> LiteralBytes(value.bytesValue)
            MESSAGE_VALUE -> messageValueToJava(value)
            ENUM_VALUE -> enumValueToJava(value)
            LIST_VALUE -> listExpression(listValuesToJava(value))
            MAP_VALUE -> mapValueToJava(value)
            else -> throw IllegalArgumentException("Empty value")
        }
    }

    private fun mapValueToJava(value: Value): MethodCall {
        val firstEntry = value.mapValue.valueList.firstOrNull()
        val firstKey = firstEntry?.key
        val keyClass = firstKey?.type?.let(this::toClass)
        val firstValue = firstEntry?.value
        val valueClass = firstValue?.type?.let(this::toClass)
        return mapExpression(mapValuesToJava(value), keyClass, valueClass)
    }

    private fun enumValueToJava(value: Value): MethodCall {
        val enumValue = value.enumValue
        val enumClassName: ClassName = knownTypes.getValue(enumValue.type.typeUrl())
        return enumClassName.enumValue(enumValue.constNumber)
    }

    private fun messageValueToJava(value: Value): Expression {
        val messageValue = value.messageValue
        val className: ClassName = knownTypes.getValue(messageValue.type.typeUrl())
        return if (messageValue.fieldsMap.isEmpty()) {
            className.getDefaultInstance()
        } else {
            var builder = className.newBuilder()
            messageValue.fieldsMap.forEach { (k, v) ->
                builder = builder.chainSet(k, valueToJava(v))
            }
            builder.chainBuild()
        }
    }

    private fun listValuesToJava(value: Value): List<Expression> =
        value.listValue
            .valuesList
            .map {
                valueToJava(it)
            }

    private fun mapValuesToJava(value: Value): Map<Expression, Expression> =
        value.mapValue.valueList.associate { valueToJava(it.key) to valueToJava(it.value) }

    /**
     * Obtains the canonical name of the class representing the given [type] in Java.
     *
     * For Java primitive types, obtains wrapper classes.
     *
     * @throws IllegalStateException if the type is unknown
     */
    @JvmOverloads
    fun toClass(type: Type, label: String = "type"): ClassName = when (type.kindCase) {
        PRIMITIVE -> type.primitive.toClass(label)
        MESSAGE, ENUMERATION -> {
            val typeUrl = type.message.typeUrl()
            knownTypes[typeUrl] ?: unknownType(label, typeUrl)
        }
        else -> throw IllegalArgumentException("Type is empty.")
    }

    private fun PrimitiveType.toClass(label: String): ClassName {
        val klass = when (this) {
            TYPE_DOUBLE -> Double::class
            TYPE_FLOAT -> Float::class
            TYPE_INT64, TYPE_UINT64, TYPE_SINT64, TYPE_FIXED64, TYPE_SFIXED64 -> Long::class
            TYPE_INT32, TYPE_UINT32, TYPE_SINT32, TYPE_FIXED32, TYPE_SFIXED32 -> Int::class
            TYPE_BOOL -> Boolean::class
            TYPE_STRING -> String::class
            TYPE_BYTES -> ByteString::class
            UNRECOGNIZED, PT_UNKNOWN -> unknownType(label, this)
        }
        return ClassName(klass.javaObjectType)
    }

    private fun unknownType(label: String, key: Any): Nothing {
        throw IllegalStateException("Unknown $label: `$key`.")
    }

    companion object {

        /**
         * Creates a new `TypeSystem` builder.
         */
        @JvmStatic
        fun newBuilder() = Builder()
    }

    class Builder internal constructor() {

        private val knownTypes = mutableMapOf<String, ClassName>()

        @CanIgnoreReturnValue
        fun put(file: File, messageType: MessageType): Builder {
            val javaClassName = messageType.javaClassName(declaredIn = file)
            knownTypes[messageType.typeUrl()] = javaClassName
            return this
        }

        @CanIgnoreReturnValue
        fun put(file: File, enumType: EnumType): Builder {
            val javaClassName = enumType.javaClassName(declaredIn = file)
            knownTypes[enumType.typeUrl()] = javaClassName
            return this
        }

        /**
         * Builds an instance of `TypeSystem`.
         */
        fun build() = TypeSystem(knownTypes)
    }
}
