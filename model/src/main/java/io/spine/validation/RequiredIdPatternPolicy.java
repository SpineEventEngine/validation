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

import com.google.common.collect.ImmutableList;
import io.spine.core.External;
import io.spine.protodata.Field;
import io.spine.protodata.FilePath;
import io.spine.protodata.TypeExited;
import io.spine.protodata.TypeName;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.tools.mc.java.codegen.FilePattern;

import static io.spine.validation.SourceFiles.findFirstField;

/**
 * A policy that marks ID fields in entity state messages and signal messages as required.
 *
 * <p>The messages are discovered via the file patterns, specified in {@link ValidationConfig}.
 * If ProtoData runs with no config, this policy never produces any validation rules.
 *
 * <p>This policy has a sister—{@link RequiredIdOptionPolicy}. They both implement the required ID
 * constraint. However, this policy finds the ID fields by the names of files that declare
 * the messages with ID fields, and the other—by a list of message options.
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
        ValidationConfig config = configAs(ValidationConfig.class);
        MessageMakers markers = config.getMessageMarkers();
        ImmutableList<FilePattern> filePatterns = allPatterns(markers);
        FilePath file = event.getFile();
        boolean match = filePatterns.stream()
                                    .anyMatch(pattern -> matches(file, pattern));
        if (!match) {
            return withNothing();
        }
        TypeName type = event.getType();
        Field field = findFirstField(type, file, this);
        return withField(field);
    }

    private static ImmutableList<FilePattern> allPatterns(MessageMakers markers) {
        return ImmutableList.<FilePattern>builder()
                .addAll(markers.getEntityPatternList())
                .addAll(markers.getEventPatternList())
                .addAll(markers.getCommandPatternList())
                .addAll(markers.getRejectionPatternList())
                .build();
    }

    private static boolean matches(FilePath path, FilePattern pattern) {
        String filePath = path.getValue();
        switch (pattern.getValueCase()) {
            case SUFFIX:
                return filePath.endsWith(pattern.getSuffix());
            case PREFIX:
                return filePath.startsWith(pattern.getPrefix());
            case REGEX:
                return filePath.matches(pattern.getRegex());
            case VALUE_NOT_SET:
            default:
                return false;
        }
    }
}
