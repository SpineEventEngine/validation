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

import com.google.protobuf.StringValue
import io.spine.option.OptionsProto
import io.spine.protobuf.pack
import io.spine.protodata.File
import io.spine.protodata.ProtoFileHeader.SyntaxVersion.PROTO3
import io.spine.protodata.PrimitiveType
import io.spine.protodata.PrimitiveType.TYPE_INT32
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.Type
import io.spine.protodata.backend.CodeGenerationContext
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.event.fieldEntered
import io.spine.protodata.event.fieldOptionDiscovered
import io.spine.protodata.event.fileEntered
import io.spine.protodata.event.typeEntered
import io.spine.protodata.field
import io.spine.protodata.fieldName
import io.spine.protodata.file
import io.spine.protodata.messageType
import io.spine.protodata.option
import io.spine.protodata.protoFileHeader
import io.spine.protodata.type
import io.spine.protodata.typeName
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
class PolicySpec {

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
        codegenContext = CodeGenerationContext(Pipeline.generateId()) {
            ValidationPlugin().policies().forEach { addEventDispatcher(it) }
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
            typeEntered { file = filePath; type = messageType },
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
            value = StringValue.of("[0..100]").pack()
            type = primitive(TYPE_STRING)
        }

        blackBox.receivesExternalEvent(
            fieldOptionDiscovered {
                field = fieldName
                type = typeName
                file = this@PolicySpec.filePath
                option = rangeOption
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

private fun primitive(type: PrimitiveType): Type =
    type {
        primitive = type
    }
