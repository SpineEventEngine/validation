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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.BoolValue
import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER
import com.google.protobuf.Empty
import io.spine.protobuf.AnyPacker
import io.spine.protodata.ConstantName
import io.spine.protodata.EnumConstant
import io.spine.protodata.EnumType
import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.File
import io.spine.protodata.FilePath
import io.spine.protodata.MessageType
import io.spine.protodata.Option
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.Type
import io.spine.protodata.TypeName
import io.spine.validation.EnumValue
import io.spine.validation.MessageValue
import io.spine.validation.Value
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class `'TypeSystem' should` {

    private val filePath = FilePath
        .newBuilder()
        .setValue("acme/example/foo.proto")
        .build()
    val multipleFilesOption = Option.newBuilder().setName("java_multiple_files")
        .setNumber(JAVA_MULTIPLE_FILES_FIELD_NUMBER)
        .setType(Type.newBuilder().setPrimitive(TYPE_BOOL))
        .setValue(AnyPacker.pack(BoolValue.of(true)))
        .build()
    private val file = File
        .newBuilder()
        .setPath(filePath)
        .setPackageName("acme.example")
        .addOption(multipleFilesOption)
        .build()
    private val messageTypeName = TypeName
        .newBuilder()
        .setPackageName(file.packageName)
        .setSimpleName("Foo")
        .setTypeUrlPrefix("type.spine.io")
        .build()
    private val field = Field
        .newBuilder()
        .setType(Type.newBuilder().setPrimitive(TYPE_STRING))
        .setName(FieldName.newBuilder().setValue("bar"))
        .setSingle(Empty.getDefaultInstance())
        .build()
    private val messageType = MessageType
        .newBuilder()
        .setFile(filePath)
        .setName(messageTypeName)
        .addField(field)
        .build()
    private val enumTypeName = TypeName
        .newBuilder()
        .setPackageName(file.packageName)
        .setTypeUrlPrefix(messageTypeName.typeUrlPrefix)
        .setSimpleName("Kind")
        .build()
    private val undefinedConstant = EnumConstant
        .newBuilder()
        .setName(ConstantName.newBuilder().setValue("UNDEFINED"))
        .setNumber(0)
        .build()
    private val enumConstant = EnumConstant
        .newBuilder()
        .setName(ConstantName.newBuilder().setValue("INSTANCE"))
        .setNumber(1)
        .build()
    private val enumType = EnumType
        .newBuilder()
        .setFile(filePath)
        .setName(enumTypeName)
        .addConstant(undefinedConstant)
        .addConstant(enumConstant)
        .build()
    private val typeSystem: TypeSystem = TypeSystem
        .newBuilder()
        .put(file, messageType)
        .put(file, enumType)
        .build()

    @Nested
    inner class `Convert a 'Value' into an expression for` {

        @Test
        fun ints() {
            val value = Value.newBuilder()
                .setIntValue(42)
                .build()
            checkCode(value, "42")
        }

        @Test
        fun floats() {
            val value = Value.newBuilder()
                .setDoubleValue(.1)
                .build()
            checkCode(value, "0.1")
        }

        @Test
        fun bool() {
            val value = Value.newBuilder()
                .setBoolValue(true)
                .build()
            checkCode(value, "true")
        }

        @Test
        fun string() {
            val value = Value.newBuilder()
                .setStringValue("hello")
                .build()
            checkCode(value, "\"hello\"")
        }

        @Test
        fun bytes() {
            val value = Value.newBuilder()
                .setBytesValue(ByteString.copyFrom(ByteArray(3) { index -> index.toByte() }))
                .build()
            checkCode(value, "${ByteString::class.qualifiedName}.copyFrom(new byte[]{0, 1, 2})")
        }

        @Test
        fun `empty message`() {
            val emptyMessage = MessageValue.newBuilder()
                .setType(messageTypeName)
                .build()
            val value = Value.newBuilder()
                .setMessageValue(emptyMessage)
                .build()
            checkCode(value, "acme.example.Foo.getDefaultInstance()")
        }

        @Test
        fun `message with a field`() {
            val message = MessageValue.newBuilder()
                .setType(messageTypeName)
                .putFields("bar", Value.newBuilder()
                    .setStringValue("hello there")
                    .build())
                .build()
            val value = Value.newBuilder()
                .setMessageValue(message)
                .build()
            checkCode(value, "acme.example.Foo.newBuilder().setBar(\"hello there\").build()")
        }

        @Test
        fun `enum value`() {
            val enumValue = EnumValue
                .newBuilder()
                .setType(enumTypeName)
                .setConstNumber(1)
                .build()
            val value = Value.newBuilder()
                .setEnumValue(enumValue)
                .build()
            checkCode(value, "acme.example.Kind.forNumber(1)")
        }

        private fun checkCode(value: Value, expectedCode: String) {
            val expression = typeSystem.valueToJava(value)
            assertThat(expression.toCode())
                .isEqualTo(expectedCode)
        }
    }
}
