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

import io.spine.protodata.Field;
import io.spine.protodata.FieldName;
import io.spine.protodata.FilePath;
import io.spine.protodata.MessageType;
import io.spine.protodata.ProtobufSourceFile;
import io.spine.protodata.TypeName;
import io.spine.server.query.Querying;

import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;

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
     * @param filePath
     *         path to the Protobuf file which declares the message which declares the field
     * @param querying
     *         the ProtoData component which conducts the search
     * @return the field
     */
    static Field findField(FieldName fieldName,
                           TypeName typeName,
                           FilePath filePath,
                           Querying querying) {
        var type = findType(typeName, filePath, querying);
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
     * Looks up the first field in a type.
     *
     * <p>The order of declaration is used to look for the fields, not the field numbers.
     *
     *  @param typeName
     *         name of the type which declares the field
     * @param filePath
     *         path to the Protobuf file which declares the message which declares the field
     * @param querying
     *         the ProtoData component which conducts the search
     * @return the first field of the type
     */
    static Field findFirstField(TypeName typeName, FilePath filePath, Querying querying) {
        var type = findType(typeName, filePath, querying);
        if (type.getFieldCount() == 0) {
            var url = typeName.getTypeUrl();
            throw newIllegalStateException("Type `%s` must have at least one field.", url);
        }
        var field = type.getField(0);
        return field;
    }

    /**
     * Looks up a message type by its name.
     *
     *  @param typeName
     *         name of the type
     * @param filePath
     *         path to the Protobuf file which declares the message
     * @param querying
     *         the ProtoData component which conducts the search
     * @return the type
     */
    static MessageType findType(TypeName typeName, FilePath filePath, Querying querying) {
        var file = querying.select(ProtobufSourceFile.class)
                           .findById(filePath);
        if (file == null) {
            throw unknownFile(filePath);
        }
        var typeUrl = typeName.getTypeUrl();
        var type = file.getTypeMap()
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

    private static IllegalArgumentException unknownFile(FilePath filePath) {
        return newIllegalArgumentException(
                "Unknown file `%s`.", filePath.getValue()
        );
    }
}
