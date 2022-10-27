/*
 * Copyright 2022, TeamDev. All rights reserved.
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
import com.google.protobuf.empty
import io.spine.protobuf.pack
import io.spine.protodata.Option
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.constantName
import io.spine.protodata.enumConstant
import io.spine.protodata.enumType
import io.spine.protodata.field
import io.spine.protodata.fieldName
import io.spine.protodata.file
import io.spine.protodata.filePath
import io.spine.protodata.messageType
import io.spine.protodata.option
import io.spine.protodata.type
import io.spine.protodata.typeName
import io.spine.validation.Value
import io.spine.validation.enumValue
import io.spine.validation.messageValue
import io.spine.validation.value
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`TypeSystem` should")
class TypeSystemSpec {

    private val filePath = filePath { 
        value = "acme/example/foo.proto" 
    }
    
    private val multipleFilesOption: Option = option {
        name = "java_multiple_files"
        number = JAVA_MULTIPLE_FILES_FIELD_NUMBER
        type = type { primitive = TYPE_BOOL }
        value = BoolValue.of(true).pack()
    } 

    private val file = file {
        path = filePath
        packageName = "acme.example"
        option.add(multipleFilesOption)
    } 

    private val messageTypeName = typeName { 
        packageName = file.packageName
        simpleName = "Foo"
        typeUrlPrefix = "type.spine.io"
    } 
        
    private val field = field { 
        type = type { primitive = TYPE_STRING }
        name = fieldName { value = "bar" }
        single = empty { }
    } 
        
    private val messageType = messageType { 
        file = filePath
        name = messageTypeName
        field.add(this@TypeSystemSpec.field) 
    } 
        
    private val enumTypeName = typeName { 
        packageName = file.packageName
        typeUrlPrefix = messageTypeName.typeUrlPrefix
        simpleName = "Kind"
    } 
        
    private val undefinedConstant = enumConstant {
        name = constantName { value = "UNDEFINED" }
        number = 0
    } 
        
    private val enumConstant = enumConstant { 
        name = constantName { value = "INSTANCE" }
        number = 1
    } 
        
    private val enumType = enumType { 
        file = filePath
        name = enumTypeName
        constant.apply {
            add(undefinedConstant)
            add(enumConstant)
        }
    } 

    private val typeSystem: TypeSystem = TypeSystem.newBuilder()
        .put(file, messageType)
        .put(file, enumType)
        .build()

    @Nested
    inner class `Convert a 'Value' into an expression for` {

        @Test
        fun ints() {
            val value = value { intValue = 42 } 
            checkCode(value, "42")
        }

        @Test
        fun floats() {
            val value = value { doubleValue = .1 } 
            checkCode(value, "0.1")
        }

        @Test
        fun bool() {
            val value = value { boolValue = true } 
            checkCode(value, "true")
        }

        @Test
        fun string() {
            val value = value { stringValue = "hello" } 
            checkCode(value, "\"hello\"")
        }

        @Test
        fun bytes() {
            val value = value {
                bytesValue = ByteString.copyFrom(ByteArray(3) { index -> index.toByte() }) 
            } 
            checkCode(value, "${ByteString::class.qualifiedName}.copyFrom(new byte[]{0, 1, 2})")
        }

        @Test
        fun `empty message`() {
            val emptyMessage = messageValue {
                type = messageTypeName
            } 
            val value = value { messageValue = emptyMessage } 
            checkCode(value, "acme.example.Foo.getDefaultInstance()")
        }

        @Test
        fun `message with a field`() {
            val message = messageValue {
                type = messageTypeName
                fields["bar"] = value { stringValue = "hello there" }
            } 
            val value = value { messageValue = message } 
            checkCode(value, "acme.example.Foo.newBuilder().setBar(\"hello there\").build()")
        }

        @Test
        fun `enum value`() {
            val enumVal = enumValue { 
                type = enumTypeName
                constNumber = 1
            } 
            val value = value { enumValue = enumVal } 
            checkCode(value, "acme.example.Kind.forNumber(1)")
        }

        private fun checkCode(value: Value, expectedCode: String) {
            val expression = typeSystem.valueToJava(value)
            assertThat(expression.toCode())
                .isEqualTo(expectedCode)
        }
    }
}
