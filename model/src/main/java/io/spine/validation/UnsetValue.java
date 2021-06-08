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

package io.spine.validation;

import com.google.protobuf.ByteString;
import io.spine.protodata.Field;
import io.spine.protodata.PrimitiveType;
import io.spine.protodata.Type;
import io.spine.protodata.Type.KindCase;
import io.spine.protodata.TypeName;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.PrimitiveType.PT_UNKNOWN;
import static io.spine.protodata.PrimitiveType.TYPE_BYTES;
import static io.spine.protodata.PrimitiveType.TYPE_STRING;
import static io.spine.protodata.PrimitiveType.UNRECOGNIZED;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A factory of {@link Value}s representing default states of Protobuf message fields.
 *
 * <p>This class does not instantiate default values of Protobuf messages. It merely creates
 * instances of {@link Value} which represent values of Protobuf message fields, which are not set.
 */
public final class UnsetValue {

    /**
     * Prevents the utility class instantiation.
     */
    private UnsetValue() {
    }

    /**
     * Obtains an unset value for the given field.
     *
     * <p>If a field is a number or a {@code bool}, it is impossible to tell if it's set or not.
     * In the binary representation, the {@code 0} and {@code false} values may either be explicitly
     * set or just be the default values. For these cases, and only for these cases, the method
     * returns {@code Optional.empty()}.
     *
     *
     * @return a {@link Value} with the field's default value or {@code Optional.empty()} if
     *         the field does not have an easily distinguished not-set value
     */
    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // Covered by `default`.
    public static Optional<Value> forField(Field field) {
        checkNotNull(field);
        switch (field.getCardinalityCase()) {
            case LIST:
                return Optional.of(Value.newBuilder()
                                        .setListValue(ListValue.getDefaultInstance())
                                        .vBuild());
            case MAP:
                return Optional.of(Value.newBuilder()
                                        .setMapValue(MapValue.getDefaultInstance())
                                        .vBuild());
            default:
                Type type = field.getType();
                return defaultValue(type);
        }
    }

    private static Optional<Value> defaultValue(Type type) {
        KindCase kind = type.getKindCase();
        switch (kind) {
            case MESSAGE:
                return Optional.of(messageValue(type));
            case ENUMERATION:
                return Optional.of(enumValue(type));
            case PRIMITIVE:
                return primitiveValue(type);
            case KIND_NOT_SET:
            default:
                throw new IllegalArgumentException("Field type unknown.");
        }
    }

    private static Optional<Value> primitiveValue(Type type) {
        PrimitiveType primitiveType = type.getPrimitive();
        if (primitiveType == PT_UNKNOWN || primitiveType == UNRECOGNIZED) {
            throw newIllegalArgumentException("Unknown type `%s`.", primitiveType);
        }
        if (primitiveType == TYPE_STRING) {
            return Optional.of(Value.newBuilder()
                                    .setStringValue("")
                                    .vBuild());
        }
        if (primitiveType == TYPE_BYTES) {
            return Optional.of(Value.newBuilder()
                                    .setBytesValue(ByteString.EMPTY)
                                    .vBuild());
        }
        return Optional.empty();
    }

    private static Value messageValue(Type type) {
        TypeName msgName = type.getMessage();
        return Value.newBuilder()
                    .setMessageValue(MessageValue.newBuilder()
                                                 .setType(msgName)
                                                 .buildPartial())
                    .vBuild();
    }

    private static Value enumValue(Type type) {
        TypeName enumName = type.getEnumeration();
        return Value.newBuilder()
                    .setEnumValue(EnumValue.newBuilder()
                                           .setType(enumName)
                                           .buildPartial())
                    .vBuild();
    }
}
