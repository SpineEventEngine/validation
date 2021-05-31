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

package io.spine.kanban.codegen;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.protobuf.Message;
import io.spine.protodata.TypeName;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.protodata.renderer.LineNumber;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.Ast.typeUrl;
import static java.lang.String.format;

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

    private final TypeName type;

    Validate(TypeName type) {
        this.type = checkNotNull(type);
    }

    @NotNull
    @Override
    public String getLabel() {
        return format("validate:%s", typeUrl(type));
    }

    @NotNull
    @Override
    public LineNumber locate(List<String> lines) {
        String code = LINE_JOINER.join(lines);
        Optional<JavaClassSource> builder = findBuilder(code);
        if (!builder.isPresent()) {
            return LineNumber.notInFile();
        }
        JavaClassSource builderClass = builder.get();
        MethodSource<JavaClassSource> method = builderClass.getMethod(BUILD_METHOD);
        if (method == null) {
            return LineNumber.notInFile();
        }
        int methodDeclarationLine = method.getLineNumber();
        int startPosition = method.getStartPosition();
        int endPosition = method.getEndPosition();
        String methodSource = code.substring(startPosition, endPosition);
        int returnIndex = returnLineIndex(methodSource);
        int returnLineNumber = methodDeclarationLine + returnIndex;
        return LineNumber.at(returnLineNumber - 1);
    }

    private Optional<JavaClassSource> findBuilder(String code) {
        Optional<JavaClassSource> classSource = findClass(code);
        if (!classSource.isPresent()) {
            return Optional.empty();
        }
        JavaClassSource source = classSource.get();
        if (!source.hasNestedType(BUILDER_CLASS)) {
            return Optional.empty();
        }
        JavaSource<?> builder = source.getNestedType(BUILDER_CLASS);
        if (!builder.isClass()) {
            return Optional.empty();
        }
        JavaClassSource builderClass = (JavaClassSource) builder;
        return Optional.of(builderClass);
    }

    private Optional<JavaClassSource> findClass(String code) {
        JavaSource<?> javaSource = Roaster.parse(JavaSource.class, code);
        if (!javaSource.isClass()) {
            return Optional.empty();
        }
        JavaClassSource source = (JavaClassSource) javaSource;
        Deque<String> names = new ArrayDeque<>(type.getNestingTypeNameList());
        names.addLast(type.getSimpleName());

        if (source.getName().equals(names.peek())) {
            names.poll();
        }
        return findSubClass(source, names);
    }

    private static Optional<JavaClassSource> findSubClass(JavaClassSource topLevelClass,
                                                          Iterable<String> names) {
        JavaClassSource source = topLevelClass;
        for (String name : names) {
            if (!source.hasNestedType(name)) {
                return Optional.empty();
            }
            String superType = source.resolveType(source.getSuperType());
            if (!superType.equals(Message.class.getName())) {
                return Optional.empty();
            }
            JavaSource<?> nestedType = source.getNestedType(name);
            if (!nestedType.isClass()) {
                return Optional.empty();
            }
            source = (JavaClassSource) nestedType;
        }
        return Optional.of(source);
    }

    private static int returnLineIndex(String code) {
        List<String> methodLines = LINE_SPLITTER.splitToList(code);
        int returnIndex = 0;
        for (String line : methodLines) {
            if (RETURN_LINE.matcher(line).matches()) {
                return returnIndex;
            }
            returnIndex++;
        }
        throw new IllegalArgumentException("No return line.");
    }
}
