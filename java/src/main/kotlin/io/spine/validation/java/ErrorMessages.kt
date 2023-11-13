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
import io.spine.protobuf.CollectionsConverter
import io.spine.protodata.Field
import io.spine.protodata.TypeName
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.LiteralString
import io.spine.protodata.codegen.java.call
import io.spine.protodata.codegen.java.newBuilder
import io.spine.protodata.isList
import io.spine.protodata.isMap
import io.spine.validate.ConstraintViolation
import io.spine.validation.ErrorMessage

/**
 * Constructs code which creates a [ConstraintViolation] of a simple validation rule and adds it
 * to the mutable list of violations from the given [ctx].
 */
public fun ErrorMessage.createViolation(ctx: GenerationContext): CodeBlock = with(ctx) {
    val violation = buildViolation(
        validatedType, fieldFromSimpleRule, fieldOrElement, ignoreCardinality = isElement
    )
    return addViolation(violation, violationsList)
}

/**
 * Constructs code which creates a [ConstraintViolation] with child violations and adds it
 * to the mutable list of violations from the passed [ctx].
 */
public fun ErrorMessage.createParentViolation(
    ctx: GenerationContext,
    childViolations: Expression
): CodeBlock {
    val field = ctx.simpleRuleField
    val fieldValue = ctx.fieldOrElement!!
    val type = field.declaringType
    val violation = buildViolation(
        type, field, fieldValue, childViolations, ignoreCardinality = ctx.isElement
    )
    return addViolation(violation, ctx.violationsList)
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
 *         the expression to obtain the value of the common field, or `null`,
 *         if there is no common field.
 *         If this parameter is `null`, `field` must also be `null`.
 */
public fun ErrorMessage.createCompositeViolation(
    type: TypeName,
    violationsList: Expression,
    field: Field?,
    fieldValue: Expression?
): CodeBlock {
    require(field != null && fieldValue != null || field == null && fieldValue == null) {
        "Either both `field` and `fieldValue` must be `null` or both must be not `null`." +
                "Got `field` = `$field` and `fieldValue` = `$fieldValue`."
    }
    val violation = buildViolation(type, field, fieldValue)
    return addViolation(violation, violationsList)
}

private fun addViolation(violation: Expression, violationsList: Expression): CodeBlock =
    CodeBlock
        .builder()
        .addStatement("\$L.add(\$L)", violationsList, violation)
        .build()

private fun ErrorMessage.buildViolation(
    type: TypeName,
    field: Field?,
    fieldValue: Expression?,
    childViolations: Expression? = null,
    ignoreCardinality: Boolean = false
): Expression {
    var violationBuilder = ClassName(ConstraintViolation::class.java).newBuilder()
        .chainSet("msg_format", Literal(this))
        .chainSet("type_name", LiteralString(type.typeUrl))
    if (field != null) {
        violationBuilder = violationBuilder.chainSet("field_path", pathOf(field))
    }
    if (fieldValue != null) {
        checkNotNull(field) { "The field value (`$fieldValue`) is set without the field." }
        val packingExpression = when {
            ignoreCardinality -> fieldValue.packToAny()
            field.isList() || field.isMap()  ->
                ClassName(CollectionsConverter::class).call("toAny", listOf(fieldValue))
            else -> fieldValue.packToAny()
        }
        violationBuilder = violationBuilder.chainSet("field_value", packingExpression)
    }
    if (childViolations != null) {
        violationBuilder = violationBuilder.chain("addAllViolation", listOf(childViolations))
    }
    return violationBuilder.chainBuild()
}

private fun pathOf(field: Field): Expression {
    val type = ClassName(FieldPath::class.java)
    return type.newBuilder()
        .chainAdd("field_name", LiteralString(field.name.value))
        .chainBuild()
}
