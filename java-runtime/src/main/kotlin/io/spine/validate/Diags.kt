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

import com.google.common.escape.Escaper
import com.google.common.escape.Escapers
import io.spine.string.containsLineSeparators
import io.spine.string.escapeLineSeparators
import org.checkerframework.checker.regex.qual.Regex as CheckRegex

/**
 * Diagnostic messages used by the validation library.
 */
@Suppress("ConstPropertyName") // https://bit.ly/kotlin-prop-names
public object Diags {

    /**
     * Messages associated with the `regex` field constraint.
     */
    public object Regex {

        private val slashEscaper: Escaper = Escapers.builder()
            .addEscape('\\', "\\\\")
            .build()

        public const val prefix: String = "The string must match the regular expression"

        @JvmStatic
        public fun errorMessage(regex: @CheckRegex String): String {
            val withoutLineSeparators = escapeLineSeparators(regex)
            val escaped = slashEscaper.escape(withoutLineSeparators)
            return "$prefix `$escaped`."
        }

        private fun escapeLineSeparators(regex: String): String {
            var toEscape = regex
            if (regex.containsLineSeparators()) {
                toEscape = regex.escapeLineSeparators()
            }
            return toEscape
        }
    }

    /**
     * Messages associated with the `required` field constraint.
     */
    public object Required {
        public const val singularErrorMsg: String = "The field must be set."
        public const val collectionErrorMsg: String = "The collection must not be empty."
    }

    /**
     * Messages associated with the `is_required` constraint of `oneof` fields.
     */
    public object IsRequired {
        public const val operatorDescription: String = "One of the fields must be set."
        public fun errorMessage(oneofName: String): String =
            "One of the fields in the `$oneofName` group must be set."
    }
}
