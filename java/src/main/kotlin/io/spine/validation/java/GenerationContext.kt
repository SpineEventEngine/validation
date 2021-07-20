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

import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.FilePath
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Querying
import io.spine.protodata.TypeName
import io.spine.protodata.codegen.java.MessageReference
import io.spine.protodata.select
import io.spine.protodata.typeUrl
import io.spine.validation.Rule

/**
 * Context of a [JavaCodeGenerator].
 */
internal data class GenerationContext(

    /**
     * The rule for which the code is generated.
     */
    val rule: Rule,

    /**
     * A reference to the validated message.
     */
    val msg: MessageReference,

    /**
     * The path to the Protobuf file where the validated type is declared.
     */
    val protoFile: FilePath,

    /**
     * The Protobuf types known to the application.
     */
    val typeSystem: TypeSystem,

    /**
     * The type of the validated message.
     */
    val declaringType: TypeName,

    /**
     * A reference to the mutable violations list, which accumulates all the constraint violations.
     */
    val violationsList: String,

    /**
     * A [Querying] ProtoData component.
     */
    private val querying: Querying
) {

    /**
     * The field associated with the given rule.
     *
     * If the [rule] is not a simple rule, the value is absent.
     */
    val fieldFromSimpleRule: Field?
        get() = if (rule.hasSimple()) {
            lookupField(rule.simple.field)
        } else {
            null
        }

    /**
     * Obtains the same context but with the given validation [rule].
     */
    fun withRule(rule: Rule): GenerationContext = copy(rule = rule)

    /**
     * Finds the field by the given name in the validated type.
     *
     * @throws IllegalArgumentException if there is no such field
     */
    fun lookupField(name: FieldName): Field =
        querying.lookUpField(protoFile, declaringType, name)
}

private fun Querying.lookUpField(file: FilePath, type: TypeName, field: FieldName): Field {
    val protoFile = select<ProtobufSourceFile>().withId(file).orElseThrow {
        IllegalArgumentException("Unknown file: `${file.value}`.")
    }
    val messageType = protoFile.typeMap[type.typeUrl()]
        ?: throw IllegalArgumentException("Unknown type: `${type.typeUrl()}`.")
    return messageType.findField(field)
        ?: throw IllegalArgumentException("Unknown field: `${type.typeUrl()}.${field.value}`.")
}

private fun MessageType.findField(name: FieldName): Field? {
    var field = fieldList.find { it.name == name }
    if (field == null) {
        field = oneofGroupList
            .flatMap { group -> group.fieldList }
            .find { it.name == name }
    }
    return field
}
