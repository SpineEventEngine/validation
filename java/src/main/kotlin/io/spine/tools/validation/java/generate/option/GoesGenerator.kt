/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.validation.java.generate.option

import io.spine.base.FieldPath
import io.spine.server.query.select
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.jvm.CodeBlock
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.JavaValueConverter
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.jvm.field
import io.spine.tools.validation.GoesField
import io.spine.tools.validation.java.expression.EmptyFieldCheck
import io.spine.tools.validation.java.expression.constraintViolation
import io.spine.tools.validation.java.expression.joinToString
import io.spine.tools.validation.java.expression.orElse
import io.spine.tools.validation.java.expression.resolve
import io.spine.tools.validation.java.expression.stringValueOf
import io.spine.tools.validation.java.expression.stringify
import io.spine.tools.validation.java.expression.templateString
import io.spine.tools.validation.java.generate.MessageScope.message
import io.spine.tools.validation.java.generate.OptionGeneratorWithConverter
import io.spine.tools.validation.java.generate.SingleOptionCode
import io.spine.tools.validation.java.generate.ValidateScope.parentName
import io.spine.tools.validation.java.generate.ValidateScope.parentPath
import io.spine.tools.validation.java.generate.ValidateScope.violations
import io.spine.tools.validation.option.GOES
import io.spine.tools.validation.sortedByDeclaration
import io.spine.validation.ConstraintViolation
import io.spine.string.Placeholder
import io.spine.validation.StandardPlaceholder.FIELD_PATH
import io.spine.validation.StandardPlaceholder.FIELD_TYPE
import io.spine.validation.StandardPlaceholder.FIELD_VALUE
import io.spine.validation.StandardPlaceholder.GOES_COMPANION
import io.spine.validation.StandardPlaceholder.PARENT_TYPE

/**
 * The generator for the `(goes)` option.
 */
internal class GoesGenerator : OptionGeneratorWithConverter() {

    /**
     * All `(goes)` fields in the current compilation process.
     *
     * The fields of each message go in the order of declaration.
     */
    private val allGoesFields by lazy {
        querying.select<GoesField>()
            .all()
            .sortedByDeclaration()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allGoesFields
            .filter { it.id.type == type }
            .map { GenerateGoes(it, converter).code() }
}

/**
 * Generates code for a single application of the `(goes)` option
 * represented by the [view].
 */
private class GenerateGoes(
    private val view: GoesField,
    override val converter: JavaValueConverter
) : EmptyFieldCheck {

    private val field = view.subject
    private val fieldType = field.type
    private val declaringType = field.declaringType

    /**
     * Returns the generated code.
     */
    fun code(): SingleOptionCode {
        val companion = view.companion
        val fieldGetter = message.field(field).getter<Any>()
        val constraint = CodeBlock(
            """
            if (!${field.hasDefaultValue()} && ${companion.hasDefaultValue()}) {
                var fieldPath = ${parentPath.resolve(field.name)};
                var typeName =  ${parentName.orElse(declaringType)};
                var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), fieldGetter)};
                $violations.add(violation);
            }
            """.trimIndent()
        )
        return SingleOptionCode(constraint)
    }

    private fun violation(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<io.spine.type.TypeName>,
        fieldValue: Expression<*>,
    ): Expression<ConstraintViolation> {
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(fieldPath, typeNameStr, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, GOES)
        return constraintViolation(errorMessage, typeNameStr, fieldPath, fieldValue)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<String>,
        fieldValue: Expression<*>,
    ): Map<Placeholder, Expression<String>> = mapOf(
        FIELD_PATH.value to fieldPath.joinToString(),
        FIELD_VALUE.value to fieldType.stringValueOf(fieldValue),
        FIELD_TYPE.value to StringLiteral(fieldType.name),
        PARENT_TYPE.value to typeName,
        GOES_COMPANION.value to StringLiteral(view.companion.name.value)
    )
}
