/*
 * Copyright 2025, TeamDev. All rights reserved.
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
import io.spine.validate.TemplateString
import io.spine.validate.checkPlaceholdersHasValue
import io.spine.validation.ErrorPlaceholder

/**
 * Yields an expression that creates a new instance of [TemplateString].
 *
 * Note that this method differs from the one provided by the API module
 * in that it accepts placeholder keys as [ErrorPlaceholder]. This enum
 * contains placeholder keys for built-in options.
 *
 * @param placeholders The supported placeholders and their values.
 * @param optionName The name of the option, which declared the provided [placeholders].
 */
public fun templateString(
    template: String,
    placeholders: Map<ErrorPlaceholder, Expression<String>>,
    optionName: String
): Expression<TemplateString> =
    withStringPlaceholders(template, placeholders.mapKeys { it.key.value }, optionName)

/**
 * Yields an expression that creates a new instance of [TemplateString].
 *
 * @param placeholders The supported placeholders and their values.
 * @param optionName The name of the option, which declared the provided [placeholders].
 */
public fun withStringPlaceholders(
    template: String,
    placeholders: Map<String, Expression<String>>,
    optionName: String
): Expression<TemplateString> {
    checkPlaceholdersHasValue(template, placeholders) { missingKeys ->
        "Unexpected error message placeholders `$missingKeys` specified for the `($optionName)`" +
                " option. The available placeholders: `${placeholders.keys}`. Please make sure" +
                " that the code that verifies the message placeholders and its code generator" +
                " operate with the same set of placeholders."
    }
    val placeholderEntries = mapExpression(
        StringClass, StringClass,
        placeholders.mapKeys { StringLiteral(it.key) }
    )
    val escapedTemplate = restoreProtobufEscapes(template)
    return TemplateStringClass.newBuilder()
        .chainSet("withPlaceholders", StringLiteral(escapedTemplate))
        .chainPutAll("placeholderValue", placeholderEntries)
        .chainBuild()
}
