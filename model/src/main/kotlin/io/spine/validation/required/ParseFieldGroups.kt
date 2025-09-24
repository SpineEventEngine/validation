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

import io.spine.option.RequireOption
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.check
import io.spine.validation.FieldGroup
import io.spine.validation.REQUIRE
import io.spine.validation.fieldGroup
import io.spine.validation.required.RequiredFieldSupport.isSupported

/**
 * Parses and validates field groups specified in the given [RequireOption].
 *
 * The class ensures the following conditions:
 *
 * 1. Each specified field is declared in the [message] type.
 * 2. Each field has a [compatible][RequiredFieldSupport] field type.
 * 3. Each field group has a unique set of fields.
 * 4. Each field group is unique.
 *
 * A compilation error is reported in case of violation of any condition.
 */
internal class ParseFieldGroups(
    option: RequireOption,
    private val message: MessageType,
    private val file: File
) {
    private val groupsDefinition = option.fields
    private val messageFields = message.fieldList
    private val messageFieldNames by lazy {
        messageFields
            .map { it.name.value }
            .toSet()
    }

    /**
     * A list of parsed [FieldGroup]s.
     */
    val result by lazy {
        groupsDefinition.split(GROUPS_DELIMITER)
            .map(::toRawFieldGroup)
            .run { checkGroupsAreUnique(this) }
            .map {
                val astFields = it.fields
                    .map(::toAstField)
                    .onEach(::checkFieldType)
                fieldGroup {
                    field.addAll(astFields)
                }
            }
    }

    private fun toRawFieldGroup(definition: String): RawFieldGroup {
        val trimmedDefinition = definition.trim()
        val fieldNames = trimmedDefinition
            .split(FIELDS_DELIMITER)
            .map { it.trim() }
            .run { checkFieldsAreUnique(fieldNames = this, definition) }
        fieldNames.forEach(::checkFieldExists)
        return RawFieldGroup(trimmedDefinition, fieldNames)
    }

    private fun toAstField(fieldName: String) =
        messageFields.first { messageField ->
            messageField.name.value == fieldName
        }

    private fun checkGroupsAreUnique(groups: List<RawFieldGroup>): Set<RawFieldGroup> {
        val duplicates = groups.groupBy { it.fields }
            .filter { it.value.size > 1 }
            .flatMap { it.value }
            .map { it.definition }
            .toSet()
        Compilation.check(duplicates.isEmpty(), file, message.span) {
            "The following groups of fields listed in the `($REQUIRE)` option of" +
                    " `${message.name.qualifiedName}` appear more than once:" +
                    " `$duplicates`."
        }
        return groups.toSet()
    }

    private fun checkFieldsAreUnique(
        fieldNames: List<String>,
        groupDefinition: String
    ): Set<String> {
        val duplicates = fieldNames.groupBy { it }
            .filter { it.value.size > 1 }
            .map { it.key }
        Compilation.check(duplicates.isEmpty(), file, message.span) {
            "The `$duplicates` fields listed in the `($REQUIRE)` option of" +
                    " `${message.name.qualifiedName}` appear more than once within" +
                    " the `$groupDefinition` field group."
        }
        return fieldNames.toSet()
    }

    private fun checkFieldExists(fieldName: String) =
        Compilation.check(messageFieldNames.contains(fieldName), file, message.span) {
            "The `$fieldName` field listed in the `($REQUIRE)` option of" +
                    " `${message.name.qualifiedName}` is not declared in the message."
        }

    private fun checkFieldType(field: Field) =
        Compilation.check(field.type.isSupported(), file, message.span) {
            "The field type `${field.type.name}` of the `${field.qualifiedName}` is not supported" +
                    " by the `($REQUIRE)` option. Supported field types: messages, enums," +
                    " strings, bytes, repeated, and maps."
        }

    private companion object {

        /**
         * Separates field groups.
         */
        const val GROUPS_DELIMITER = "|"

        /**
         * Separates fields within a group.
         */
        const val FIELDS_DELIMITER = "&"
    }
}

/**
 * A field group definition along with the names of the fields it includes.
 *
 * This class is used during the validation phase of the parsing.
 *
 * @property definition The field group as was specified by a user.
 * @property fields The contained field names.
 */
private class RawFieldGroup(
    val definition: String,
    val fields: Set<String>,
)
