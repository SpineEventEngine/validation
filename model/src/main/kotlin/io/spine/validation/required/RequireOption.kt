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
import io.spine.option.RequireOption
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.boolValue
import io.spine.protodata.ast.event.MessageOptionDiscovered
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.ref
import io.spine.protodata.ast.unpack
import io.spine.protodata.check
import io.spine.protodata.plugin.Policy
import io.spine.server.event.Just
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.validation.OPTION_NAME
import io.spine.validation.REQUIRE
import io.spine.validation.REQUIRED
import io.spine.validation.defaultMessage
import io.spine.validation.event.RequireMessageDiscovered
import io.spine.validation.event.RequiredFieldDiscovered
import io.spine.validation.event.requiredFieldDiscovered
import io.spine.validation.required.RequiredFieldSupport.isSupported

/**
 * Controls whether a field should be validated as `(required)`.
 *
 * Whenever a field marked with `(required)` option is discovered, emits
 * [RequiredFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 *
 * If (1) is violated, the policy reports a compilation error.
 *
 * Violation of (2) means that the `(required)` option is applied correctly,
 * but disabled. In this case, the policy emits [NoReaction] because we actually
 * have a non-required field, marked with `(required)`.
 */
// TODO:2025-04-16:yevhenii.nadtochii: Update docs.
internal class RequirePolicy : Policy<MessageOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = REQUIRED)
        event: MessageOptionDiscovered,
    ): Just<RequireMessageDiscovered> {
        val file = event.file
        val option = event.option.unpack<RequireOption>()
        val messageType = event.subject

        val allMessageFields = event.subject.fieldList
        val alternatives = option.fields.split(FIELDS_DELIMITER)
            .map { combination ->
                val messageFields = allMessageFields.map { it.name.value }.toSet()
                val combinationFields = combination.trim()
                    .split(COMBINATION_DELIMITER)
                    .map { it.trim() }
                checkFieldsExist(nonExistentFields, messageType, file)
            }

        checkFieldType(field, file)

        if (!event.option.boolValue) {
            return ignore()
        }

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return requiredFieldDiscovered {
            id = field.ref
            errorMessage = message
            subject = field
        }.asA()
    }
}

/**
 * Separates standalone fields or combinations of fields.
 */
private const val FIELDS_DELIMITER = "|"

/**
 * Separates fields within a combination.
 */
private const val COMBINATION_DELIMITER = "&"

// TODO:2025-04-16:yevhenii.nadtochii: Check for duplicate combinations.
// TODO:2025-04-16:yevhenii.nadtochii: Check for duplicates within a combination.
// TODO:2025-04-16:yevhenii.nadtochii: Cover these cases with tests.

private fun checkFieldsExist(
    messageFields: Set<String>,
    combinationFields: Set<String>,
    message: MessageType,
    file: File
) {
    val nonExistentFields = combinationFields - messageFields
    Compilation.check(nonExistentFields.isEmpty(), file, message.span) {
        "The following fields listed in the `($REQUIRE)` option of" +
                " `${message.name.qualifiedName}` are not declared in the message:" +
                " `${nonExistentFields.joinToString()}`."
    }
}

private fun checkFieldType(field: Field, file: File) =
    Compilation.check(field.type.isSupported(), file, field.span) {
        "The field type `${field.type.name}` of the `${field.qualifiedName}` is not supported" +
                " by the `($REQUIRE)` option. Supported field types: messages, enums," +
                " strings, bytes, repeated, and maps."
    }
