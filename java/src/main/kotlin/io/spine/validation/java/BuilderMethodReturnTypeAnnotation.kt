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

package io.spine.validation.java

import com.google.errorprone.annotations.Immutable
import io.spine.text.TextCoordinates
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource

/**
 * An insertion point for a type annotation for the type returned by a message builder method.
 *
 * The insertion point is placed in the spot where the `TYPE_USE` annotation can be put.
 * The annotation would mark the method return type with the given name.
 *
 * It is assumed that:
 *
 *  1. The method has no parameters.
 *  2. The method is public.
 *  3. The method returns a value.
 *  4. The return type is formatted as a fully qualified name of a class and placed on the same
 *   line with the `public` modifier and the method name.
 */
@Immutable
internal open class BuilderMethodReturnTypeAnnotation(
    private val methodName: String
) : BuilderInsertionPoint() {

    private val signaturePattern: Pattern =
        Pattern.compile("\\s+public\\s+[\\w.]+\\.(\\w+)\\s+$methodName")

    override val label: String
        get() = javaClass.simpleName

    override fun locate(text: String): Set<TextCoordinates> {
        return findBuilders(text)
            .map { it.getMethod(methodName) }
            .filter(Objects::nonNull)
            .map { it.locateMethod(text.lines()) }
            .filter(Objects::nonNull)
            .map { it!! }
            .collect(Collectors.toSet())
    }

    private fun MethodSource<JavaClassSource>.locateMethod(lines: List<String>): TextCoordinates? {
        val declarationLineIndex = lineNumber - 1
        var matcher: Matcher
        for (signatureIndex in declarationLineIndex..<lines.size) {
            matcher = signaturePattern.matcher(lines[signatureIndex])
            if (matcher.find()) {
                val pointInLine = matcher.start(1)
                return at(signatureIndex, pointInLine)
            }
        }
        return null
    }
}
