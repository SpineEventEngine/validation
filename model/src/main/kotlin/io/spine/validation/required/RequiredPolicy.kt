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
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.plugin.Policy
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.IF_MISSING
import io.spine.validation.OPTION_NAME
import io.spine.validation.REQUIRED
import io.spine.validation.boolValue
import io.spine.validation.defaultMessage
import io.spine.validation.event.RequiredFieldDiscovered
import io.spine.validation.event.requiredFieldDiscovered
import io.spine.validation.fieldId

/**
 * Controls whether a field should be validated as `(required)`.
 *
 * Whenever a filed marked with `(required)` option is discovered,
 * emits [RequiredFieldDiscovered] if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. The option value is `true`.
 *
 * If (1) is violated, the policy reports a compilation error.
 *
 * Violation of (2) means that the `(required)` option is applied correctly,
 * but disabled. In this case, the policy emits [NoReaction] because we actually
 * have a non-required field, marked with `(required)`.
 *
 * Note that this policy is responsible only for fields explicitly marked with
 * the validation option. There are other policies that handle implicitly
 * required fields, i.e., ID fields in entities and signal messages.
 *
 * @see [RequiredIdOptionPolicy]
 * @see [RequiredIdPatternPolicy]
 */
internal class RequiredPolicy : Policy<FieldOptionDiscovered>() {

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

        val message = determineErrorMessage(field)
        return requiredFieldDiscovered {
            id = fieldId {
                type = field.declaringType
                name = field.name
            }
            errorMessage = message
            subject = field
        }.asA()
    }
}

private fun checkFieldType(field: Field, file: File) {
    val type = field.type
    if (type.isPrimitive && type.primitive !in SUPPORTED_PRIMITIVES) {
        Compilation.error(file, field.span) {
            "The field type `${field.type}` of `${field.qualifiedName}` is not supported " +
                    "by the `($REQUIRED)` option."
        }
    }
}

private val SUPPORTED_PRIMITIVES = listOf(
    TYPE_STRING, TYPE_BYTES
)

private fun determineErrorMessage(field: Field): String {
    val companion = field.optionList.find { it.name == IF_MISSING }
    return if (companion == null) {
        IfMissingOption.getDescriptor().defaultMessage
    } else {
        companion.value.unpack<IfMissingOption>()
            .errorMsg
    }
}
