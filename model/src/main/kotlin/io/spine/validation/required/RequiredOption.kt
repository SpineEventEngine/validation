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

package io.spine.validation.required

import io.spine.core.External
import io.spine.core.Where
import io.spine.option.IfMissingOption
import io.spine.option.OptionsProto.ifMissing
import io.spine.option.OptionsProto.required
import io.spine.server.event.Just
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.event.just
import io.spine.server.tuple.EitherOf2
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.boolValue
import io.spine.tools.compiler.ast.event.FieldOptionDiscovered
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.ast.ref
import io.spine.tools.compiler.ast.unpack
import io.spine.tools.compiler.check
import io.spine.tools.compiler.plugin.Reaction
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.IF_MISSING
import io.spine.validation.OPTION_NAME
import io.spine.validation.REQUIRED
import io.spine.validation.checkPlaceholders
import io.spine.validation.checkPrimaryApplied
import io.spine.validation.defaultErrorMessage
import io.spine.validation.event.IfMissingOptionDiscovered
import io.spine.validation.event.RequiredFieldDiscovered
import io.spine.validation.event.ifMissingOptionDiscovered
import io.spine.validation.event.requiredFieldDiscovered
import io.spine.validation.required.RequiredFieldSupport.isSupported

/**
 * Controls whether a field should be validated as `(required)`.
 *
 * Whenever a field marked with the `(required)` option is discovered, emits
 * [RequiredFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. The option value is `true`.
 *
 * If (1) is violated, the reaction reports a compilation error.
 *
 * Violation of (2) means that the `(required)` option is applied correctly,
 * but effectively disabled. [RequiredFieldDiscovered] is not emitted for
 * disabled options. In this case, the reaction emits [NoReaction] meaning
 * that the option is ignored.
 *
 * Note that this reaction is responsible only for fields explicitly marked with
 * the validation option. There are other policies that handle implicitly
 * required fields, i.e., ID fields in entities and signal messages.
 *
 * @see [RequiredIdOptionReaction]
 * @see [RequiredIdPatternReaction]
 */
internal class RequiredReaction : Reaction<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = REQUIRED)
        event: FieldOptionDiscovered,
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        if (!event.option.boolValue) {
            return ignore()
        }

        val defaultMessage = defaultErrorMessage<IfMissingOption>()
        return requiredFieldDiscovered {
            id = field.ref
            subject = field
            defaultErrorMessage = defaultMessage
        }.asA()
    }
}

/**
 * Controls whether the `(if_missing)` option is applied correctly.
 *
 * Whenever a field marked with the `(if_missing)` option is discovered, emits
 * [IfMissingOptionDiscovered] event if the following conditions are met:
 *
 * 1. The target field is also marked with the `(required)` option.
 * 2. The specified error message template does not contain placeholders
 * not supported by the option.
 *
 * A compilation error is reported in case of violation of any condition.
 */
internal class IfMissingReaction : Reaction<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = IF_MISSING)
        event: FieldOptionDiscovered
    ): Just<IfMissingOptionDiscovered> {
        val field = event.subject
        val file = event.file
        ifMissing.checkPrimaryApplied(required, field, file)

        val option = event.option.unpack<IfMissingOption>()
        val message = option.errorMsg
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS, field, file, IF_MISSING)

        return ifMissingOptionDiscovered {
            id = field.ref
            customErrorMessage = message
        }.just()
    }
}

private fun checkFieldType(field: Field, file: File) =
    Compilation.check(field.type.isSupported(), file, field.span) {
        "The field type `${field.type.name}` of the `${field.qualifiedName}` is not supported" +
                " by the `($REQUIRED)` option. Supported field types: messages, enums," +
                " strings, bytes, repeated, and maps."
    }

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_TYPE,
    PARENT_TYPE,
)
