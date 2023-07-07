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
import io.spine.protodata.TypeName;
import io.spine.text.Text;
import io.spine.text.TextCoordinates;

import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.UNICODE_CASE;

/**
 * An insertion point at the place where Java validation code should be inserted.
 *
 * <p>Points at a line in the {@code Builder.build()} method right before the return statement.
 */
@Immutable
final class ValidateBeforeReturn extends BuilderInsertionPoint {

    private static final Pattern RETURN_LINE = Pattern.compile(
            "\\s*return .+;.*", UNICODE_CASE | DOTALL
    );

    ValidateBeforeReturn(TypeName type, TypeSystem typeSystem) {
        super(type, typeSystem);
    }

    @Override
    public String getLabel() {
        return format("validate:%s", messageType().getTypeUrl());
    }

    @Override
    public TextCoordinates locateOccurrence(Text text) {
        if (!containsMessageType(text)) {
            return nowhere();
        }
        var maybeCoords = findMethodCoordinates(text, buildMethod(messageType()));
        if (maybeCoords.isEmpty()) {
            return nowhere();
        }
        var coords = maybeCoords.get();

        var bodyStart = coords.getBodyStartLine();
        var bodyEnd = coords.getBodyEndLine();
        var lines = text.lines().subList(bodyStart, bodyEnd);
        var returnIndex = returnLineIndex(lines);
        var returnLineNumber = bodyStart + returnIndex;
        return atLine(returnLineNumber);
    }

    private static int returnLineIndex(List<String> lines) {
        var returnIndex = 0;
        for (var line : lines) {
            if (RETURN_LINE.matcher(line).matches()) {
                return returnIndex;
            }
            returnIndex++;
        }
        throw new IllegalArgumentException("No return statement.");
    }
}
