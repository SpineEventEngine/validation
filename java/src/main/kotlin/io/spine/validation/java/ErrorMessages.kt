/*
 * Copyright 2021, TeamDev. All rights reserved.
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
import io.spine.protodata.Field
import io.spine.protodata.TypeName
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.LiteralString
import io.spine.protodata.isList
import io.spine.protodata.isMap
import io.spine.protodata.typeUrl
import io.spine.validate.ConstraintViolation
import io.spine.validation.ErrorMessage
import io.spine.validation.ListOfAnys
import io.spine.validation.MapOfAnys

/**
 * Constructs code which creates a [ConstraintViolation] of a simple validation rule and adds it
 * to the given mutable [violationsList].
 */
fun ErrorMessage.createViolation(ctx: GenerationContext): CodeBlock = with(ctx) {
    val violation = buildViolation(
        validatedType, fieldFromSimpleRule, fieldOrElement, ignoreCardinality = isElement
    )
    return addViolation(violation, violationsList)
}

/**
 * Constructs code which creates a [ConstraintViolation] with child violations and adds it
 * to the given mutable [violationsList].
 */
fun ErrorMessage.createParentViolation(
    ctx: GenerationContext,
    childViolations: Expression
): CodeBlock {
    val field = ctx.fieldFromSimpleRule!!
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
 */
fun ErrorMessage.createCompositeViolation(type: TypeName, violationsList: String): CodeBlock {
    val violation = buildViolation(type, null, null)
    return addViolation(violation, violationsList)
}

private fun addViolation(violation: Expression, violationsList: String): CodeBlock =
    CodeBlock
        .builder()
        .addStatement("\$N.add(\$L)", violationsList, violation)
        .build()

private fun ErrorMessage.buildViolation(
    type: TypeName,
    field: Field?,
    fieldValue: Expression?,
    childViolations: Expression? = null,
    ignoreCardinality: Boolean = false
): Expression {
    var violationBuilder = ClassName(ConstraintViolation::class.java)
        .newBuilder()
        .chainSet("msg_format", Literal(this))
        .chainSet("type_name", LiteralString(type.typeUrl()))
    if (field != null) {
        violationBuilder = violationBuilder.chainSet("field_path", pathOf(field))
    }
    if (fieldValue != null) {
        checkNotNull(field) { "Field value is set without the field." }
        val packable = when {
            ignoreCardinality -> fieldValue
            field.isList() -> ClassName(ListOfAnys::class).call("from", listOf(fieldValue))
            field.isMap() -> ClassName(MapOfAnys::class).call("from", listOf(fieldValue))
            else -> fieldValue
        }
        violationBuilder = violationBuilder.chainSet("field_value", packable.packToAny())
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
