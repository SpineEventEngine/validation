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

import io.spine.core.External;
import io.spine.protodata.File;
import io.spine.protodata.event.TypeExited;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.RuleAdded;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.validation.Markers.allPatterns;
import static io.spine.validation.SourceFiles.findFirstField;

/**
 * A policy that marks ID fields in entity state messages and signal messages as required.
 *
 * <p>The messages are discovered via the file patterns, specified in {@link ValidationConfig}.
 * If ProtoData runs with no config, this policy never produces any validation rules.
 *
 * <p>This policy has a sister—{@link RequiredIdOptionPolicy}. They both implement the required ID
 * constraint. However, this policy looks for the ID fields in messages that are defined in files
 * matching certain path patterns, and the other—in messages marked with certain options.
 *
 * @see RequiredIdOptionPolicy
 */
final class RequiredIdPatternPolicy extends RequiredIdPolicy {

    @Override
    @React
    protected EitherOf2<RuleAdded, Nothing> whenever(@External TypeExited event) {
        if (!configIsPresent()) {
            return withNothing();
        }
        var config = configAs(ValidationConfig.class);
        var markers = config.getMessageMarkers();
        var filePatterns = allPatterns(markers);
        var file = event.getFile();
        var match = filePatterns.stream()
                .anyMatch(pattern -> matches(file, pattern));
        if (!match) {
            return withNothing();
        }
        var type = event.getType();
        var field = findFirstField(type, file, this);
        return withField(field);
    }

    private static boolean matches(File path, FilePattern pattern) {
        var filePath = path.getPath();
        var kind = pattern.getKindCase();
        checkNotNull(kind, "File pattern has unknown kind: %s.", pattern);
        switch (kind) {
            case SUFFIX:
                return filePath.endsWith(pattern.getSuffix());
            case PREFIX:
                return filePath.startsWith(pattern.getPrefix());
            case REGEX:
                return filePath.matches(pattern.getRegex());
            case KIND_NOT_SET:
            default:
                return false;
        }
    }
}
