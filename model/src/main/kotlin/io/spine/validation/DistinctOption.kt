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
import io.spine.option.IfHasDuplicatesOption
import io.spine.option.OptionsProto.distinct
import io.spine.option.OptionsProto.ifHasDuplicates
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldRef
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.File
import io.spine.protodata.ast.boolValue
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.ref
import io.spine.protodata.ast.unpack
import io.spine.protodata.check
import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.event.just
import io.spine.server.tuple.EitherOf2
import io.spine.validation.ErrorPlaceholder.FIELD_DUPLICATES
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.api.OPTION_NAME
import io.spine.validation.event.DistinctFieldDiscovered
import io.spine.validation.event.IfHasDuplicatesOptionDiscovered
import io.spine.validation.event.distinctFieldDiscovered
import io.spine.validation.event.ifHasDuplicatesOptionDiscovered

/**
 * Controls whether a field should be validated as `(distinct)`.
 *
 * Whenever a field marked with `(distinct)` option is discovered, emits
 * [DistinctFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is `repeated` or `map`.
 * 2. The option value is `true`.
 *
 * If (1) is violated, the policy reports a compilation error.
 *
 * Violation of (2) means that the `(distinct)` option is applied correctly,
 * but effectively disabled. [DistinctFieldDiscovered] is not emitted for
 * disabled options. In this case, the policy emits [NoReaction] meaning
 * that the option is ignored.
 */
internal class DistinctPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = DISTINCT)
        event: FieldOptionDiscovered
    ): EitherOf2<DistinctFieldDiscovered, NoReaction> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        if (!event.option.boolValue) {
            return ignore()
        }

        val message = defaultErrorMessage<IfHasDuplicatesOption>()
        return distinctFieldDiscovered {
            id = field.ref
            defaultErrorMessage = message
            subject = field
        }.asA()
    }
}

/**
 * Controls whether the `(if_has_duplicates)` option is applied correctly.
 *
 * Whenever a field marked with the `(if_has_duplicates)` option is discovered,
 * emits [IfHasDuplicatesOptionDiscovered] event if the following conditions are met:
 *
 * 1. The target field is also marked with the `(distinct)` option.
 * 2. The specified error message template does not contain placeholders
 * not supported by the option.
 *
 * A compilation error is reported in case of violation of any condition.
 */
internal class IfHasDuplicatesPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = IF_HAS_DUPLICATES)
        event: FieldOptionDiscovered
    ): Just<IfHasDuplicatesOptionDiscovered> {
        val field = event.subject
        val file = event.file
        ifHasDuplicates.checkPrimaryApplied(distinct, field,  file)

        val option = event.option.unpack<IfHasDuplicatesOption>()
        val message = option.errorMsg
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS, field, file, IF_HAS_DUPLICATES)

        return ifHasDuplicatesOptionDiscovered {
            id = field.ref
            customErrorMessage = message
        }.just()
    }
}

/**
 * A view of a field that is marked with `(distinct) = true` option.
 */
internal class DistinctFieldView : View<FieldRef, DistinctField, DistinctField.Builder>() {

    @Subscribe
    fun on(e: DistinctFieldDiscovered) {
        val currentMessage = state().errorMessage
        val message = currentMessage.ifEmpty { e.defaultErrorMessage }
        alter {
            subject = e.subject
            errorMessage = message
        }
    }

    @Subscribe
    fun on(e: IfHasDuplicatesOptionDiscovered) = alter {
        errorMessage = e.customErrorMessage
    }
}

private fun checkFieldType(field: Field, file: File) =
    Compilation.check(field.type.isSupported(), file, field.span) {
        "The field type `${field.type.name}` of `${field.qualifiedName}` is not supported" +
                " by the `($DISTINCT)` option. This options supports `map` and `repeated` fields."
    }

/**
 * Tells if this [FieldType] can be validated with the `(distinct)` option.
 */
private fun FieldType.isSupported(): Boolean = isMap || isList

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_DUPLICATES,
    FIELD_PATH,
    FIELD_TYPE,
    FIELD_VALUE,
    PARENT_TYPE,
)
