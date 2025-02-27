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

package io.spine.validation.java

import io.spine.base.FieldPath
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.call
import io.spine.protodata.java.field
import io.spine.protodata.java.toBuilder
import io.spine.validate.ConstraintViolation
import io.spine.validation.DistinctField
import io.spine.validation.PATTERN
import io.spine.validation.java.ErrorPlaceholder.FIELD_DUPLICATES
import io.spine.validation.java.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.java.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.java.ValidationCodeInjector.MessageScope.message
import io.spine.validation.java.ValidationCodeInjector.ValidateScope.parentPath

/**
 * The generator for `(distinct)` option.
 *
 * Generates code for a single field represented by the provided [view].
 */
internal class DistinctFieldGenerator(private val view: DistinctField) {

    private val field = view.subject
    private val fieldType = field.type
    private val getter = message.field(field).getter<List<*>>()
    private val declaringType = field.declaringType

    fun generate(): FieldOptionCode {
        val list = when {
            fieldType.isList -> getter
            fieldType.isMap -> getter.call("values")
            else -> error("...")
        }
        val set = ImmutableSetClass.call<Set<*>>("copyOf", list)
        val constraint = CodeBlock(
            """
            if ($list.size() != $set.size()) {
                var duplicates = ${extractDuplicates(list)};
                var fieldPath = ${fieldPath(parentPath)};
                var violation = ${violation(ReadVar("fieldPath"), ReadVar("duplicates"))};
                violations.add(violation);
            }
            """.trimIndent()
        )
        return FieldOptionCode(constraint)
    }

    /**
     * Returns an expression that extracts values occurring multiple times in the given [list].
     *
     * This method uses Guava's [LinkedHashMultisetClass] to ensure that the resulting
     * list preserves the order of element occurrences from the original list.
     */
    private fun extractDuplicates(list: Expression<List<*>>): Expression<List<*>> =
        Expression(
            """
            $LinkedHashMultisetClass.create($list)
                .entrySet()
                .stream()
                .filter(e -> e.getCount() > 1)
                .map($MultiSetEntryClass::getElement)
                .collect($CollectorsClass.toList())
            """.trimIndent()
        )

    private fun fieldPath(parent: Expression<FieldPath>): Expression<FieldPath> =
        parent.toBuilder()
            .chainAdd("field_name", StringLiteral(field.name.value))
            .chainBuild()

    private fun violation(
        fieldPath: Expression<FieldPath>,
        duplicates: Expression<List<*>>
    ): Expression<ConstraintViolation> {
        val qualifiedName = field.qualifiedName
        val placeholders = supportedPlaceholders(fieldPath, duplicates)
        val errorMessage = templateString(view.errorMessage, placeholders, PATTERN, qualifiedName)
        return constraintViolation(errorMessage, declaringType, fieldPath, getter)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        duplicates: Expression<List<*>>
    ): Map<ErrorPlaceholder, Expression<String>> {
        val pathAsString = FieldPathsClass.call<String>("getJoined", fieldPath)
        return mapOf(
            FIELD_PATH to pathAsString,
            FIELD_VALUE to fieldType.stringValueOf(getter),
            FIELD_TYPE to StringLiteral(fieldType.name),
            PARENT_TYPE to StringLiteral(declaringType.qualifiedName),
            FIELD_DUPLICATES to duplicates.call("toString")
        )
    }
}
