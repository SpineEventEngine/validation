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

package io.spine.validation.java.setonce

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.mapExpression
import io.spine.protodata.java.newBuilder
import io.spine.protodata.java.packToAny
import io.spine.validate.ConstraintViolation
import io.spine.validate.ErrorPlaceholder
import io.spine.validate.ErrorPlaceholder.FIELD_NAME
import io.spine.validate.ErrorPlaceholder.FIELD_PROPOSED_VALUE
import io.spine.validate.ErrorPlaceholder.FIELD_TYPE
import io.spine.validate.ErrorPlaceholder.FIELD_VALUE
import io.spine.validate.ErrorPlaceholder.PARENT_TYPE
import io.spine.validate.TemplateString
import io.spine.validate.checkPlaceholdersHasValue
import io.spine.validation.IF_SET_AGAIN

/**
 * Builds a [ConstraintViolation] instance for the given field.
 *
 * @param errorTemplate The error message template.
 * @param field The field, which was attempted to set twice.
 */
internal class SetOnceConstraintViolation(
    private val errorTemplate: String,
    field: Field
) {

    private val fieldName = field.name.value
    private val fieldType = field.type.name
    private val qualifiedName = field.qualifiedName
    private val declaringMessage = field.declaringType.qualifiedName

    /**
     * Builds an expression that returns a new instance of [ConstraintViolation]
     * with the given values.
     *
     * @param currentValue The current field value as string.
     * @param newValue The proposed new value as string.
     * @param payload The violated value to be packed as Protobuf `Any` and set for
     *  [ConstraintViolation.getFieldValue] property. Usually, it is just [newValue].
     *  But for some field types, some conversion may take place.
     */
    fun withValues(
        currentValue: Expression<String>,
        newValue: Expression<String>,
        payload: Expression<*> = newValue
    ): Expression<ConstraintViolation> {
        val placeholders = supportedPlaceholders(currentValue, newValue)
        val message = templateString(errorTemplate, placeholders)
        val violation = ClassName(ConstraintViolation::class).newBuilder()
            .chainSet("message", message)
            .chainSet("type_name", StringLiteral(declaringMessage))
            .chainSet("field_name", StringLiteral(fieldName))
            .chainSet("field_value", payload.packToAny())
            .chainBuild<ConstraintViolation>()
        return violation
    }

    private fun templateString(
        template: String,
        placeholders: Map<ErrorPlaceholder, Expression<String>>
    ): Expression<TemplateString> {
        checkPlaceholdersHasValue(template, placeholders.mapKeys { it.key.value }) {
            "The `($IF_SET_AGAIN)` option doesn't support the following placeholders: `$it`. " +
                    "The supported placeholders: `${placeholders.keys}`. " +
                    "The declared field: `${qualifiedName}`."
        }
        val placeholderEntries = mapExpression(
            ClassName(String::class), ClassName(String::class),
            placeholders.mapKeys { StringLiteral(it.key.toString()) }
        )
        return ClassName(TemplateString::class).newBuilder()
            .chainSet("withPlaceholders", StringLiteral(errorTemplate))
            .chainPutAll("placeholderValue", placeholderEntries)
            .chainBuild()
    }

    /**
     * Determines the value for each of the supported `(set_once)` placeholders.
     */
    private fun supportedPlaceholders(
        currentValue: Expression<String>,
        newValue: Expression<String>
    ) = mapOf(
        FIELD_NAME to StringLiteral(fieldName),
        FIELD_TYPE to StringLiteral(fieldType),
        FIELD_VALUE to currentValue,
        FIELD_PROPOSED_VALUE to newValue,
        PARENT_TYPE to StringLiteral(declaringMessage)
    )
}
