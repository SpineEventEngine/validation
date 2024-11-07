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
import io.spine.protodata.render.InsertionPoint
import io.spine.text.TextCoordinates
import io.spine.text.TextFactory
import io.spine.validation.java.BuilderInsertionPoint.Companion.BUILD_METHOD
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors.toSet
import org.jboss.forge.roaster.model.source.MethodSource

/**
 * An insertion point at the place where Java validation code should be inserted.
 *
 * Points at a line in the `Builder.build()` method right before the return statement.
 */
@Immutable
internal class ValidateBeforeReturn : InsertionPoint {

    override val label: String
        get() = ValidateBeforeReturn::class.java.simpleName

    override fun locate(text: String): Set<TextCoordinates> =
        BuilderInsertionPoint.findBuilders(text)
            .map { b -> b.getMethod(BUILD_METHOD) }
            .filter { obj -> Objects.nonNull(obj) }
            .map { m -> findLine(m, text) }
            .collect(toSet())

    private fun findLine(method: MethodSource<*>, code: String): TextCoordinates {
        val methodDeclarationLine = method.lineNumber
        val startPosition = method.startPosition
        val endPosition = method.endPosition
        val methodSource = code.substring(startPosition, endPosition)
        val returnIndex = returnLineIndex(methodSource)
        val returnLineNumber = methodDeclarationLine + returnIndex
        return atLine(returnLineNumber - 1)
    }

    companion object {

        private val RETURN_LINE: Pattern = Pattern.compile(
            "\\s*return .+;.*", Pattern.UNICODE_CASE or Pattern.DOTALL
        )

        private fun returnLineIndex(code: String): Int {
            val methodLines = TextFactory.lineSplitter().split(code)
            var returnIndex = 0
            for (line in methodLines) {
                if (RETURN_LINE.matcher(line).matches()) {
                    return returnIndex
                }
                returnIndex++
            }
            throw IllegalArgumentException("No return statement.")
        }
    }
}
