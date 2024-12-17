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

import io.spine.base.FieldPath
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.listExpression
import io.spine.protodata.java.newBuilder
import io.spine.protodata.java.packToAny
import io.spine.validate.ConstraintViolation
import io.spine.validation.IF_SET_AGAIN
import io.spine.validation.java.placeholder.Placeholder
import io.spine.validation.java.placeholder.PlaceholderParser

/**
 * Builds a [ConstraintViolation] instance for the given field.
 *
 * @param errorMessage The error message pattern.
 * @param field The field, which was attempted to set twice.
 */
internal class SetOnceConstraintViolation(
    private val errorMessage: String,
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
        val parser = placeholderParser(currentValue, newValue)
        val (format, params) = parser.toPrintfString(errorMessage)
        val fieldPath = ClassName(FieldPath::class).newBuilder()
            .chainAdd("field_name", StringLiteral(fieldName))
            .chainBuild<FieldPath>()
        val violation = ClassName(ConstraintViolation::class).newBuilder()
            .chainSet("msg_format", StringLiteral(format))
            .chainAddAll("param", listExpression(params))
            .chainSet("type_name", StringLiteral(declaringMessage))
            .chainSet("field_path", fieldPath)
            .chainSet("field_value", payload.packToAny())
            .chainBuild<ConstraintViolation>()
        return violation
    }

    /**
     * Determines the value for each of the supported tokens.
     */
    private fun placeholderParser(currentValue: Expression<String>, newValue: Expression<String>) =
        PlaceholderParser(
            mapOf(
                "field.name" to StringLiteral(fieldName),
                "field.type" to StringLiteral(fieldType),
                "field.value" to currentValue,
                "field.proposed_value" to newValue,
                "parent.type" to StringLiteral(declaringMessage)
            ), ::onUnsupportedPlaceholder
        )

    private fun onUnsupportedPlaceholder(
        name: Placeholder,
        supported: Set<Placeholder>
    ): Nothing {
        val supportedPlaceholders = supported.joinToString { "{$it}" }
        throw IllegalArgumentException(
            "The `($IF_SET_AGAIN)` option doesn't support the token: `{$name}`. " +
                    "The supported tokens: `$supportedPlaceholders`. " +
                    "The declared field: `$qualifiedName`."
        )
    }
}
