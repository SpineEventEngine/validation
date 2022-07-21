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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.spine.protodata.TypeName;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.protodata.renderer.LineNumber;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.Ast.typeUrl;

/**
 * An insertion point at the place where Java validation code should be inserted.
 *
 * <p>Points at a line in the {@code Builder.build()} method right before the return statement.
 */
final class Validate implements InsertionPoint {

    private static final Splitter LINE_SPLITTER = Splitter.on(System.lineSeparator());
    private static final Joiner LINE_JOINER = Joiner.on(System.lineSeparator());
    private static final Pattern RETURN_LINE = Pattern.compile("\\s*return .+;\\s*");
    private static final String BUILDER_CLASS = "Builder";
    private static final String BUILD_METHOD = "build";

    /**
     * Cached results of parsing the Java source code.
     *
     * <p>Without caching, this operation may be executed for too many times
     * for the same input.
     */
    private static final ParsedSources parsedSources = new ParsedSources();

    private final TypeName type;

    Validate(TypeName type) {
        this.type = checkNotNull(type);
    }

    @Override
    public String getLabel() {
        return String.format("validate:%s", typeUrl(type));
    }

    @Override
    public LineNumber locate(List<String> lines) {
        var typeNameNotFound = !isTypeNameIn(lines);
        if (typeNameNotFound) {
            return LineNumber.notInFile();
        }
        var code = LINE_JOINER.join(lines);
        var builderClass = findBuilder(code);
        if (builderClass == null) {
            return LineNumber.notInFile();
        }
        var method = builderClass.getMethod(BUILD_METHOD);
        if (method == null) {
            return LineNumber.notInFile();
        }
        var methodDeclarationLine = method.getLineNumber();
        var startPosition = method.getStartPosition();
        var endPosition = method.getEndPosition();
        var methodSource = code.substring(startPosition, endPosition);
        var returnIndex = returnLineIndex(methodSource);
        var returnLineNumber = methodDeclarationLine + returnIndex;
        return LineNumber.at(returnLineNumber - 1);
    }

    private boolean isTypeNameIn(List<String> lines) {
        var simpleName = type.getSimpleName();
        for (var line : lines) {
            if (line.contains(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable JavaClassSource findBuilder(String code) {
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
        Deque<String> names = new ArrayDeque<>(type.getNestingTypeNameList());
        names.addLast(type.getSimpleName());

        if (source.getName().equals(names.peek())) {
            names.poll();
        }
        return findSubClass(source, names);
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

    private static int returnLineIndex(String code) {
        var methodLines = LINE_SPLITTER.splitToList(code);
        var returnIndex = 0;
        for (var line : methodLines) {
            if (RETURN_LINE.matcher(line).matches()) {
                return returnIndex;
            }
            returnIndex++;
        }
        throw new IllegalArgumentException("No return line.");
    }
}
