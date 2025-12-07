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

package io.spine.tools.validation.option

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.option.PatternOption
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.FieldRef
import io.spine.tools.compiler.ast.FieldType
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.event.FieldOptionDiscovered
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.ast.ref
import io.spine.tools.compiler.ast.unpack
import io.spine.tools.compiler.check
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.compiler.plugin.View
import io.spine.tools.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.tools.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.tools.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.tools.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.tools.validation.ErrorPlaceholder.REGEX_MODIFIERS
import io.spine.tools.validation.ErrorPlaceholder.REGEX_PATTERN
import io.spine.tools.validation.OPTION_NAME
import io.spine.tools.validation.checkPlaceholders
import io.spine.tools.validation.defaultMessage
import io.spine.validation.PatternField
import io.spine.validation.event.PatternFieldDiscovered
import io.spine.validation.event.patternFieldDiscovered

/**
 * Controls whether a field should be validated with the `(pattern)` option.
 *
 * Whenever a field marked with the `(pattern)` option is discovered, emits
 * [PatternFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. The error message does not contain unsupported placeholders.
 *
 * Any violation of the above conditions leads to a compilation error.
 */
internal class PatternReaction : Reaction<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = PATTERN)
        event: FieldOptionDiscovered
    ): Just<PatternFieldDiscovered> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        val option = event.option.unpack<PatternOption>()
        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS, field, file, PATTERN)

        return patternFieldDiscovered {
            id = field.ref
            errorMessage = message
            pattern = option.regex
            modifier = option.modifier
            subject = field
        }.just()
    }
}

/**
 * A view of a field that is marked with the `(pattern)` option.
 */
internal class PatternFieldView : View<FieldRef, PatternField, PatternField.Builder>() {

    @Subscribe
    fun on(e: PatternFieldDiscovered) = alter {
        id = e.id
        errorMessage = e.errorMessage
        pattern = e.pattern
        modifier = e.modifier
        subject = e.subject
    }
}

private fun checkFieldType(field: Field, file: File) =
    Compilation.check(field.type.isSupported(), file, field.span) {
        "The field type `${field.type.name}` of `${field.qualifiedName}` is not supported" +
                " by the `(${PATTERN})` option. Supported field types: strings and repeated" +
                " of strings."
    }

/**
 * Tells if this [FieldType] can be validated with the `(pattern)` option.
 */
private fun FieldType.isSupported(): Boolean = isSingularString || isRepeatedString

/**
 * Tells if this [FieldType] represents a `repeated string` field.
 *
 * The property is `public` because the option generator also uses it.
 */
public val FieldType.isRepeatedString: Boolean
    get() = list.primitive == TYPE_STRING

/**
 * Tells if this [FieldType] represents a `string` field.
 *
 * The property is `public` because the option generator also uses it.
 */
public val FieldType.isSingularString: Boolean
    get() = primitive == TYPE_STRING

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_TYPE,
    FIELD_VALUE,
    PARENT_TYPE,
    REGEX_MODIFIERS,
    REGEX_PATTERN,
)
