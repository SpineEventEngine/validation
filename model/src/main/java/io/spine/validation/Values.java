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
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import java.util.List;
import java.util.Map;

import static io.spine.protobuf.Messages.isNotDefault;
import static io.spine.protodata.Ast.name;
import static io.spine.validation.NullValue.NULL_VALUE;

/**
 * A factory of {@code Value}s.
 */
public final class Values {

    /**
     * Prevents the utility class instantiation.
     */
    private Values() {
    }

    /**
     * Converts the given message into a value.
     *
     * <p>If the message is equal to the default instance, it will be represented by
     * a {@code MessageValue} with no fields. Otherwise, all the present fields are converted
     * into {@code Value}s.
     */
    public static Value from(Message message) {
        MessageValue.Builder builder = MessageValue.newBuilder()
                .setType(name(message.getDescriptorForType()));
        if (isNotDefault(message)) {
            populate(builder, message);
        }
        return Value.newBuilder()
                .setMessageValue(builder.build())
                .build();
    }

    private static void populate(MessageValue.Builder value, Message source) {
        source.getAllFields().forEach((k, v) -> value.putFields(k.getName(), fromField(k, v)));
    }

    private static Value fromField(FieldDescriptor field, Object value) {
        if (field.isMapField()) {
            return fromMap(field, value);
        }
        if (field.isRepeated()) {
            return fromList(field, value);
        }
        switch (field.getJavaType()) {
            case INT:
            case LONG:
                return Value.newBuilder()
                        .setIntValue(((long) value))
                        .build();
            case FLOAT:
            case DOUBLE:
                return Value.newBuilder()
                        .setDoubleValue(((double) value))
                        .build();
            case BOOLEAN:
                return Value.newBuilder()
                        .setBoolValue(((boolean) value))
                        .build();
            case STRING:
                return Value.newBuilder()
                        .setStringValue(((String) value))
                        .build();
            case BYTE_STRING:
                return Value.newBuilder()
                        .setBytesValue(((ByteString) value))
                        .build();
            case ENUM:
                EnumValueDescriptor enumDescriptor = (EnumValueDescriptor) value;
                @SuppressWarnings("KotlinInternalInJava")
                EnumValue enumValue = EnumValue.newBuilder()
                        .setType(name(field.getEnumType()))
                        .setConstNumber(enumDescriptor.getNumber())
                        .build();
                return Value.newBuilder()
                        .setEnumValue(enumValue)
                        .build();
            case MESSAGE:
                return from(((Message) value));
            default:
                return Value.newBuilder()
                        .setNullValue(NULL_VALUE)
                        .build();
        }
    }

    private static Value fromMap(FieldDescriptor field, Object value) {
        List<FieldDescriptor> syntheticEntry = field.getMessageType()
                                            .getFields();
        FieldDescriptor keyType = syntheticEntry.get(0);
        FieldDescriptor valueType = syntheticEntry.get(1);
        Map<?, ?> map = (Map<?, ?>) value;
        MapValue.Builder mapBuilder = MapValue.newBuilder();
        map.forEach((k, v) -> {
            Value key = fromField(keyType, k);
            Value val = fromField(valueType, v);
            mapBuilder.addValue(MapValue.Entry
                                        .newBuilder()
                                        .setKey(key)
                                        .setValue(val)
                                        .build());
        });
        return Value.newBuilder()
                .setMapValue(mapBuilder)
                .build();
    }

    private static Value fromList(FieldDescriptor field, Object value) {
        List<?> values = (List<?>) value;
        ListValue.Builder listBuilder = values
                .stream()
                .map(entry -> fromField(field, entry))
                .collect(ListValue::newBuilder,
                         ListValue.Builder::addValues,
                         (l, r) -> l.addAllValues(r.getValuesList()));
        return Value.newBuilder()
                .setListValue(listBuilder)
                .build();
    }
}
