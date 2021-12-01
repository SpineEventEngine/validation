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
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.tools.mc.java.codegen.FilePattern;
import io.spine.validation.event.SimpleRuleAdded;

import java.util.Optional;

import static io.spine.validation.SourceFiles.findFirstField;
import static java.lang.String.format;

final class RequiredIdPolicy extends Policy<TypeExited> {

    @Override
    @React
    @SuppressWarnings("OptionalIsPresent") // For better readability.
    protected EitherOf2<SimpleRuleAdded, Nothing> whenever(@External TypeExited event) {
        if (!configIsPresent()) {
            return EitherOf2.withB(nothing());
        }
        ValidationConfig config = configAs(ValidationConfig.class);
        MessageMakers markers = config.getMessageMarkers();
        ImmutableList<FilePattern> filePatterns = allPatterns(markers);
        FilePath file = event.getFile();
        boolean match = filePatterns.stream()
                                    .anyMatch(pattern -> matches(file, pattern));
        if (!match) {
            return EitherOf2.withB(nothing());
        }
        TypeName type = event.getType();
        Field field = findFirstField(type, file, this);
        String errorMessage = format("ID field `%s` must be set.", field.getName()
                                                                        .getValue());
        Optional<SimpleRule> rule = RequiredRule.forField(field, errorMessage);
        if (!rule.isPresent()) {
            return EitherOf2.withB(nothing());
        }
        return EitherOf2.withA(SimpleRuleAdded.newBuilder()
                                       .setType(type)
                                       .setRule(rule.get())
                                       .build());
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
