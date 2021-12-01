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
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.SimpleRuleAdded;

import java.util.Optional;
import java.util.Set;

import static io.spine.protodata.Ast.typeUrl;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validation.SourceFiles.findType;
import static java.lang.String.format;

final class RequiredEntityIdPolicy extends Policy<TypeExited> {

    @Override
    @React
    @SuppressWarnings("OptionalIsPresent") // For better readability.
    protected EitherOf2<SimpleRuleAdded, Nothing> whenever(@External TypeExited event) {
        if (!configIsPresent()) {
            return EitherOf2.withB(nothing());
        }
        Set<String> options = options();
        if (options.isEmpty()) {
            return EitherOf2.withB(nothing());
        }
        TypeName typeName = event.getType();
        MessageType type = findType(typeName, event.getFile(), this);
        boolean optionMatches = type.getOptionList()
                                    .stream()
                                    .map(Option::getName)
                                    .anyMatch(options::contains);
        if (!optionMatches) {
            return EitherOf2.withB(nothing());
        }
        if (type.getFieldCount() == 0) {
            throw newIllegalStateException(
                    "Entity type `%s` must have at least one field.", typeUrl(typeName)
            );
        }
        Field field = type.getField(0);
        String errorMessage = format("Entity ID field `%s` must be set.", field.getName()
                                                                               .getValue());
        Optional<SimpleRule> rule = RequiredRule.forField(field, errorMessage);
        if (!rule.isPresent()) {
            return EitherOf2.withB(nothing());
        }
        return EitherOf2.withA(SimpleRuleAdded.newBuilder()
                                       .setType(typeName)
                                       .setRule(rule.get())
                                       .build());
    }

    private Set<String> options() {
        ValidationConfig config = configAs(ValidationConfig.class);
        MessageMakers markers = config.getMessageMarkers();
        Set<String> options = ImmutableSet.copyOf(markers.getEntityOptionNameList());
        return options;
    }
}
