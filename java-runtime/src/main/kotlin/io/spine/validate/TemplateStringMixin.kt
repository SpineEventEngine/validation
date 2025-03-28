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

package io.spine.validate

import io.spine.annotation.GeneratedMixin

/**
 * Extends [TemplateString] with additional functionality.
 */
@GeneratedMixin
public interface TemplateStringMixin {

    /**
     * The template string that may contain one or more placeholders.
     */
    public val withPlaceholders: String

    /**
     * A map that provides values for placeholders referenced in [withPlaceholders].
     *
     * The keys in this map should match the placeholder keys inside [withPlaceholders]
     * excluding the `${}` placeholder markers.
     *
     * All placeholders present in the template string must have corresponding entries
     * in this map. Otherwise, the template is considered invalid.
     */
    public val placeholderValueMap: Map<String, String>

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
     * This method will return "My dog's name is Fido."
     */
    public fun format(): String {
        checkPlaceholdersHasValue(withPlaceholders, placeholderValueMap) {
            "Cannot format the given `TemplateString`: `$withPlaceholders`. " +
                    "Missing value for the following placeholders: `$it`."
        }
        return (this as TemplateString).formatUnsafe()
    }

    /**
     * Returns a template string with all placeholders substituted with
     * their actual values, without validating that all placeholders have
     * corresponding values.
     *
     * This method does not check whether every placeholder in the template has a matching value
     * in the placeholder map. Any placeholders without a corresponding value will remain
     * unchanged in the resulting string.
     *
     * For example, for a template string with the following values:
     *
     * ```
     * withPlaceholders = "My dog's name is ${dog.name} and its breed is ${dog.breed}."
     * placeholderValue = { "dog.name": "Fido" }
     * ```
     *
     * This method will return "My dog's name is Fido and its breed is ${dog.breed}.".
     */
    public fun formatUnsafe(): String {
        var result = withPlaceholders
        for ((key, value) in placeholderValueMap) {
            result = result.replace("\${$key}", value)
        }
        return result
    }
}
