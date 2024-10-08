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

package io.spine.validation;

import io.spine.protodata.ast.Field;
import io.spine.protodata.ast.FieldName;
import io.spine.protodata.ast.File;
import io.spine.protodata.ast.MessageType;
import io.spine.protodata.ast.ProtobufSourceFile;
import io.spine.protodata.ast.TypeName;
import io.spine.server.query.Querying;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Utilities for working with {@link ProtobufSourceFile}s.
 */
final class SourceFiles {

    /**
     * Prevents the utility class instantiation.
     */
    private SourceFiles() {
    }

    /**
     * Looks up a field by its name, the type name, and the file path.
     *
     * @param fieldName
     *         name of the field
     * @param typeName
     *         name of the type which declares the field
     * @param file
     *         path to the Protobuf file which declares the message which declares the field
     * @param querying
     *         the ProtoData component which conducts the search
     * @return the field
     */
    static Field findField(FieldName fieldName,
                           TypeName typeName,
                           File file,
                           Querying querying) {
        var type = findType(typeName, file, querying);
        var field = type.getFieldList().stream()
                .filter(f -> fieldName.equals(f.getName()))
                .findFirst();
        var foundField = field.orElseGet(() -> type.getOneofGroupList().stream()
                .flatMap(g -> g.getFieldList().stream())
                .filter(f -> fieldName.equals(f.getName()))
                .findFirst()
                .orElseThrow(() -> unknownField(fieldName)));
        return foundField;
    }

    /**
     * Looks up a message type by its name.
     *
     *  @param typeName
     *         name of the type
     * @param file
     *         path to the Protobuf file which declares the message
     * @param querying
     *         the ProtoData component which conducts the search
     * @return the type
     */
    private static MessageType findType(TypeName typeName, File file, Querying querying) {
        var sourceFile = querying.select(ProtobufSourceFile.class)
                           .findById(file);
        if (sourceFile == null) {
            throw unknownFile(file);
        }
        var typeUrl = typeName.getTypeUrl();
        var type = sourceFile.getTypeMap()
                       .get(typeUrl);
        if (type == null) {
            throw unknownType(typeName);
        }
        return type;
    }

    private static IllegalArgumentException unknownField(FieldName name) {
        return newIllegalArgumentException(
                "Unknown field `%s`.", name.getValue()
        );
    }

    private static IllegalArgumentException unknownType(TypeName name) {
        return newIllegalArgumentException(
                "Unknown type `%s`.", name.getTypeUrl()
        );
    }

    private static IllegalArgumentException unknownFile(File file) {
        return newIllegalArgumentException(
                "Unknown file `%s`.", file.getPath()
        );
    }
}
