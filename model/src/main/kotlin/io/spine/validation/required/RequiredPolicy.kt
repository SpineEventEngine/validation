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

package io.spine.validation.required

import io.spine.core.External
import io.spine.core.Where
import io.spine.option.IfMissingOption
import io.spine.protobuf.unpack
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.Span
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.toPath
import io.spine.protodata.plugin.Policy
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.DefaultErrorMessage
import io.spine.validation.IF_MISSING
import io.spine.validation.OPTION_NAME
import io.spine.validation.REQUIRED
import io.spine.validation.ValidationPolicy
import io.spine.validation.boolValue
import io.spine.validation.event.RequiredFieldAccepted
import io.spine.validation.event.requiredFieldAccepted
import io.spine.validation.fieldId

/**
 * A [ValidationPolicy] which controls whether a field should be validated as `(required)`.
 *
 * Whenever a required field is discovered, emit [RequiredFieldAccepted]
 * if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. The field has zero or exactly one [IF_MISSING] companion option declared.
 * 3. The value of the option is `true`.
 *
 * Violation of the first or second condition leads to a compilation error.
 *
 * Violation of the third condition means that the `(required)` option
 * is applied correctly, but disabled. In this case, the policy emits [NoReaction]
 * because we actually have a non-required field.
 */
internal class RequiredPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = REQUIRED)
        event: FieldOptionDiscovered,
    ): EitherOf2<RequiredFieldAccepted, NoReaction> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)
        val message = determineErrorMessage(field, file)
        return if (event.option.boolValue) {
            accepted(field, message).asA()
        } else {
            ignore()
        }
    }
}

private fun accepted(field: Field, message: String): RequiredFieldAccepted =
    requiredFieldAccepted {
        id = fieldId {
            type = field.declaringType
            name = field.name
        }
        errorMessage = message
        subject = field
    }

private fun checkFieldType(field: Field, file: File) {
    val type = field.type
    if (type.isPrimitive && type.primitive !in SUPPORTED_PRIMITIVES) {
        file.compilationError(field.span) {
            "The field `${field.qualifiedName}` of the type `${field.type}` does not " +
                    "support `($REQUIRED)` option."
        }
    }
}

// TODO:2025-01-31:yevhenii.nadtochii: Locally changed ProtoData.
//  `Field.optionList` is empty when it is part of `FieldOptionDiscovered` event.
private fun determineErrorMessage(field: Field, file: File): String {
    val ifMissingOptions = field.optionList.filter { it.name == IF_MISSING }
    return when (ifMissingOptions.size) {
        0 -> DefaultErrorMessage.from(IfMissingOption.getDescriptor())
        1 -> {
            val companion = ifMissingOptions.first().value.unpack<IfMissingOption>()
            companion.errorMsg
        }

        else -> {
            file.compilationError(field.span) {
                "The field `${field.qualifiedName}` is allowed to have zero or one " +
                        "`($IF_MISSING)` companion option."
            }
        }
    }
}

private fun File.compilationError(span: Span, message: () -> String): Nothing =
    Compilation.error(
        toPath().toFile(),
        span.startLine, span.startColumn,
        message()
    )

private val SUPPORTED_PRIMITIVES = listOf(
    TYPE_STRING, TYPE_BYTES
)
