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

package io.spine.validate.text

import io.spine.validate.TemplateString

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
    checkAllPlaceholdersHasValue()
    var result = withPlaceholders
    for ((key, value) in placeholderValueMap) {
        result = result.replace("\${$key}", value)
    }
    return result
}

/**
 * Makes sure that every placeholder found within the template string has a value
 * in [TemplateString.getPlaceholderValueMap].
 */
private fun TemplateString.checkAllPlaceholdersHasValue() {
    val placeholders = templatePlaceholders()
    for (placeholder in placeholders) {
        if (!placeholderValueMap.containsKey(placeholder)) {
            throw IllegalArgumentException(
                "Can not format the given `TemplateString`: `$withPlaceholders`." +
                        "Missing value for placeholder: `\${$placeholder}`."
            )
        }
    }
}

/**
 * Extracts all placeholders used within the template string.
 */
private fun TemplateString.templatePlaceholders(): Set<String> =
    PLACEHOLDERS.findAll(withPlaceholders)
        .map { it.groupValues[1] }
        .toSet()

private val PLACEHOLDERS = Regex("\\$\\{([^}]+)}")