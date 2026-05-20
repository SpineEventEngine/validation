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

package io.spine.tools.validation.java.generate.option

import io.spine.base.FieldPath
import io.spine.server.query.select
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.isMap
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.jvm.CodeBlock
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.JavaValueConverter
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.jvm.field
import io.spine.tools.validation.RequiredField
import io.spine.tools.validation.java.expression.EmptyFieldCheck
import io.spine.tools.validation.java.expression.constraintViolation
import io.spine.tools.validation.java.expression.joinToString
import io.spine.tools.validation.java.expression.orElse
import io.spine.tools.validation.java.expression.resolve
import io.spine.tools.validation.java.expression.stringify
import io.spine.tools.validation.java.expression.templateString
import io.spine.tools.validation.java.generate.MessageScope.message
import io.spine.tools.validation.java.generate.OptionGeneratorWithConverter
import io.spine.tools.validation.java.generate.SingleOptionCode
import io.spine.tools.validation.java.generate.ValidateScope.parentName
import io.spine.tools.validation.java.generate.ValidateScope.parentPath
import io.spine.tools.validation.java.generate.ValidateScope.violations
import io.spine.tools.validation.option.IF_MISSING
import io.spine.validation.ConstraintViolation
import io.spine.string.Placeholder
import io.spine.validation.StandardPlaceholder.FIELD_PATH
import io.spine.validation.StandardPlaceholder.FIELD_TYPE
import io.spine.validation.StandardPlaceholder.PARENT_TYPE

/**
 * The generator for `(required)` option.
 */
internal class RequiredGenerator : OptionGeneratorWithConverter() {

    /**
     * All `(required)` fields in the current compilation process.
     */
    private val allRequiredFields by lazy {
        querying.select<RequiredField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allRequiredFields
            .filter { it.id.type == type }
            .map { GenerateRequired(it, converter).code() }
}

/**
 * Generates code for a single application of the `(required)` option
 * represented by the [view].
 */
private class GenerateRequired(
    private val view: RequiredField,
    override val converter: JavaValueConverter
) : EmptyFieldCheck {

    private val field = view.subject
    private val declaringType = field.declaringType

    /**
     * Returns the generated code.
     */
    fun code(): SingleOptionCode {
        val constraint = CodeBlock(
            """
            if (${missingCondition()}) {
                var fieldPath = ${parentPath.resolve(field.name)};
                var typeName =  ${parentName.orElse(declaringType)};
                var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"))};
                $violations.add(violation);
            }
            """.trimIndent()
        )
        return SingleOptionCode(constraint)
    }

    /**
     * Returns the boolean expression that evaluates to `true` when the field
     * is considered missing.
     *
     * For a `map<K, string>` field, an entry with an empty-string value is also
     * treated as missing — analogously to how an empty string in a `(required)`
     * `string` field is rejected.
     *
     * For a `map<K, M>` field with a message-typed value, an entry whose value is
     * the default instance of `M` is treated as missing — analogously to how the
     * default instance is rejected for a singular `(required)` message field.
     */
    private fun missingCondition(): Expression<Boolean> {
        val isDefault = field.hasDefaultValue()
        val valuesCheck = mapValueMissingCheck() ?: return isDefault
        val mapGetter = message.field(field).getter<Map<*, *>>()
        return Expression("$isDefault || $mapGetter.values().stream().anyMatch($valuesCheck)")
    }

    /**
     * Returns the Java predicate (a `Predicate<V>` source fragment) that
     * detects a "missing" entry value for this map field, or `null` if the
     * field is not a supported map type for per-value checks.
     */
    private fun mapValueMissingCheck(): String? {
        val valueType = field.type.takeIf { it.isMap }?.map?.valueType ?: return null
        return when {
            valueType.isPrimitive && valueType.primitive == TYPE_STRING -> "String::isEmpty"
            valueType.isMessage -> "v -> v.equals(v.getDefaultInstanceForType())"
            else -> null
        }
    }

    private fun violation(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<io.spine.type.TypeName>
    ): Expression<ConstraintViolation> {
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(fieldPath, typeNameStr)
        val errorMessage = templateString(view.errorMessage, placeholders, IF_MISSING)
        return constraintViolation(errorMessage, typeNameStr, fieldPath, fieldValue = null)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<String>,
    ): Map<Placeholder, Expression<String>> = mapOf(
        FIELD_PATH.value to fieldPath.joinToString(),
        FIELD_TYPE.value to StringLiteral(field.type.name),
        PARENT_TYPE.value to typeName
    )
}
