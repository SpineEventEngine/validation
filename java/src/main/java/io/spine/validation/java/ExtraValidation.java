/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.protodata.TypeName;
import io.spine.protodata.renderer.NonRepeatingInsertionPoint;
import io.spine.text.TextCoordinates;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.renderer.CoordinatesFactory.nowhere;
import static java.lang.String.format;

/**
 * An insertion point at the place where users may add some extra validation code to the one already
 * generated by the standard {@link JavaValidationRenderer}.
 *
 * <p>Goes after all the standard validation checks but before returning/throwing
 * the resulting error.
 */
public final class ExtraValidation implements NonRepeatingInsertionPoint {

    private final TypeName name;

    /**
     * Creates a new instance for the given type.
     */
    public ExtraValidation(TypeName name) {
        this.name = checkNotNull(name);
    }

    @Override
    public String getLabel() {
        return format("extra_validation:%s", name.getTypeUrl());
    }

    /**
     * Does not try to locate the point.
     *
     * <p>This insertion point is printed as a part of the code generated by
     * the {@link JavaValidationRenderer}. So we never require locating it in code.
     * Thus, this method always returns {@code LineNumber.notInFile()}.
     */
    @Override
    public TextCoordinates locateOccurrence(String code) {
        return nowhere();
    }
}
