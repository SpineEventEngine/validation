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
import io.spine.protodata.java.Expression
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.This
import io.spine.protodata.java.call
import io.spine.protodata.java.field
import io.spine.protodata.java.mapExpression
import io.spine.protodata.java.newBuilder
import io.spine.protodata.java.packToAny
import io.spine.protodata.type.TypeSystem
import io.spine.server.query.Querying
import io.spine.server.query.select
import io.spine.validate.ConstraintViolation
import io.spine.validate.TemplateString
import io.spine.validate.checkPlaceholdersHasValue
import io.spine.validation.IF_MISSING
import io.spine.validation.RequiredField
import io.spine.validation.UnsetValue
import io.spine.validation.java.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.java.protodata.CodeBlock
import io.spine.validation.protodata.toBuilder

internal class RequiredGenerator(
    private val querying: Querying,
    typeSystem: TypeSystem
) : MessageStateValidation {

    private val valueConverter = JavaValueConverter(typeSystem)
    private val allViews by lazy {
        querying.select<RequiredField>()
            .all()
    }

    override fun constraints(type: TypeName): List<CodeBlock> =
        allViews.filter { it.id.type == type }
            .map { constraints(it) }

    // How do we know about variables `violations` and `parent`?
    private fun constraints(view: RequiredField): CodeBlock {
        val field = view.subject
        val getter = This<Message>()
            .field(field)
            .getter<Any>()
        val message = view.errorMessage
        return CodeBlock(
            """
            |if ($getter.equals(${defaultValue(field)})) {
            |    var fieldPath = ${filedPath(field.name.value, ReadVar("parent"))};
            |    var violation = ${violation(field, getter, ReadVar("fieldPath"), message)};
            |    violations.add(violation);
            |}
            """.trimMargin()
        )
    }

    private fun defaultValue(field: Field): Expression<*> {
        val unsetValue = UnsetValue.forField(field)!!
        val expression = valueConverter.valueToCode(unsetValue)
        return expression
    }

    private fun filedPath(fieldName: String, parent: Expression<FieldPath>): Expression<FieldPath> =
        parent.toBuilder()
            .chainAdd("field_name", StringLiteral(fieldName))
            .chainBuild()

    private fun violation(
        field: Field,
        getter: Expression<*>,
        path: Expression<FieldPath>,
        message: String
    ): Expression<ConstraintViolation> {
        val placeholders = supportedPlaceholders(field, path)
        val templateString = field.templateString(message, placeholders)
        val violation = ClassName(ConstraintViolation::class).newBuilder()
            .chainSet("message", templateString)
            .chainSet("type_name", StringLiteral(field.declaringType.qualifiedName))
            .chainSet("field_path", path)
            .chainSet("field_value", getter.packToAny())
            .chainBuild<ConstraintViolation>()
        return violation
    }

    private fun Field.templateString(
        template: String,
        placeholders: Map<ErrorPlaceholder, Expression<String>>
    ): Expression<TemplateString> {
        checkPlaceholdersHasValue(template, placeholders.mapKeys { it.key.value }) {
            "The `($IF_MISSING)` option doesn't support the following placeholders: `$it`. " +
                    "The supported placeholders: `${placeholders.keys}`. " +
                    "The declared field: `${qualifiedName}`."
        }
        val placeholderEntries = mapExpression(
            ClassName(String::class), ClassName(String::class),
            placeholders.mapKeys { StringLiteral(it.key.toString()) }
        )
        return ClassName(TemplateString::class).newBuilder()
            .chainSet("withPlaceholders", StringLiteral(template))
            .chainPutAll("placeholderValue", placeholderEntries)
            .chainBuild()
    }

    /**
     * Determines the value for each of the supported `(required)` placeholders.
     */
    private fun supportedPlaceholders(
        field: Field,
        path: Expression<FieldPath>
    ): Map<ErrorPlaceholder, Expression<String>> {
        val pathAsString = ClassName("io.spine.base", "FieldPaths")
            .call<String>("getJoined", path)
        return mapOf(
            FIELD_PATH to pathAsString,
            FIELD_TYPE to StringLiteral(field.type.name),
            PARENT_TYPE to StringLiteral(field.declaringType.qualifiedName)
        )
    }
}
