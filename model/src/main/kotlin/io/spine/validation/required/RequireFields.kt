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
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.check
import io.spine.validation.FieldCombination
import io.spine.validation.REQUIRE
import io.spine.validation.fieldCombination
import io.spine.validation.required.RequiredFieldSupport.isSupported

/**
 * Parses and validates field combinations specified in the given [RequireOption].
 *
 * The class ensures the following:
 *
 * 1. Each specified field is present in the [message] type.
 * 2. Each field has a [compatible][RequiredFieldSupport] field type.
 * 3. Each combination has a unique set of fields.
 * 4. Each combination is unique.
 *
 * In case of violations, the class reports a compilation error.
 */
internal class RequireFields(
    option: RequireOption,
    private val message: MessageType,
    private val file: File
) {
    private val specifiedFields = option.fields
    private val messageFields = message.fieldList
    private val messageFieldNames = messageFields
        .map { it.name.value }
        .toSet()

    /**
     * Returns this [RequireFields] as a list of [FieldCombination].
     */
    fun toCombinations(): List<FieldCombination> {
        val combinations = specifiedFields.split(FIELDS_DELIMITER)
            .map(::combinationFieldNames)
            .run { checkCombinationsUnique(this) }
        return combinations.map {
            val astFields = it.toAstFields()
                .onEach(::checkFieldType)
            fieldCombination {
                field.addAll(astFields)
            }
        }
    }

    private fun combinationFieldNames(combination: String): Set<String> {
        val fieldNames = combination.trim()
            .split(COMBINATION_DELIMITER)
            .map { it.trim() }
            .run { checkFieldsUnique(this) }
        fieldNames.forEach(::checkFieldExists)
        return fieldNames
    }

    private fun Set<String>.toAstFields() = map { fieldName ->
        messageFields.first { messageField ->
            messageField.name.value == fieldName
        }
    }

    private fun checkCombinationsUnique(combinations: List<Set<String>>): Set<Set<String>> {
        val duplicates = combinations.groupBy { it }
            .filter { it.value.size > 1 }
            .map { it.key }
        Compilation.check(duplicates.isEmpty(), file, message.span) {
            "The following combinations of fields listed in the `($REQUIRE)` option of" +
                    " `${message.name.qualifiedName}` appear more than once:" +
                    " `$duplicates`."
        }
        return combinations.toSet()
    }

    private fun checkFieldsUnique(fieldNames: List<String>): Set<String> {
        val duplicates = fieldNames.groupBy { it }
            .filter { it.value.size > 1 }
            .map { it.key }
        Compilation.check(duplicates.isEmpty(), file, message.span) {
            "The following fields listed in the `($REQUIRE)` option of" +
                    " `${message.name.qualifiedName}` appear more than once within" +
                    " a single combination: `$duplicates`."
        }
        return fieldNames.toSet()
    }

    private fun checkFieldExists(fieldName: String) =
        Compilation.check(messageFieldNames.contains(fieldName), file, message.span) {
            "The `$fieldName` listed in the `($REQUIRE)` option of" +
                    " `${message.name.qualifiedName}` is not declared in the message."
        }

    private fun checkFieldType(field: Field) =
        Compilation.check(field.type.isSupported(), file, message.span) {
            "The field type `${field.type.name}` of the `${field.qualifiedName}` is not supported" +
                    " by the `($REQUIRE)` option. Supported field types: messages, enums," +
                    " strings, bytes, repeated, and maps."
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
