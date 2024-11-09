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

package io.spine.validation

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.File
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.ProtobufSourceFile
import io.spine.protodata.ast.TypeName
import io.spine.server.query.Querying
import io.spine.server.query.select

/**
 * Looks up a field by its name, the type name, and the file path.
 *
 * @param fieldName The name of the field.
 * @param typeName The name of the type which declares the field.
 * @param file The path to the Protobuf file which declares the message which declares the field.
 * @return the field.
 */
internal fun Querying.findField(
    fieldName: FieldName,
    typeName: TypeName,
    file: File
): Field {
    val type = findType(typeName, file)
    val field = type.fieldList.firstOrNull { f -> fieldName == f.name }
    val foundField = field ?: run {
        type.oneofGroupList
            .flatMap { g -> g.fieldList }
            .firstOrNull { f -> fieldName == f.name }
            ?: unknownField(fieldName)
    }
    return foundField
}

/**
 * Looks up a message type by its name.
 *
 * @param typeName The name of the type.
 * @param file The path to the Protobuf file which declares the message.
 * @return the type.
 */
private fun Querying.findType(typeName: TypeName, file: File): MessageType {
    val sourceFile = select<ProtobufSourceFile>().findById(file)
        ?: unknownFile(file)
    val typeUrl = typeName.typeUrl
    val type = sourceFile.typeMap[typeUrl]
        ?: unknownType(typeName)
    return type
}

private fun unknownField(name: FieldName): Nothing = error("Unknown field `${name.value}`.")

private fun unknownType(name: TypeName): Nothing = error("Unknown type `${name.typeUrl}`.")

private fun unknownFile(file: File): Nothing = error("Unknown file `${file.path}`.")
