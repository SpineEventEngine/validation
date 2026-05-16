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

import io.spine.code.proto.FieldDeclaration
import io.spine.string.TemplateString
import io.spine.validation.StandardPlaceholder.FIELD_PATH
import io.spine.validation.StandardPlaceholder.FIELD_TYPE
import io.spine.validation.StandardPlaceholder.GOES_COMPANION
import io.spine.validation.StandardPlaceholder.PARENT_TYPE
import io.spine.validation.StandardPlaceholder.REGEX_PATTERN

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
    putPlaceholderValue(FIELD_PATH.value.name, field.name().value)
        .putPlaceholderValue(FIELD_TYPE.value.name, field.javaTypeName())
        .putPlaceholderValue(PARENT_TYPE.value.name, field.declaringType().name().value)

/**
 * Fills in the value for [GOES_COMPANION] placeholder.
 */
public fun TemplateString.Builder.withCompanion(field: FieldDeclaration): TemplateString.Builder =
    putPlaceholderValue(GOES_COMPANION.value.name, field.name().value)

/**
 * Fills in the value for [REGEX_PATTERN] placeholder.
 */
public fun TemplateString.Builder.withRegex(regex: String): TemplateString.Builder =
    putPlaceholderValue(REGEX_PATTERN.value.name, regex)
