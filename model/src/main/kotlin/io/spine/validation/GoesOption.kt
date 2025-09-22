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
import io.spine.option.GoesOption
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.FieldRef
import io.spine.tools.compiler.ast.FieldType
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BYTES
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.event.FieldOptionDiscovered
import io.spine.tools.compiler.ast.field
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.ast.ref
import io.spine.tools.compiler.ast.unpack
import io.spine.tools.compiler.check
import io.spine.tools.compiler.jvm.findField
import io.spine.tools.compiler.plugin.Policy
import io.spine.tools.compiler.plugin.View
import io.spine.tools.compiler.type.message
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.GOES_COMPANION
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.api.OPTION_NAME
import io.spine.validation.event.GoesFieldDiscovered
import io.spine.validation.event.goesFieldDiscovered

/**
 * Controls whether a field should be validated with the `(goes)` option.
 *
 * Whenever a field marked with the `(goes)` option is discovered, emits
 * [GoesFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. The companion field is present in the message.
 * 3. The companion field and the target field are different fields.
 * 4. The companion field type is supported by the option.
 * 5. The error message does not contain unsupported placeholders.
 *
 * Any violation of the above conditions leads to a compilation error.
 */
internal class GoesPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = GOES)
        event: FieldOptionDiscovered
    ): Just<GoesFieldDiscovered> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        val option = event.option.unpack<GoesOption>()
        val declaringMessage = typeSystem.message(field.declaringType)
        val companionName = option.with
        checkFieldExists(declaringMessage, companionName, field, file)

        val companionField = declaringMessage.field(companionName)
        checkFieldsDistinct(field, companionField, file)
        checkCompanionType(companionField, file)

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS, field, file, GOES)

        return goesFieldDiscovered {
            id = field.ref
            errorMessage = message
            companion = companionField
            subject = field
        }.just()
    }
}

/**
 * A view of a field that is marked with the `(goes)` option.
 */
internal class GoesFieldView : View<FieldRef, GoesField, GoesField.Builder>() {

    @Subscribe
    fun on(e: GoesFieldDiscovered) = alter {
        errorMessage = e.errorMessage
        companion = e.companion
        subject = e.subject
    }
}

private fun checkFieldType(field: Field, file: File) =
    Compilation.check(field.type.isSupported(), file, field.span) {
        "The field type `${field.type.name}` of the `${field.qualifiedName}` field" +
                " is not supported by the `($GOES)` option. Supported field types: messages," +
                " enums, strings, bytes, repeated, and maps."
    }

private fun checkCompanionType(companion: Field, file: File) =
    Compilation.check(companion.type.isSupported(), file, companion.span) {
        "The field type `${companion.type.name}` of the companion `${companion.qualifiedName}`" +
                " field is not supported by the `($GOES)` option."
    }

private fun checkFieldExists(message: MessageType, companion: String, field: Field, file: File) =
    Compilation.check(message.findField(companion) != null, file, field.span) {
        "The message `${message.name.qualifiedName}` does not have `$companion` field" +
                " declared as companion of `${field.name.value}` by the `($GOES)` option."
    }

private fun checkFieldsDistinct(field: Field, companion: Field, file: File) =
    Compilation.check(field != companion, file, field.span) {
        "The `($GOES)` option cannot use the marked field as its own companion." +
                " Self-referencing is prohibited. Please specify another field." +
                " The invalid field: `${field.qualifiedName}`."
    }

/**
 * Tells if this [FieldType] can be validated with the `(goes)` option.
 */
private fun FieldType.isSupported(): Boolean =
    !isPrimitive || primitive in SUPPORTED_PRIMITIVES

private val SUPPORTED_PRIMITIVES = listOf(
    TYPE_STRING, TYPE_BYTES
)

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_TYPE,
    FIELD_VALUE,
    GOES_COMPANION,
    PARENT_TYPE,
)
