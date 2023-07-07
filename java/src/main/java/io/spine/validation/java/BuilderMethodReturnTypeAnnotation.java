/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.regex.Pattern.compile;

/**
 * An insertion point for a type annotation for the return type of a method of a message builder.
 *
 * <p>The insertion point is placed in the spot where the {@code TYPE_USE} annotation can be put.
 * The annotation would mark the return type of a method with the given name.
 *
 * <p>It is assumed that:
 * <ol>
 *     <li>The method has no parameters.
 *     <li>The method is public.
 *     <li>The method returns a value.
 *     <li>The return type is formatted as a fully-qualified name of a class and placed on the same
 *         line with the {@code public} modifier and the method name.
 * </ol>
 */
@Immutable
class BuilderMethodReturnTypeAnnotation extends BuilderInsertionPoint {

    private final Pattern signaturePattern;
    private final MethodSignature method;

    BuilderMethodReturnTypeAnnotation(TypeName messageType,
                                      MethodSignature method,
                                      TypeSystem typeSystem) {
        super(messageType, typeSystem);
        this.method = checkNotNull(method);
        this.signaturePattern = compile(
                "\\s+public\\s+[\\w.]+\\.(\\w+)\\s+" + method.getMethodName()
        );
    }

    @Override
    public String getLabel() {
        return getClass().getSimpleName() + ':' + messageType().getTypeUrl();
    }

    @Override
    public TextCoordinates locateOccurrence(Text text) {
        var maybeCoords = findSignatureCoordinates(text, method);
        if (maybeCoords.isEmpty()) {
            return nowhere();
        }
        var coords = maybeCoords.get();
        var declarationLineIndex = coords.getSignatureStartLine();
        var lines = text.lines();

        Matcher matcher;
        for (var signatureIndex = declarationLineIndex; signatureIndex < lines.size(); signatureIndex++) {
            matcher = signaturePattern.matcher(lines.get(signatureIndex));
            if (matcher.find()) {
                var pointInLine = matcher.start(1);
                return at(signatureIndex, pointInLine);
            }
        }
        return nowhere();
    }
}
