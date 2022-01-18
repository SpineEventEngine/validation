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

import com.google.common.collect.ImmutableSet;
import io.spine.core.External;
import io.spine.protodata.Option;
import io.spine.protodata.TypeExited;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;

import java.util.Set;

import static io.spine.protodata.Ast.typeUrl;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validation.SourceFiles.findType;

/**
 * A policy that marks ID fields in entity state messages as required.
 *
 * <p>The entity state messages are discovered via the options, specified
 * in {@link ValidationConfig}. If ProtoData runs with no config, this policy never produces any
 * validation rules.
 *
 * <p>This policy has a sister—{@link RequiredIdPatternPolicy}. They both implement the required ID
 * constraint. However, this policy looks for the ID fields in messages with certain options,
 * and the other—in the messages declared in files that match certain path patterns.
 *
 * @see RequiredIdPatternPolicy
 */
final class RequiredIdOptionPolicy extends RequiredIdPolicy {

    @Override
    @React
    protected EitherOf2<RuleAdded, Nothing> whenever(@External TypeExited event) {
        if (!configIsPresent()) {
            return withNothing();
        }
        var options = options();
        if (options.isEmpty()) {
            return withNothing();
        }
        var typeName = event.getType();
        var type = findType(typeName, event.getFile(), this);
        var optionMatches = type.getOptionList()
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
        var field = type.getField(0);
        return withField(field);
    }

    private Set<String> options() {
        var config = configAs(ValidationConfig.class);
        var markers = config.getMessageMarkers();
        Set<String> options = ImmutableSet.copyOf(markers.getEntityOptionNameList());
        return options;
    }
}
