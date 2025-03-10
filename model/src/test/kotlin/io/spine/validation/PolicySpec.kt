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

package io.spine.validation

import io.spine.option.OptionsProto
import io.spine.option.rangeOption
import io.spine.protobuf.pack
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.File
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.ProtoFileHeader.SyntaxVersion.PROTO3
import io.spine.protodata.ast.event.fieldEntered
import io.spine.protodata.ast.event.fieldOptionDiscovered
import io.spine.protodata.ast.event.fileEntered
import io.spine.protodata.ast.event.typeDiscovered
import io.spine.protodata.ast.field
import io.spine.protodata.ast.fieldName
import io.spine.protodata.ast.fieldType
import io.spine.protodata.ast.file
import io.spine.protodata.ast.messageType
import io.spine.protodata.ast.option
import io.spine.protodata.ast.protoFileHeader
import io.spine.protodata.ast.type
import io.spine.protodata.ast.typeName
import io.spine.protodata.backend.CodeGenerationContext
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.plugin.applyTo
import io.spine.protodata.protobuf.ProtoFileList
import io.spine.protodata.type.TypeSystem
import io.spine.testing.server.blackbox.BlackBox
import io.spine.validation.ComparisonOperator.GREATER_OR_EQUAL
import io.spine.validation.ComparisonOperator.LESS_OR_EQUAL
import io.spine.validation.event.CompositeRuleAdded
import io.spine.validation.event.SimpleRuleAdded
import io.spine.validation.event.compositeRuleAdded
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Validation policies should")
internal class PolicySpec {

    private lateinit var codegenContext: CodeGenerationContext
    private lateinit var blackBox: BlackBox
    private val filePath: File = file {
        path = "example/bar.proto"
    }
    private val typeName = typeName {
        typeUrlPrefix = "type.example.org"
        packageName = "example.test"
        simpleName = "Bar"
    }
    private val fieldName = fieldName {
        value = "foo"
    }

    @BeforeEach
    fun prepareBlackBox() {
        val typeSystem = TypeSystem(
            ProtoFileList(emptyList()),
            emptySet()
        )
        val plugin = object : ValidationPlugin() {}
        codegenContext = CodeGenerationContext(Pipeline.generateId(), typeSystem) {
            // Mimic what a `Pipeline` does to its plugins.
            plugin.applyTo(this, typeSystem)
        }

        blackBox = BlackBox.from(codegenContext.context)

        val protoFileHeader = protoFileHeader {
            file = filePath
            packageName = typeName.packageName
            syntax = PROTO3
        }
        val messageType = messageType {
            name = typeName
            file = filePath
        }
        val int32Field = field {
            declaringType = typeName
            name = fieldName
            type = primitive(TYPE_INT32)
        }

        blackBox.receivesExternalEvents(
            fileEntered { file = filePath; header = protoFileHeader },
            typeDiscovered { file = filePath; type = messageType },
            fieldEntered { field = int32Field; file = filePath; type = typeName }
        )
    }

    @AfterEach
    fun closeBlackBox() {
        codegenContext.close()
        blackBox.close()
    }
    
    @Test
    fun `produce simple composite rules for (range)`() {
        val rangeOption = option {
            name = "range"
            number = OptionsProto.range.number
            value = rangeOption { value = "[0..100]" }.pack()
            type = type {
                primitive = TYPE_STRING
            }
        }

        blackBox.receivesExternalEvent(
            fieldOptionDiscovered {
                file = this@PolicySpec.filePath
                option = rangeOption
                subject = field {
                    declaringType = typeName
                    name = fieldName
                    type = primitive(TYPE_INT32)
                }
            }
        )
        blackBox.assertEvent(CompositeRuleAdded::class.java)
            .comparingExpectedFieldsOnly()
            .isEqualTo(
                compositeRuleAdded {
                    type = typeName
                    rule = compositeRule {
                        left = incompleteRuleWith(GREATER_OR_EQUAL)
                        right = incompleteRuleWith(LESS_OR_EQUAL)
                    }
                }
            )
        blackBox.assertEvents()
            .withType(SimpleRuleAdded::class.java)
            .isEmpty()
    }
}

private fun incompleteRuleWith(sign: ComparisonOperator): Rule =
    rule {
        simple = simpleRule { operator = sign }
    }

private fun primitive(type: PrimitiveType): FieldType =
    fieldType {
        primitive = type
    }
