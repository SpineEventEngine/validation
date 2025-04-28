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

package io.spine.validation

import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.check
import io.spine.validate.extractPlaceholders

/**
 * Checks if this [String] contains placeholders that are not present
 * in the given set of the [supportedPlaceholders], and reports a compilation error if so.
 *
 * @param supportedPlaceholders The set of placeholders that can occur in this [String].
 * @param field The field declared the template.
 * @param file The file that contains the [field] declaration.
 * @param option The name of the option with which the message template was specified.
 */
internal fun String.checkPlaceholders(
    supportedPlaceholders: Set<ErrorPlaceholder>,
    field: Field,
    file: File,
    option: String
) {
    val template = this
    val missing = missingPlaceholders(template, supportedPlaceholders)
    Compilation.check(missing.isEmpty(), file, field.span) {
        "The `${field.qualifiedName}` field specifies an error message for the `($option)`" +
                " option using unsupported placeholders: `$missing`. Supported placeholders are" +
                " the following: `${supportedPlaceholders.map { it.value }}`."
    }
}

/**
 * Returns a set of placeholders that are used by the given [template] string,
 * but not present in the provided [placeholders] set.
 *
 * @param template The template with placeholders like `${something}`.
 * @param placeholders The set of error placeholders.
 */
private fun missingPlaceholders(
    template: String,
    placeholders: Set<ErrorPlaceholder>
): Set<String> {
    val requested = extractPlaceholders(template)
    val provided = placeholders.map { it.value }
    val missing = mutableSetOf<String>()
    for (placeholder in requested) {
        if (!provided.contains(placeholder)) {
            missing.add(placeholder)
        }
    }
    return missing
}
