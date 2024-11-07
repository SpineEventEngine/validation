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

package io.spine.validation.java;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.spine.protodata.ast.MessageType;
import io.spine.protodata.ast.TypeName;
import io.spine.server.query.QueryingClient;
import io.spine.validation.MessageValidation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * Maps a message type map to corresponding validation rules.
 */
final class Validations {

    private final ImmutableMap<TypeName, MessageValidation> map;

    /**
     * Creates a new instance taking all the validations obtained from the passed client.
     */
    Validations(QueryingClient<MessageValidation> client) {
        this.map = checkNotNull(client).all()
                .stream()
                .collect(toImmutableMap(v -> v.getType().getName(), v -> v));
    }

    /**
     * Obtains validation for the given type.
     *
     * <p>If there are no validation rules specified for this type, the method returns
     * an instance which refers to the type, containing no validation rules.
     */
    MessageValidation get(MessageType type) {
        @Nullable MessageValidation validation = map.get(type.getName());
        if (validation == null) {
            return MessageValidation.newBuilder()
                    .setName(type.getName())
                    .setType(type)
                    .build();
        }
        return validation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (Validations) o;
        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("map", map)
                          .toString();
    }
}
