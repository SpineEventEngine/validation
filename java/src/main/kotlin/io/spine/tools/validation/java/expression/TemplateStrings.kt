/*
 * Copyright 2026, TeamDev. All rights reserved.
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

@file:JvmName("TemplateStrings")

package io.spine.tools.validation.java.expression

import io.spine.protobuf.restoreProtobufEscapes
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.jvm.mapExpression
import io.spine.tools.compiler.jvm.newBuilder
import io.spine.string.Placeholder
import io.spine.string.TemplateString
import io.spine.string.joinQuoted

/**
 * Yields an expression that creates a new instance of [TemplateString].
 *
 * Note that this method differs from the one provided by the API module
 * in that it accepts placeholder keys as [Placeholder]. The standard
 * placeholder keys for built-in options are provided by [io.spine.validation.StandardPlaceholder].
 *
 * @param placeholders The supported placeholders and their values.
 * @param optionName The name of the option, which declared the provided [placeholders].
 */
public fun templateString(
    template: String,
    placeholders: Map<Placeholder, Expression<String>>,
    optionName: String
): Expression<TemplateString> {
    checkPlaceholdersHasValue(template, placeholders.keys) { missing ->
        "Unexpected error message placeholders ${missing.joinQuoted()} specified for" +
                " the `($optionName)` option." +
                " The available placeholders: ${placeholders.keys.joinQuoted()}." +
                " Please make sure that the code that verifies the message placeholders and" +
                " its code generator operate with the same set of placeholders."
    }
    val placeholderEntries = mapExpression(
        StringClass, StringClass,
        placeholders.mapKeys { StringLiteral(it.key.name) }
    )
    val escapedTemplate = restoreProtobufEscapes(template)
    return TemplateStringClass.newBuilder()
        .chainSet("withPlaceholders", StringLiteral(escapedTemplate))
        .chainPutAll("placeholderValue", placeholderEntries)
        .chainBuild()
}

/**
 * Makes sure that each placeholder within the [template] string is present
 * in the [placeholders] set.
 *
 * @param template The template with placeholders like `${something}`.
 * @param placeholders The set of available placeholders.
 */
private fun checkPlaceholdersHasValue(
    template: String,
    placeholders: Set<Placeholder>,
    lazyMessage: (List<Placeholder>) -> String
) {
    val needed = Placeholder.extractPlaceholders(template)
    val missing = needed.filter { it !in placeholders }
    if (missing.isNotEmpty()) {
        throw IllegalArgumentException(lazyMessage(missing))
    }
}
