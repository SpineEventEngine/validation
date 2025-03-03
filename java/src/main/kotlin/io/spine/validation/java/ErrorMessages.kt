/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

@file:JvmName("ErrorMessages")

package io.spine.validation.java

import com.squareup.javapoet.CodeBlock
import io.spine.base.FieldPath
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.Literal
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.newBuilder
import io.spine.protodata.java.packToAny
import io.spine.validate.ConstraintViolation
import io.spine.validate.TemplateString
import io.spine.validation.ErrorMessage
import io.spine.validation.java.expression.TemplateStringClass

/**
 * Constructs code which creates a [ConstraintViolation] of a simple validation rule and adds it
 * to the mutable list of violations from the given [ctx].
 */
public fun ErrorMessage.createViolation(ctx: GenerationContext): CodeBlock = with(ctx) {
    val violation = buildViolation(validatedType, fieldFromSimpleRule, fieldOrElement)
    return addViolation(violation, violationList)
}

/**
 * Constructs code which creates a [ConstraintViolation] of a composite validation rule and adds
 * it to the given mutable [violationsList].
 *
 * @param type
 *         a name of the validated message type.
 * @param violationsList
 *         a code reference to a list of violations.
 * @param field
 *         field that is common to all the simple rules that constitute the associated
 *         composite rule, or `null` if no such field exists.
 *         If this param is `null`, `fieldValue` must also be `null`.
 * @param fieldValue
 *         the expression to get the value of the common field, or `null`,
 *         if there is no common field.
 *         If this parameter is `null`, `field` must also be `null`.
 */
public fun ErrorMessage.createCompositeViolation(
    type: TypeName,
    violationsList: Expression<MutableList<ConstraintViolation>>,
    field: Field?,
    fieldValue: Expression<*>?
): CodeBlock {
    require(field != null && fieldValue != null || field == null && fieldValue == null) {
        "Either both `field` and `fieldValue` must be `null` or both must be not `null`." +
                "Got `field` = `$field` and `fieldValue` = `$fieldValue`."
    }
    val violation = buildViolation(type, field, fieldValue)
    return addViolation(violation, violationsList)
}

private fun addViolation(
    violation: Expression<ConstraintViolation>,
    violationsList: Expression<MutableList<ConstraintViolation>>
): CodeBlock = CodeBlock.builder()
    .addStatement("\$L.add(\$L)", violationsList, violation)
    .build()

private fun ErrorMessage.buildViolation(
    type: TypeName,
    field: Field?,
    fieldValue: Expression<*>?,
): Expression<ConstraintViolation> {
    val message = TemplateStringClass.newBuilder()
        .chainSet("withPlaceholders", Literal(this))
        .chainBuild<TemplateString>()
    var violationBuilder = ClassName(ConstraintViolation::class).newBuilder()
        .chainSet("message", message)
        .chainSet("type_name", StringLiteral(type.typeUrl))
    if (field != null) {
        violationBuilder = violationBuilder.chainSet("field_path", pathOf(field))
    }
    if (fieldValue != null) {
        checkNotNull(field) { "The field value (`$fieldValue`) is set without the field." }
        val packingExpression = fieldValue.packToAny()
        violationBuilder = violationBuilder.chainSet("field_value", packingExpression)
    }
    return violationBuilder.chainBuild()
}

private fun pathOf(field: Field): Expression<FieldPath> =
    ClassName(FieldPath::class.java).newBuilder()
        .chainAdd("field_name", StringLiteral(field.name.value))
        .chainBuild()
