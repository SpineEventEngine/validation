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

package io.spine.validation.java

import com.google.protobuf.Message
import io.spine.base.FieldPath
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.This
import io.spine.protodata.java.call
import io.spine.protodata.java.field
import io.spine.protodata.java.toBuilder
import io.spine.server.query.Querying
import io.spine.server.query.select
import io.spine.validate.ConstraintViolation
import io.spine.validation.IF_MISSING
import io.spine.validation.RequiredField
import io.spine.validation.UnsetValue
import io.spine.validation.java.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.ErrorPlaceholder.PARENT_TYPE

/**
 * The generator for `(required)` option.
 */
internal class RequiredOptionGenerator(
    private val querying: Querying,
    private val converter: JavaValueConverter
) : OptionGenerator {

    /**
     * All required fields in the current compilation process.
     */
    private val allRequiredFields by lazy {
        querying.select<RequiredField>()
            .all()
    }

    override fun codeFor(
        type: TypeName,
        parent: Expression<FieldPath>,
        violations: Expression<MutableList<ConstraintViolation>>
    ): MessageOptionCode {
        val requiredFields = allRequiredFields.filter { it.id.type == type }
        val constraints = requiredFields.map { constraints(it, parent, violations) }
        return MessageOptionCode(constraints)
    }

    private fun constraints(
        view: RequiredField,
        parent: Expression<FieldPath>,
        violations: Expression<List<ConstraintViolation>>
    ): CodeBlock {
        val field = view.subject
        val getter = This<Message>()
            .field(field)
            .getter<Any>()
        val message = view.errorMessage
        return CodeBlock(
            """
            if ($getter.equals(${defaultValue(field)})) {
                var fieldPath = ${fieldPath(field.name.value, parent)};
                var violation = ${violation(field, ReadVar("fieldPath"), message)};
                $violations.add(violation);
            }
            """.trimIndent()
        )
    }

    /**
     * Returns the expression that yields a default value for the given field.
     *
     * Each field type has its own default value. The option considers the field
     * to be missing if its current value equals to the default one.
     */
    private fun defaultValue(field: Field): Expression<*> {
        val unsetValue = UnsetValue.forField(field)!!
        val expression = converter.valueToCode(unsetValue)
        return expression
    }

    private fun fieldPath(fieldName: String, parent: Expression<FieldPath>): Expression<FieldPath> =
        parent.toBuilder()
            .chainAdd("field_name", StringLiteral(fieldName))
            .chainBuild()

    private fun violation(
        field: Field,
        fieldPath: Expression<FieldPath>,
        message: String
    ): Expression<ConstraintViolation> {
        val placeholders = supportedPlaceholders(field, fieldPath)
        val errorMessage = templateString(message, placeholders, IF_MISSING, field.qualifiedName)
        return constraintViolation(errorMessage, field.declaringType, fieldPath)
    }

    /**
     * Determines the value for each of the supported `(if_missing)` placeholders.
     *
     * Note: `FieldPaths` is a synthetic Java class, which contains Kotlin extensions
     * declared for [FieldPath]. It is available from Java, but not from Kotlin.
     * So, we specify it as a string literal here.
     */
    private fun supportedPlaceholders(
        field: Field,
        fieldPath: Expression<FieldPath>
    ): Map<ErrorPlaceholder, Expression<String>> {
        val pathAsString = ClassName("io.spine.base", "FieldPaths")
            .call<String>("getJoined", fieldPath)
        return mapOf(
            FIELD_PATH to pathAsString,
            FIELD_TYPE to StringLiteral(field.type.name),
            PARENT_TYPE to StringLiteral(field.declaringType.qualifiedName)
        )
    }
}
