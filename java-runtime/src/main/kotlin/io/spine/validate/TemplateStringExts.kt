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

@file:JvmName("TemplateStrings")

package io.spine.validate

import io.spine.code.proto.FieldDeclaration
import io.spine.validate.RuntimeErrorPlaceholder.FIELD_PATH
import io.spine.validate.RuntimeErrorPlaceholder.FIELD_TYPE
import io.spine.validate.RuntimeErrorPlaceholder.GOES_COMPANION
import io.spine.validate.RuntimeErrorPlaceholder.PARENT_TYPE
import io.spine.validate.RuntimeErrorPlaceholder.REGEX_PATTERN

/**
 * Makes sure that each placeholder within the [template] string has a value
 * in [placeholders] list.
 *
 * @param template The template with placeholders like `${something}`.
 * @param placeholders The list with placeholder values.
 * @param lazyMessage The message to use in [IllegalArgumentException] if the check fails.
 */
public fun checkPlaceholdersHasValue(
    template: String,
    placeholders: Set<String>,
    lazyMessage: (List<String>) -> String =
        { "Missing value for the following template placeholders: `$it`." }
) {
    val neededPlaceholders = extractPlaceholders(template)
    val missing = mutableListOf<String>()
    for (placeholder in neededPlaceholders) {
        if (!placeholders.contains(placeholder)) {
            missing.add(placeholder)
        }
    }
    if (missing.isNotEmpty()) {
        throw IllegalArgumentException(lazyMessage(missing))
    }
}

/**
 * Makes sure that each placeholder within the [template] string has a value
 * in [placeholders] map.
 *
 * @param template The template with placeholders like `${something}`.
 * @param placeholders The map containing placeholders (without curly braces and the dollar sign)
 *  and their values.
 * @param lazyMessage The message to use in [IllegalArgumentException] if the check fails.
 */
public fun checkPlaceholdersHasValue(
    template: String,
    placeholders: Map<String, Any>,
    lazyMessage: (List<String>) -> String =
        { "Missing value for the following template placeholders: `$it`." }
): Unit = checkPlaceholdersHasValue(template, placeholders.keys, lazyMessage)

/**
 * Fills in the fields-related placeholders from the given [field] declaration.
 *
 * This method sets the values for the following placeholders:
 *
 * 1. [FIELD_PATH].
 * 2. [FIELD_TYPE].
 * 3. [PARENT_TYPE].
 */
public fun TemplateString.Builder.withField(field: FieldDeclaration): TemplateString.Builder =
    putPlaceholderValue(FIELD_PATH.value, field.name().value)
        .putPlaceholderValue(FIELD_TYPE.value, field.javaTypeName())
        .putPlaceholderValue(PARENT_TYPE.value, field.declaringType().name().value)

/**
 * Fills in the value for [GOES_COMPANION] placeholder.
 */
public fun TemplateString.Builder.withCompanion(field: FieldDeclaration): TemplateString.Builder =
    putPlaceholderValue(GOES_COMPANION.value, field.name().value)

/**
 * Fills in the value for [REGEX_PATTERN] placeholder.
 */
public fun TemplateString.Builder.withRegex(regex: String): TemplateString.Builder =
    putPlaceholderValue(REGEX_PATTERN.value, regex)

/**
 * Extracts all placeholders used within this [template] string.
 */
private fun extractPlaceholders(template: String): Set<String> =
    PLACEHOLDERS.findAll(template)
        .map { it.groupValues[1] }
        .toSet()

private val PLACEHOLDERS = Regex("\\$\\{([^}]+)}")
