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

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.option.IfSetAgainOption
import io.spine.option.OptionsProto.ifSetAgain
import io.spine.option.OptionsProto.setOnce
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.FieldRef
import io.spine.tools.compiler.ast.FieldType
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.boolValue
import io.spine.tools.compiler.ast.event.FieldOptionDiscovered
import io.spine.tools.compiler.ast.isList
import io.spine.tools.compiler.ast.isMap
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.ast.ref
import io.spine.tools.compiler.ast.unpack
import io.spine.tools.compiler.check
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.compiler.plugin.View
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.event.just
import io.spine.server.tuple.EitherOf2
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_PROPOSED_VALUE
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.api.OPTION_NAME
import io.spine.validation.event.IfSetAgainOptionDiscovered
import io.spine.validation.event.SetOnceFieldDiscovered
import io.spine.validation.event.ifSetAgainOptionDiscovered
import io.spine.validation.event.setOnceFieldDiscovered

/**
 * Controls whether a field should be validated with the `(set_once)` option.
 *
 * Whenever a field marked with `(set_once)` option is discovered, emits
 * [SetOnceFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. The option value is `true`.
 *
 * If (1) is violated, the reaction reports a compilation error.
 *
 * Violation of (2) means that the `(set_once)` option is applied correctly,
 * but effectively disabled. [SetOnceFieldDiscovered] is not emitted for
 * disabled options. In this case, the reaction emits [NoReaction] meaning
 * that the option is ignored.
 */
internal class SetOnceReaction : Reaction<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = SET_ONCE)
        event: FieldOptionDiscovered
    ): EitherOf2<SetOnceFieldDiscovered, NoReaction> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        if (!event.option.boolValue) {
            return ignore()
        }

        val defaultMessage = defaultErrorMessage<IfSetAgainOption>()
        return setOnceFieldDiscovered {
            id = field.ref
            subject = field
            defaultErrorMessage = defaultMessage
        }.asA()
    }
}

/**
 * Controls whether the `(if_set_again)` option is applied correctly.
 *
 * Whenever a field marked with the `(if_set_again)` option is discovered,
 * emits [IfSetAgainOptionDiscovered] event if the following conditions are met:
 *
 * 1. The target field is also marked with the `(set_once)` option.
 * 2. The specified error message template does not contain placeholders
 * not supported by the option.
 *
 * A compilation error is reported in case of violation of any condition.
 */
internal class IfSetAgainReaction : Reaction<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = IF_SET_AGAIN)
        event: FieldOptionDiscovered
    ): Just<IfSetAgainOptionDiscovered> {
        val field = event.subject
        val file = event.file
        ifSetAgain.checkPrimaryApplied(setOnce, field, file)

        val option = event.option.unpack<IfSetAgainOption>()
        val message = option.errorMsg
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS, field, file, IF_SET_AGAIN)

        return ifSetAgainOptionDiscovered {
            id = field.ref
            customErrorMessage = message
        }.just()
    }
}

/**
 * A view of a field that is marked with `(set_once) = true` option.
 */
internal class SetOnceFieldView : View<FieldRef, SetOnceField, SetOnceField.Builder>() {

    @Subscribe
    fun on(e: SetOnceFieldDiscovered) {
        val currentMessage = state().errorMessage
        val message = currentMessage.ifEmpty { e.defaultErrorMessage }
        alter {
            subject = e.subject
            errorMessage = message
        }
    }

    @Subscribe
    fun on(e: IfSetAgainOptionDiscovered) = alter {
        errorMessage = e.customErrorMessage
    }
}

private fun checkFieldType(field: Field, file: File) =
    Compilation.check(field.type.isSupported(), file, field.span) {
        "The field type `${field.type.name}` of the `${field.qualifiedName}` field" +
                " is not supported by the `($SET_ONCE)` option. Supported field types: messages," +
                " enums, strings, bytes, bools and all numeric fields."
    }

/**
 * Tells if this [FieldType] can be validated with the `(set_once)` option.
 */
private fun FieldType.isSupported(): Boolean = !isList && !isMap

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_PROPOSED_VALUE,
    FIELD_TYPE,
    FIELD_VALUE,
    PARENT_TYPE,
)
