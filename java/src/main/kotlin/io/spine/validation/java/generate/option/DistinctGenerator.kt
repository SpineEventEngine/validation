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

package io.spine.validation.java.generate.option

import io.spine.base.FieldPath
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.name
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.call
import io.spine.protodata.java.field
import io.spine.server.query.select
import io.spine.string.qualified
import io.spine.string.ti
import io.spine.validate.ConstraintViolation
import io.spine.validation.DistinctField
import io.spine.validation.ErrorPlaceholder
import io.spine.validation.ErrorPlaceholder.FIELD_DUPLICATES
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.PATTERN
import io.spine.validation.api.expression.CollectorsClass
import io.spine.validation.api.expression.ImmutableSetClass
import io.spine.validation.api.expression.LinkedHashMapClass
import io.spine.validation.api.expression.LinkedHashMultisetClass
import io.spine.validation.api.expression.MapClass
import io.spine.validation.api.expression.constraintViolation
import io.spine.validation.api.expression.joinToString
import io.spine.validation.api.expression.orElse
import io.spine.validation.api.expression.resolve
import io.spine.validation.api.expression.stringValueOf
import io.spine.validation.api.expression.stringify
import io.spine.validation.api.generate.MessageScope.message
import io.spine.validation.api.generate.OptionGenerator
import io.spine.validation.api.generate.SingleOptionCode
import io.spine.validation.api.generate.ValidateScope.parentName
import io.spine.validation.api.generate.ValidateScope.parentPath
import io.spine.validation.java.expression.templateString

/**
 * The generator for the `(distinct)` option.
 */
internal class DistinctGenerator : OptionGenerator() {

    /**
     * All `(distinct)` fields in the current compilation process.
     */
    private val allDistinctFields by lazy {
        querying.select<DistinctField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allDistinctFields
            .filter { it.id.type == type }
            .map { GenerateDistinct(it).code() }
}

/**
 * Generates code for a single application of the `(distinct)` option
 * represented by the [view].
 */
private class GenerateDistinct(private val view: DistinctField) {

    private val field = view.subject
    private val fieldType = field.type
    private val fieldAccess = message.field(field)
    private val declaringType = field.declaringType

    /**
     * Returns the generated code.
     */
    fun code(): SingleOptionCode = when {
        fieldType.isList -> {
            val list = fieldAccess.getter<List<*>>()
            val setOfItems = ImmutableSetClass.call<Set<*>>("copyOf", list)
            val constraint = CodeBlock(
                """
                if (!$list.isEmpty() && $list.size() != $setOfItems.size()) {
                    var frequencies = $LinkedHashMultisetClass.create($list);
                    var duplicates = frequencies.elementSet().stream()
                        .filter(e -> frequencies.count(e) > 1)
                        .toList();
                    var fieldPath = ${parentPath.resolve(field.name)};
                    var typeName =  ${parentName.orElse(declaringType)};
                    var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), list, ReadVar<List<*>>("duplicates"))};
                    violations.add(violation);
                }
                """.trimIndent()
            )
            SingleOptionCode(constraint)
        }
        fieldType.isMap -> {
            val map = fieldAccess.getter<Map<*, *>>()
            val mapValues = map.call<Collection<*>>("values")
            val setOfValues = ImmutableSetClass.call<Set<*>>("copyOf", mapValues)
            val constraint = CodeBlock(
                """
                if (!$map.isEmpty() && $mapValues.size() != $setOfValues.size()) {
                    var frequencies = $LinkedHashMultisetClass.create($mapValues);
                    var duplicates = $map.entrySet().stream()
                        .filter(entry -> frequencies.count(entry.getValue()) > 1)
                        .collect($CollectorsClass.toMap(
                            $MapClass.Entry::getKey,
                            $MapClass.Entry::getValue,
                            (v1, v2) -> v1, // We don't expect key duplicates here.
                            $LinkedHashMapClass::new
                        ));
                    var fieldPath = ${parentPath.resolve(field.name)};
                    var typeName =  ${parentName.orElse(declaringType)};
                    var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), map, ReadVar<Map<*, *>>("duplicates"))};
                    violations.add(violation);
                }
                """.trimIndent()
            )
            SingleOptionCode(constraint)
        }
        else -> error(
            """
            The field type `${fieldType.name}` is not supported by `${qualified<DistinctGenerator>()}`.
            Please ensure that the generator supports all field types allowed by its policy.
            """.ti()
        )
    }

    private fun violation(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<io.spine.type.TypeName>,
        fieldValue: Expression<*>,
        duplicates: Expression<*>
    ): Expression<ConstraintViolation> {
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(fieldPath, typeNameStr, fieldValue, duplicates)
        val errorMessage = templateString(view.errorMessage, placeholders, PATTERN)
        return constraintViolation(errorMessage, typeNameStr, fieldPath, fieldValue)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<String>,
        fieldValue: Expression<*>,
        duplicates: Expression<*>
    ): Map<ErrorPlaceholder, Expression<String>> = mapOf(
        FIELD_PATH to fieldPath.joinToString(),
        FIELD_VALUE to fieldType.stringValueOf(fieldValue),
        FIELD_TYPE to StringLiteral(fieldType.name),
        PARENT_TYPE to typeName,
        FIELD_DUPLICATES to fieldType.stringValueOf(duplicates)
    )
}
