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

package io.spine.validation.java.placeholder

import io.spine.protodata.java.Expression

internal typealias Placeholder = String
internal typealias PrintfParam = Expression<String>
internal typealias PrintfString = String

internal class PlaceholderParser(
    private val placeholders: Map<Placeholder, PrintfParam>,
    private val onUnsupportedPlaceholder: (Placeholder, Set<Placeholder>) -> Nothing,
) {

    private companion object {

        /**
         * A Regex pattern for placeholders.
         */
        val PlaceholderSearch = Regex("""\{(.*?)}""")
    }

    /**
     * Prepares `printf`-style format string and its parameters from the given
     * [message] string, which may contain one or more placeholders.
     *
     * This method does the following:
     *
     * 1. Finds all placeholders in the given [message] and replaces them with `%s`.
     * 2. For each found placeholder, it finds its corresponding [value][placeholders],
     *    and puts it to the list of parameters.
     */
    fun toPrintfString(message: String): Pair<PrintfString, List<PrintfParam>> {
        val params = mutableListOf<Expression<String>>()
        val format = PlaceholderSearch.replace(message) { matchResult ->
            val name = matchResult.groupValues[1]
            val value = placeholders[name] ?: onUnsupportedPlaceholder(name, placeholders.keys)
            "%s".also { params.add(value) }
        }
        return format to params
    }
}
