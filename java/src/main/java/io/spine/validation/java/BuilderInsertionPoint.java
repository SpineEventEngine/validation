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

import io.spine.protodata.TypeName;
import io.spine.protodata.renderer.InsertionPoint;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract base for insertion points for generated code implementing
 * {@link io.spine.validate.ValidatingBuilder ValidatingBuilder} interface.
 */
public abstract class BuilderInsertionPoint implements InsertionPoint {

    private static final String BUILDER_CLASS = "Builder";

    /**
     * Cached results of parsing the Java source code.
     *
     * <p>Without caching, this operation may be executed for too many times
     * for the same input.
     */
    private static final ParsedSources parsedSources = new ParsedSources();

    private final TypeName messageType;

    BuilderInsertionPoint(TypeName messageType) {
        this.messageType = checkNotNull(messageType);
    }

    protected final TypeName messageType() {
        return messageType;
    }

    protected final @Nullable JavaClassSource findBuilder(String code) {
        var classSource = findClass(code);
        if (classSource == null) {
            return null;
        }
        if (!classSource.hasNestedType(BUILDER_CLASS)) {
            return null;
        }
        var builder = classSource.getNestedType(BUILDER_CLASS);
        if (!builder.isClass()) {
            return null;
        }
        var builderClass = (JavaClassSource) builder;
        return builderClass;
    }

    private @Nullable JavaClassSource findClass(String code) {
        var javaSource = parseSource(code);
        if (!javaSource.isClass()) {
            return null;
        }
        var source = (JavaClassSource) javaSource;
        Deque<String> names = new ArrayDeque<>(messageType.getNestingTypeNameList());
        names.addLast(messageType.getSimpleName());

        if (source.getName().equals(names.peek())) {
            names.poll();
        }
        var result = findSubClass(source, names);
        return result;
    }

    private static JavaSource<?> parseSource(String code) {
        return parsedSources.get(code);
    }

    private static
    @Nullable JavaClassSource findSubClass(JavaClassSource topLevelClass, Iterable<String> names) {
        var source = topLevelClass;
        for (var name : names) {
            if (!source.hasNestedType(name)) {
                return null;
            }
            var nestedType = source.getNestedType(name);
            if (!nestedType.isClass()) {
                return null;
            }
            source = (JavaClassSource) nestedType;
        }
        return source;
    }
}
