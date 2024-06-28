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

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.GeneratedMessageV3;
import io.spine.protodata.TypeName;
import io.spine.protodata.renderer.InsertionPoint;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.stream.Stream.generate;

/**
 * Abstract base for insertion points for generated code implementing
 * {@link io.spine.validate.ValidatingBuilder ValidatingBuilder} interface.
 */
@Immutable
abstract class BuilderInsertionPoint implements InsertionPoint {

    private static final String BUILDER_CLASS = "Builder";

    /**
     * Cached results of parsing the Java source code.
     *
     * <p>Without caching, this operation may be executed for too many times
     * for the same input.
     */
    private static final ParsedSources parsedSources = new ParsedSources();

    static final String BUILD_METHOD = "build";
    static final String BUILD_PARTIAL_METHOD = "buildPartial";

    private static final String MESSAGE_SUPERCLASS = GeneratedMessageV3.class.getCanonicalName();

    /**
     * Obtains the Java class source for the builder in the given code.
     *
     * @param code
     *         the Java code to parse
     * @return the found binder code or {@code null} if no builder class found
     */
    protected static Stream<JavaClassSource> findBuilders(String code) {
        var classSources = findMessageClasses(code);
        return classSources.flatMap(cls -> findBuilder(cls).stream());
    }

    protected static Optional<JavaClassSource> findBuilder(TypeName messageName, String code) {
        var classSources = findMessageClasses(code);
        var targetClassName = messageName.getSimpleName();
        var messageClass = classSources
                .filter(cls -> targetClassName.equals(cls.getName()))
                .findFirst();
        return messageClass
                .flatMap(BuilderInsertionPoint::findBuilder);
    }

    private static Optional<JavaClassSource> findBuilder(JavaClassSource cls) {
        var builder = cls.getNestedType(BUILDER_CLASS);
        if (builder == null) {
            return empty();
        }
        if (!builder.isClass()) {
            return empty();
        }
        var builderClass = (JavaClassSource) builder;
        return Optional.of(builderClass);
    }

    private static Stream<JavaClassSource> findMessageClasses(String code) {
        var javaSource = parseSource(code);
        if (!javaSource.isClass()) {
            return Stream.empty();
        }
        var javaClass = (JavaClassSource) javaSource;
        Deque<JavaSource<?>> nestedTypes = new ArrayDeque<>();
        nestedTypes.add(javaClass);
        @SuppressWarnings("ReturnOfNull") // legit in this case. Filtered by `takeWhile()`.
        var types = generate(
                () -> nestedTypes.isEmpty() ? null : nestedTypes.poll()
        ).takeWhile(Objects::nonNull);
        var allClasses = types.filter(JavaSource::isClass)
                              .map(JavaClassSource.class::cast)
                              .peek(c -> nestedTypes.addAll(c.getNestedTypes()));
        return allClasses.filter(BuilderInsertionPoint::isMessageClass);
    }

    private static JavaSource<?> parseSource(String code) {
        return parsedSources.get(code);
    }

    private static boolean isMessageClass(JavaClassSource cls) {
        var superClass = cls.getSuperType();
        return MESSAGE_SUPERCLASS.equals(superClass);
    }
}
