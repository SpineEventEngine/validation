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

package io.spine.tools.validation

/**
 * A template placeholder that can be used in error messages.
 *
 * Enumerates placeholder names that can be used within Protobuf definitions.
 * Each validation option declares the supported placeholders. Take a look at
 * `options.proto` for examples.
 *
 * ### Important Note
 *
 * we have the same items in this enum as in `io.spine.validation.RuntimeErrorPlaceholder`
 * in the runtime library, which is exactly as this one. Please keep them in sync.
 * This duplication is done intentionally to prevent clash between the runtime library,
 * which is added to the classpath of the Compiler and the runtime library, which is part
 * of the Compiler itself because it is a part of Spine. As we complete our migration
 * of validation to codegen, the runtime library will either be significantly simplified,
 * or even its content may be moved to `base`. Then, the duplicate enum should be removed.
 */
public enum class ErrorPlaceholder(public val value: String) {

    // Common placeholders.
    FIELD_PATH("field.path"),
    FIELD_VALUE("field.value"),
    FIELD_TYPE("field.type"),
    MESSAGE_TYPE("message.type"),
    PARENT_TYPE("parent.type"),

    // Placeholders for the field options.
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
    WHEN_IN("when.in"),

    // Placeholders for the `oneof` options.
    GROUP_PATH("group.path"),

    // Placeholder for the message options.
    REQUIRE_FIELDS("require.fields");

    override fun toString(): String = value
}
