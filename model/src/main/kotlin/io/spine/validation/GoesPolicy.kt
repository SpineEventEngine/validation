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

package io.spine.validation

import io.spine.core.External
import io.spine.core.Where
import io.spine.option.GoesOption
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.Span
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.field
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.toPath
import io.spine.protodata.ast.unpack
import io.spine.protodata.plugin.Policy
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.event.GoesFieldDiscovered
import io.spine.validation.event.goesFieldDiscovered

/**
 * A policy to add a validation rule to a type whenever the `(goes)` field option
 * is discovered.
 *
 * This option, when being applied to a target field `A`, declares a dependency to
 * a field `B`. So, whenever `A` is set, `B` also must be set.
 *
 * Upon discovering a field with the mentioned option, the police emits the following
 * composite rule for `A`: `(A isNot Set) OR (B is Set)`.
 *
 * Please note, this police relies on implementation of `required` option to determine
 * whether the field is set. Thus, inheriting its behavior regarding the supported
 * field types and specification about when a field of a specific type is considered
 * to be set.
 */
internal class GoesPolicy : Policy<FieldOptionDiscovered>() {

    // TODO:2025-02-18:yevhenii.nadtochii: Make non-nullable in ProtoData.
    override val typeSystem by lazy { super.typeSystem!! }

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = GOES)
        event: FieldOptionDiscovered
    ): EitherOf2<GoesFieldDiscovered, NoReaction> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        // TODO:2025-02-18:yevhenii.nadtochii: Use a shortcut for `defaultMessage`.
        val option = event.option.unpack<GoesOption>()
        val declaringType = field.declaringType
        val declaringMessage = typeSystem.findMessage(declaringType)!!.first
        val companionName = option.with
        checkFieldExists(declaringMessage, companionName, field, file)

        val companionField = declaringMessage.field(companionName)
        checkDistinct(field, companionField, file)
        checkCompanionType(companionField, file)

        val message = option.errorMsg.ifEmpty { DefaultErrorMessage.from(option.descriptorForType) }
        return goesFieldDiscovered {
            id = field.id()
            errorMessage = message
            companion = companionField
            subject = field
        }.asA()
    }
}

public fun Field.id(): FieldId = fieldId {
    type = declaringType
    name = this@id.name
}

private fun checkFieldType(field: Field, file: File) {
    val type = field.type
    if (type.isPrimitive && type.primitive !in SUPPORTED_PRIMITIVES) {
        compilationError(file, field.span) {
            "The field type `${field.type}` of the `${field.qualifiedName}` field " +
                    "is not supported by the `($GOES)` option."
        }
    }
}

private fun checkCompanionType(field: Field, file: File) {
    val type = field.type
    if (type.isPrimitive && type.primitive !in SUPPORTED_PRIMITIVES) {
        compilationError(file, field.span) {
            "The field type `${field.type}` of the companion `${field.qualifiedName}` field " +
                    "is not supported by the `($GOES)` option."
        }
    }
}

private fun checkFieldExists(message: MessageType, companion: String, field: Field, file: File) {
    if (message.fieldList.find { it.name.value == companion } == null) {
        compilationError(file, field.span) {
            "The message `${message.name.qualifiedName}` does not have `$companion` field " +
                    "declared as companion of `${field.name.value}` by the `($GOES)` option."
        }
    }
}

/**
 * Checks that the given [field] and its [companion] are distinct fields.
 */
private fun checkDistinct(field: Field, companion: Field, file: File) {
    if (field == companion) {
        compilationError(file, field.span) {
            "The `($GOES)` option can not use the marked field as its own companion. " +
                    "Self-referencing is prohibited. Please specify another field. " +
                    "The invalid field: `${field.qualifiedName}`."
        }
    }
}

private val SUPPORTED_PRIMITIVES = listOf(
    TYPE_STRING, TYPE_BYTES
)

private fun compilationError(file: File, span: Span, message: () -> String): Nothing =
    Compilation.error(
        file.toPath().toFile(),
        span.startLine, span.startColumn,
        message()
    )
