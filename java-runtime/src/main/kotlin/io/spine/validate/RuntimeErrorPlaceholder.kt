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
 * A template placeholder that can be used in error messages.
 *
 * Enumerates placeholder names that can be used within Protobuf definitions.
 * Each validation option declares the supported placeholders within `options.proto`.
 *
 * Important Note: this enum is an exact copy of `io.spine.validation.java.ErrorPlaceholder`.
 * Please keep them in sync. Take a look at docs to the original enum for details.
 *
 * @see TemplateString
 */
public enum class RuntimeErrorPlaceholder(public val value: String) {

    // Common placeholders.
    FIELD_PATH("field.path"),
    FIELD_VALUE("field.value"),
    FIELD_TYPE("field.type"),
    PARENT_TYPE("parent.type"),

    // Option-specific placeholders.
    REGEX_PATTERN("regex.pattern"),
    REGEX_MODIFIERS("regex.modifiers"),
    GOES_COMPANION("goes.companion"),
    FIELD_PROPOSED_VALUE("field.proposed_value"),
    FIELD_DUPLICATES("field.duplicates"),
    RANGE_VALUE("range.value"),
    MAX_VALUE("max.value"),
    MAX_OPERATOR("max.operator"),
    MIN_VALUE("min.value"),
    MIN_OPERATOR("min.operator"),
    GROUP_PATH("group.path");

    override fun toString(): String = value
}
