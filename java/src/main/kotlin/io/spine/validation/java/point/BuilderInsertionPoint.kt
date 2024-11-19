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
package io.spine.validation.java.point

import com.google.errorprone.annotations.Immutable
import com.google.protobuf.GeneratedMessageV3
import io.spine.protodata.render.InsertionPoint
import java.util.*
import java.util.stream.Stream
import java.util.stream.Stream.generate
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource

/**
 * Abstract base for insertion points for generated code implementing
 * [ValidatingBuilder][io.spine.validate.ValidatingBuilder] interface.
 */
@Immutable
internal abstract class BuilderInsertionPoint : InsertionPoint {

    companion object {

        const val BUILD_METHOD: String = "build"
        const val BUILD_PARTIAL_METHOD: String = "buildPartial"

        /**
         * Cached results of parsing the Java source code.
         *
         * Without caching, this operation may be executed for too many times
         * for the same input.
         */
        private val parsedSources = ParsedSources()

        private fun parseSource(code: String): JavaSource<*> = parsedSources[code]

        /**
         * Obtains instances of [JavaClassSource] that are found in the given the Java class source.
         *
         * Since the code may contain several nested message classes, there can be several
         * builders classes discovered in the code.
         *
         * @param code The Java code to parse.
         * @return the stream with the found builders
         */
        @JvmStatic
        fun findBuilders(code: String): Stream<JavaClassSource> {
            val classSources = findMessageClasses(code)
            return classSources.flatMap {
                // This construct helps in type transformation because `Optional` is streamable.
                Optional.ofNullable(it!!.findBuilder()).stream()
            }.filter(Objects::nonNull)
        }

        private fun findMessageClasses(code: String): Stream<JavaClassSource> {
            val javaSource = parseSource(code)
            if (!javaSource.isClass) {
                return Stream.empty()
            }
            val javaClass = javaSource as JavaClassSource
            val nestedTypes: Deque<JavaSource<*>> = ArrayDeque()
            nestedTypes.add(javaClass)

            val types = generate {
                if (nestedTypes.isEmpty()) null else nestedTypes.poll()
            }.takeWhile(Objects::nonNull)

            val allClasses = types.filter { it?.isClass ?: false }
                .map(JavaClassSource::class.java::cast)
                .peek { nestedTypes.addAll(it!!.nestedTypes) }

            return allClasses.filter { it.isMessageClass() }
        }
    }
}

private const val BUILDER_CLASS = "Builder"

private fun JavaClassSource.isMessageClass(): Boolean =
    superType == GeneratedMessageV3::class.java.canonicalName

private fun JavaClassSource.findBuilder(): JavaClassSource? {
    val builder = getNestedType(BUILDER_CLASS) ?: return null
    return if (builder.isClass) {
        builder as JavaClassSource
    } else {
        null
    }
}
