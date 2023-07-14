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

import com.google.errorprone.annotations.Immutable;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.text.Text;
import io.spine.text.TextCoordinates;
import io.spine.text.TextFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static io.spine.validation.java.BuilderInsertionPoint.BUILD_METHOD;
import static io.spine.validation.java.BuilderInsertionPoint.findBuilders;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.UNICODE_CASE;
import static java.util.stream.Collectors.toSet;

/**
 * An insertion point at the place where Java validation code should be inserted.
 *
 * <p>Points at a line in the {@code Builder.build()} method right before the return statement.
 */
@Immutable
final class ValidateBeforeReturn implements InsertionPoint {

    private static final String LABEL = ValidateBeforeReturn.class.getSimpleName();
    private static final Pattern RETURN_LINE = Pattern.compile(
            "\\s*return .+;.*", UNICODE_CASE | DOTALL
    );

    @Override
    public String getLabel() {
        return LABEL;
    }

    @NonNull
    @Override
    public Set<TextCoordinates> locate(@NonNull Text code) {
        return findBuilders(code)
                .map(b -> b.getMethod(BUILD_METHOD))
                .filter(Objects::nonNull)
                .map(m -> findLine(m, code))
                .collect(toSet());
    }

    private TextCoordinates findLine(MethodSource<?> method, Text code) {
        var methodDeclarationLine = method.getLineNumber();
        var startPosition = method.getStartPosition();
        var endPosition = method.getEndPosition();
        var wholeCode = code.getValue();
        var methodSource = wholeCode.substring(startPosition, endPosition);
        var returnIndex = returnLineIndex(methodSource);
        var returnLineNumber = methodDeclarationLine + returnIndex;
        return atLine(returnLineNumber - 1);
    }
    private static int returnLineIndex(String code) {
        var methodLines = TextFactory.lineSplitter().split(code);
        var returnIndex = 0;
        for (var line : methodLines) {
            if (RETURN_LINE.matcher(line).matches()) {
                return returnIndex;
            }
            returnIndex++;
        }
        throw new IllegalArgumentException("No return statement.");
    }
}
