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

package io.spine.validation

import io.spine.string.Placeholder

/**
 * The standard set of [Placeholder]s that can be used in error messages.
 *
 * Enumerates placeholder names that can be used within Protobuf definitions.
 * Each validation option declares the supported placeholders. Take a look at
 * `options.proto` for examples.
 *
 * The enum is used by the compiler model and Java renderer when validating and rendering
 * built-in option error messages.
 */
public enum class StandardPlaceholder(public val value: Placeholder) {

    // Common placeholders.
    FIELD_PATH(Placeholder("field.path")),
    FIELD_VALUE(Placeholder("field.value")),
    FIELD_TYPE(Placeholder("field.type")),
    MESSAGE_TYPE(Placeholder("message.type")),
    PARENT_TYPE(Placeholder("parent.type")),

    // Placeholders for the field options.
    REGEX_PATTERN(Placeholder("regex.pattern")),
    REGEX_MODIFIERS(Placeholder("regex.modifiers")),
    GOES_COMPANION(Placeholder("goes.companion")),
    FIELD_PROPOSED_VALUE(Placeholder("field.proposed_value")),
    FIELD_DUPLICATES(Placeholder("field.duplicates")),
    RANGE_VALUE(Placeholder("range.value")),
    MAX_VALUE(Placeholder("max.value")),
    MAX_OPERATOR(Placeholder("max.operator")),
    MIN_VALUE(Placeholder("min.value")),
    MIN_OPERATOR(Placeholder("min.operator")),

    @Deprecated(message = "Use the placeholder reference from Spine Time instead.")
    WHEN_IN(Placeholder("when.in")),

    // Placeholders for the `oneof` options.
    GROUP_PATH(Placeholder("group.path")),

    // Placeholder for the message options.
    REQUIRE_FIELDS(Placeholder("require.fields"));

    /**
     * The placeholder text as it appears in a template string (e.g., `${field.path}`).
     *
     * Shortcut for [value].[placed][Placeholder.placed].
     */
    public val placed: String
        get() = value.placed
}
