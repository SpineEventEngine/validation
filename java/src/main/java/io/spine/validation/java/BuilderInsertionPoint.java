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
import io.spine.protodata.Type;
import io.spine.protodata.TypeName;
import io.spine.protodata.renderer.NonRepeatingInsertionPoint;
import io.spine.text.Text;

import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.text.TextFactory.lineJoiner;
import static io.spine.text.TextFactory.lineSplitter;
import static io.spine.text.TextFactory.text;
import static java.util.regex.Pattern.DOTALL;

/**
 * Abstract base for insertion points for generated code implementing
 * {@link io.spine.validate.ValidatingBuilder ValidatingBuilder} interface.
 */
@Immutable
abstract class BuilderInsertionPoint implements NonRepeatingInsertionPoint {

    private final TypeName messageType;
    private final TypeSystem typeSystem;

    BuilderInsertionPoint(TypeName messageType, TypeSystem typeSystem) {
        this.messageType = checkNotNull(messageType);
        this.typeSystem = checkNotNull(typeSystem);
    }

    /**
     * Obtains the name of the message type for which we find insertion point.
     */
    protected final TypeName messageType() {
        return messageType;
    }

    final boolean containsMessageType(Text text) {
        var simpleName = messageType().getSimpleName();
        for (var line : text.lines()) {
            if (line.contains(simpleName)) {
                return true;
            }
        }
        return false;
    }

    static MethodSignature buildMethod(TypeName messageType) {
        var type = Type.newBuilder()
                .setMessage(messageType)
                .build();
        return MethodSignature.newBuilder()
                .setMethodName("build")
                .setReturnType(type)
                .build();
    }

    static MethodSignature buildPartialMethod(TypeName messageType) {
        var type = Type.newBuilder()
                .setMessage(messageType)
                .build();
        return MethodSignature.newBuilder()
                .setMethodName("buildPartial")
                .setReturnType(type)
                .build();
    }

    Optional<FragmentCoordinates> findMethodCoordinates(Text code, MethodSignature signature) {
        var coordinates = findSignatureCoordinates(code, signature);
        return coordinates.map(coods -> {
            var bodyStartLine = coods.getBodyStartLine();
            var bodyEndLine = findMethodEndLine(code, bodyStartLine);
            return coods.toBuilder()
                        .setBodyEndLine(bodyEndLine)
                        .build();
        });
    }

    private static int findMethodEndLine(Text code, int methodStartLine) {
        var lines = code.lines();
        var codeFromLine = lineJoiner().join(lines.subList(methodStartLine, lines.size()));
        var firstBraceIndex = codeFromLine.indexOf('{');
        var braces = 1;
        for (var i = firstBraceIndex + 1; i < codeFromLine.length(); i++) {
            if (codeFromLine.charAt(i) == '{') {
                braces++;
            } else if (codeFromLine.charAt(i) == '}') {
                braces--;
            }
            if (braces == 0) {
                return lineOfIndex(text(codeFromLine), i) + methodStartLine;
            }
        }
        throw new IllegalStateException(
                "Cannot find the end of the method starting at line " + methodStartLine
        );
    }

    final Optional<FragmentCoordinates> findSignatureCoordinates(Text code, MethodSignature signature) {
        var javaTypeName = typeSystem.javaTypeName(signature.getReturnType());
        var methodName = signature.getMethodName();
        var pattern = "public" + "\\s+" + javaTypeName + "\\s+" + methodName + "\\s*?\\(.*?\\)\\s*?\\{";
        var regex = Pattern.compile(pattern, DOTALL);
        var matcher = regex.matcher(code.getValue());
        if (!matcher.find()) {
            return Optional.empty();
        }
        var index = matcher.start();
        var end = matcher.group(0).length() + index;
        return Optional.of(FragmentCoordinates.newBuilder()
                                   .setSignatureStartLine(lineOfIndex(code, index))
                                   .setBodyStartLine(lineOfIndex(code, end))
                                   .build());
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static int lineOfIndex(Text code, int index) {
        var wholeText = code.getValue();
        var beforeIndex = wholeText.substring(0, index);
        return (int) lineSplitter().splitToStream(beforeIndex).count() - 1;
    }
}
