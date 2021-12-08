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

import com.google.common.collect.ImmutableSet;
import io.spine.core.External;
import io.spine.protodata.Field;
import io.spine.protodata.MessageType;
import io.spine.protodata.Option;
import io.spine.protodata.TypeExited;
import io.spine.protodata.TypeName;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;

import java.util.Set;

import static io.spine.protodata.Ast.typeUrl;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validation.SourceFiles.findType;

final class RequiredIdOptionPolicy extends RequiredIdPolicy {

    @Override
    @React
    protected EitherOf2<RuleAdded, Nothing> whenever(@External TypeExited event) {
        if (!configIsPresent()) {
            return withNothing();
        }
        Set<String> options = options();
        if (options.isEmpty()) {
            return withNothing();
        }
        TypeName typeName = event.getType();
        MessageType type = findType(typeName, event.getFile(), this);
        boolean optionMatches = type.getOptionList()
                                    .stream()
                                    .map(Option::getName)
                                    .anyMatch(options::contains);
        if (!optionMatches) {
            return withNothing();
        }
        if (type.getFieldCount() == 0) {
            throw newIllegalStateException(
                    "Entity type `%s` must have at least one field.", typeUrl(typeName)
            );
        }
        Field field = type.getField(0);
        return withField(typeName, field);
    }

    private Set<String> options() {
        ValidationConfig config = configAs(ValidationConfig.class);
        MessageMakers markers = config.getMessageMarkers();
        Set<String> options = ImmutableSet.copyOf(markers.getEntityOptionNameList());
        return options;
    }
}
