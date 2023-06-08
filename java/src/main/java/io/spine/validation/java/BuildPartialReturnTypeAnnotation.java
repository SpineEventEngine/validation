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
import io.spine.protodata.renderer.LineNumber;
import io.spine.text.Text;
import io.spine.text.TextCoordinates;

import java.util.List;

import static java.lang.String.format;

/**
 * Locates the placement for annotating the type returned by
 * the {@code Builder.buildPartial()} method.
 */
@Immutable
final class BuildPartialReturnTypeAnnotation extends BuilderInsertionPoint {

    BuildPartialReturnTypeAnnotation(TypeName messageType) {
        super(messageType);
    }

    @Override
    public String getLabel() {
        return format("buildPartial:%s", messageType().getTypeUrl());
    }

    @Override
    public TextCoordinates locateOccurrence(Text text) {
        var method = findMethod(text, BUILD_PARTIAL_METHOD);
        if (method == null) {
            return nowhere();
        }
        var methodDeclarationLine = method.getLineNumber();
        //TODO:2022-10-09:alexander.yevsyukov: We should return a placement inside the line
        // after the package name and before the type name.
        // See https://github.com/SpineEventEngine/ProtoData/issues/84
        return atLine(methodDeclarationLine);
    }
}
