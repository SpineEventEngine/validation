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

package io.spine.validation

import com.google.protobuf.StringValue
import io.spine.option.OptionsProto
import io.spine.protobuf.AnyPacker
import io.spine.protodata.CodeGenerationContext
import io.spine.protodata.Field
import io.spine.protodata.FieldEntered
import io.spine.protodata.FieldName
import io.spine.protodata.FieldOptionDiscovered
import io.spine.protodata.File
import io.spine.protodata.File.SyntaxVersion.PROTO3
import io.spine.protodata.FileEntered
import io.spine.protodata.FilePath
import io.spine.protodata.MessageType
import io.spine.protodata.Option
import io.spine.protodata.PrimitiveType
import io.spine.protodata.PrimitiveType.TYPE_INT32
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.Type
import io.spine.protodata.TypeEntered
import io.spine.protodata.TypeName
import io.spine.testing.server.blackbox.BlackBox
import io.spine.validation.ComparisonOperator.GREATER_OR_EQUAL
import io.spine.validation.ComparisonOperator.LESS_OR_EQUAL
import io.spine.validation.event.CompositeRuleAdded
import io.spine.validation.event.SimpleRuleAdded
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class `Validation policies should` {

    private lateinit var blackBox: BlackBox
    private val filePath = FilePath.newBuilder()
        .setValue("example/bar.proto")
        .build()
    private val typeName = TypeName.newBuilder()
        .setTypeUrlPrefix("type.example.org")
        .setPackageName("example.test")
        .setSimpleName("Bar")
        .build()
    private val fieldName = FieldName.newBuilder()
        .setValue("foo")
        .build()

    @BeforeEach
    fun prepareBlackBox() {
        val ctx = CodeGenerationContext.builder()
        ValidationPlugin().policies().forEach { ctx.addEventDispatcher(it) }
        blackBox = BlackBox.from(ctx)

        val file = File
            .newBuilder()
            .setPath(filePath)
            .setPackageName(typeName.packageName)
            .setSyntax(PROTO3)
            .build()
        val type = MessageType
            .newBuilder()
            .setName(typeName)
            .setFile(filePath)
            .build()
        val field = Field
            .newBuilder()
            .setDeclaringType(typeName)
            .setName(fieldName)
            .setType(primitive(TYPE_INT32))
            .build()
        blackBox.receivesExternalEvents(
            FileEntered
                .newBuilder()
                .setPath(filePath)
                .setFile(file)
                .build(),
            TypeEntered
                .newBuilder()
                .setFile(filePath)
                .setType(type)
                .build(),
            FieldEntered
                .newBuilder()
                .setField(field)
                .setFile(filePath)
                .setType(typeName)
                .build()
        )
    }

    @Test
    fun `produce simple composite rules for (range)`() {
        val option = Option
            .newBuilder()
            .setName("range")
            .setNumber(OptionsProto.range.number)
            .setValue(AnyPacker.pack(StringValue.of("[0..100]")))
            .setType(primitive(TYPE_STRING))
            .build()
        blackBox.receivesExternalEvent(
            FieldOptionDiscovered
                .newBuilder()
                .setField(fieldName)
                .setType(typeName)
                .setFile(filePath)
                .setOption(option)
                .build()
        )
        blackBox.assertEvent(CompositeRuleAdded::class.java)
            .comparingExpectedFieldsOnly()
            .isEqualTo(
                CompositeRuleAdded.newBuilder()
                    .setType(typeName)
                    .setRule(CompositeRule.newBuilder()
                        .setLeft(incompleteRuleWith(GREATER_OR_EQUAL))
                        .setRight(incompleteRuleWith(LESS_OR_EQUAL)))
                    .build()
            )
        blackBox.assertEvents()
            .withType(SimpleRuleAdded::class.java)
            .isEmpty()
    }
}

private fun incompleteRuleWith(sign: ComparisonOperator): Rule {
    val simple = SimpleRule
        .newBuilder()
        .setOperator(sign)
    return Rule.newBuilder()
        .setSimple(simple)
        .build()
}

private fun primitive(type: PrimitiveType): Type {
    return Type.newBuilder()
        .setPrimitive(type)
        .build()
}
