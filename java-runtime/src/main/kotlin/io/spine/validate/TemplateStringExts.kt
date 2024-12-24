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

package io.spine.validate

/**
 * Returns a template string with all placeholders substituted with
 * their actual values.
 *
 * For example, for a template string with the following values:
 *
 * ```
 * with_placeholders = "My dog's name is ${dog.name}."
 * placeholder_value = { "dog.name": "Fido" }
 * ```
 *
 * This method will return "My dog's name is Fido".
 */
public fun TemplateString.format(): String {
    checkPlaceholdersHasValue(withPlaceholders, placeholderValueMap) {
        "Can not format the given `TemplateString`: `$withPlaceholders`." +
                "Missing value for the following placeholders: `$it`."
    }
    var result = withPlaceholders
    for ((key, value) in placeholderValueMap) {
        result = result.replace("\${$key}", value)
    }
    return result
}

/**
 * Makes sure that each placeholder within the [template] string has a value
 * in [placeholders] map.
 *
 * @param template The template with placeholders like `${something}`.
 * @param placeholders The map containing placeholder values.
 * @param lazyMessage The message to use in [IllegalArgumentException] if the check fails.
 */
public fun checkPlaceholdersHasValue(
    template: String,
    placeholders: Map<String, Any>,
    lazyMessage: (List<String>) -> String =
        { "Missing value for the following template placeholders: `$it`." }
) {
    val neededPlaceholders = extractPlaceholders(template)
    val missing = mutableListOf<String>()
    for (placeholder in neededPlaceholders) {
        if (!placeholders.containsKey(placeholder)) {
            missing.add("\${$placeholder}")
        }
    }
    if (missing.isNotEmpty()) {
        throw IllegalArgumentException(lazyMessage(missing))
    }
}

/**
 * Extracts all placeholders used within this [template] string.
 */
private fun extractPlaceholders(template: String): Set<String> =
    PLACEHOLDERS.findAll(template)
        .map { it.groupValues[1] }
        .toSet()

private val PLACEHOLDERS = Regex("\\$\\{([^}]+)}")
