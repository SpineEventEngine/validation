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

@file:JvmName("ConstraintViolations")

package io.spine.validation.java.violation

import io.spine.base.FieldPath
import io.spine.protobuf.restoreProtobufEscapes
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.mapExpression
import io.spine.protodata.java.newBuilder
import io.spine.protodata.java.packToAny
import io.spine.validate.ConstraintViolation
import io.spine.validate.TemplateString
import io.spine.validate.checkPlaceholdersHasValue
import io.spine.validation.java.expression.StringClass
import io.spine.validation.java.expression.TemplateStringClass

/**
 * Yields an expression that creates a new instance of [ConstraintViolation]
 * with the given parameters.
 *
 * @param errorMessage The error message template string.
 * @param typeName The message type being validated. In the case of in-depth validation,
 *  contains the root message name.
 * @param fieldPath The path to the field containing an invalid value, if any.
 *   For example, the `(require)` option uses `null` for this parameter because
 *   it is a message-wide option.
 * @param fieldValue The field value that violated the constraint, if any.
 *   For example, the `(required)` option uses `null` for this parameter because
 *   the invalid field value for this option is the field type's default value,
 *   which is treated as "no value" at all.
 */
public fun constraintViolation(
    errorMessage: Expression<TemplateString>,
    typeName: Expression<String>,
    fieldPath: Expression<FieldPath>?,
    fieldValue: Expression<*>?
): Expression<ConstraintViolation> {
    var builder = ClassName(ConstraintViolation::class).newBuilder()
        .chainSet("message", errorMessage)
        .chainSet("type_name", typeName)
    fieldPath?.let {
        builder = builder.chainSet("field_path", fieldPath)
    }
    fieldValue?.let {
        builder = builder.chainSet("field_value", fieldValue.packToAny())
    }
    return builder.chainBuild()
}

/**
 * Yields an expression that creates a new instance of [TemplateString]
 * with the given parameters.
 *
 * Pass placeholder keys either as [ErrorPlaceholder] instances of plain `String`.
 *
 * @param placeholders The supported placeholders and their values.
 * @param optionName The name of the option, which declared the provided [placeholders].
 */
@Suppress("Unchecked_Cast")
public inline fun <reified T : Any> templateString(
    template: String,
    placeholders: Map<T, Expression<String>>,
    optionName: String
): Expression<TemplateString> = when (T::class) {
    String::class -> {
        val typedPlaceholders = placeholders as Map<String, Expression<String>>
        withStringPlaceholders(template, typedPlaceholders, optionName)
    }
    ErrorPlaceholder::class -> {
        val typedPlaceholders = placeholders as Map<ErrorPlaceholder, Expression<String>>
        withEnumPlaceholders(template, typedPlaceholders, optionName)
    }
    else -> error(
        "Unsupported error placeholder type: `${T::class}`. The supported types are `String`" +
                " and `io.spine.validation.java.violation.ErrorPlaceholder`."
    )
}

/**
 * Yields an expression that creates a new instance of [TemplateString]
 * using [ErrorPlaceholder] for key values.
 *
 * Note that the method is kept public because it is invoked from
 * the inlined [templateString].
 */
public fun withEnumPlaceholders(
    template: String,
    placeholders: Map<ErrorPlaceholder, Expression<String>>,
    optionName: String
): Expression<TemplateString> =
    templateString(template, placeholders.mapKeys { it.key.value }, optionName)

/**
 * Yields an expression that creates a new instance of [TemplateString]
 * using [String] for key values.
 *
 * Note that the method is kept public because it is invoked from
 * the inlined [templateString].
 */
public fun withStringPlaceholders(
    template: String,
    placeholders: Map<String, Expression<String>>,
    optionName: String
): Expression<TemplateString> {
    checkPlaceholdersHasValue(template, placeholders) { missingKeys ->
        "Unexpected error message placeholders `$missingKeys` specified for the `($optionName)`" +
                " option. The available placeholders: `${placeholders.keys}`. Please make sure" +
                " that the policy that verifies the message placeholders and its code generator" +
                " operate with the same set of placeholders."
    }
    val placeholderEntries = mapExpression(
        StringClass, StringClass,
        placeholders.mapKeys { StringLiteral(it.key) }
    )
    val escapedTemplate = restoreProtobufEscapes(template)
    return TemplateStringClass.newBuilder()
        .chainSet("withPlaceholders", StringLiteral(escapedTemplate))
        .chainPutAll("placeholderValue", placeholderEntries)
        .chainBuild()
}
