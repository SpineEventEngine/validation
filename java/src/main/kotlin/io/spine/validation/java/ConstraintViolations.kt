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

package io.spine.validation.java

import io.spine.base.FieldPath
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.mapExpression
import io.spine.protodata.java.newBuilder
import io.spine.protodata.java.packToAny
import io.spine.validate.ConstraintViolation
import io.spine.validate.TemplateString
import io.spine.validate.checkPlaceholdersHasValue

/**
 * Yields an expression that creates a new instance of [ConstraintViolation]
 * with the given parameters.
 *
 * @param errorMessage The error message template string.
 * @param declaringType The message type being validated.
 * @param fieldPath The path to the field containing an invalid value.
 * @param fieldValue The field value that violated the constraint, if any.
 *   For example, the `(required)` option uses `null` for this parameter because
 *   the invalid field value for this option is the field type's default value,
 *   which is treated as "no value" at all.
 */
public fun constraintViolation(
    errorMessage: Expression<TemplateString>,
    declaringType: TypeName,
    fieldPath: Expression<FieldPath>,
    fieldValue: Expression<*>? = null
): Expression<ConstraintViolation> {
    var builder = ClassName(ConstraintViolation::class).newBuilder()
        .chainSet("message", errorMessage)
        .chainSet("type_name", StringLiteral(declaringType.qualifiedName))
        .chainSet("field_path", fieldPath)
    fieldValue?.let {
        builder = builder.chainSet("field_value", fieldValue.packToAny())
    }
    return builder.chainBuild()
}

/**
 * Yields an expression that creates a new instance of [TemplateString]
 * with the given parameters.
 *
 * @param template The template string that may have one or more placeholders.
 * @param placeholders The supported placeholders and their values.
 * @param optionName The name of the option, which declared the provided [placeholders].
 * @param fieldName The fully qualified name of the field, which passed the provided [template].
 */
public fun templateString(
    template: String,
    placeholders: Map<ErrorPlaceholder, Expression<String>>,
    optionName: String,
    fieldName: String
): Expression<TemplateString> {
    checkPlaceholdersHasValue(template, placeholders.mapKeys { it.key.value }) {
        "The `($optionName)` option doesn't support the following placeholders: `$it`. " +
                "The supported placeholders: `${placeholders.keys}`. " +
                "The declared field: `${fieldName}`."
    }
    val placeholderEntries = mapExpression(
        StringClass, StringClass,
        placeholders.mapKeys { StringLiteral(it.key.toString()) }
    )
    return TemplateStringClass.newBuilder()
        .chainSet("withPlaceholders", StringLiteral(template))
        .chainPutAll("placeholderValue", placeholderEntries)
        .chainBuild()
}
