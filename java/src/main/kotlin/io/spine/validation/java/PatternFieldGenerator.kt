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

import io.spine.base.FieldPath
import io.spine.option.PatternOption
import io.spine.protodata.ast.camelCase
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.FieldDeclaration
import io.spine.protodata.java.InitField
import io.spine.protodata.java.Literal
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.MethodDeclaration
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.call
import io.spine.protodata.java.field
import io.spine.protodata.java.toBuilder
import io.spine.validate.ConstraintViolation
import io.spine.validation.PATTERN
import io.spine.validation.PatternField
import io.spine.validation.isRepeatedString
import io.spine.validation.isSingularString
import io.spine.validation.java.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.java.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.java.ErrorPlaceholder.REGEX_MODIFIERS
import io.spine.validation.java.ErrorPlaceholder.REGEX_PATTERN
import io.spine.validation.java.ValidationCodeInjector.MessageScope.message
import io.spine.validation.java.ValidationCodeInjector.ValidateScope.parentPath
import io.spine.validation.java.ValidationCodeInjector.ValidateScope.violations
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.lang.StringEscapeUtils.escapeJava

/**
 * The generator for `(pattern)` option.
 *
 * Generates code for a single field represented by the provided [view].
 */
internal class PatternFieldGenerator(private val view: PatternField) {

    private val field = view.subject
    private val fieldType = field.type
    private val fieldAccess = message.field(field)
    private val declaringType = field.declaringType
    private val camelFieldName = field.name.camelCase
    private val pattern = compilePattern()

    /**
     * Generates code for a field represented by the [view].
     */
    fun generate(): FieldOptionCode = when {
        fieldType.isSingularString -> {
            val fieldValue = fieldAccess.getter<String>()
            val constraint = singularStringConstraint(fieldValue)
            FieldOptionCode(constraint, listOf(pattern.field))
        }

        fieldType.isRepeatedString -> {
            val fieldValue = fieldAccess.getter<List<String>>()
            val validateRepeatedField = mangled("validate$camelFieldName")
            val validateRepeatedFieldDecl = validateRepeatedField(fieldValue, validateRepeatedField)
            val constraint = repeatedStringConstraint(fieldValue, validateRepeatedField)
            FieldOptionCode(constraint, listOf(pattern.field), listOf(validateRepeatedFieldDecl))
        }

        else -> error {
            "Unsupported field type: `${fieldType.name}`. The `(${PATTERN})` option can be" +
                    "applied only to singular or repeated string fields."
        }
    }

    private fun singularStringConstraint(fieldValue: Expression<String>) = CodeBlock(
        """
        if (!$fieldValue.isEmpty() && !${pattern.matches(fieldValue)}) {
            var fieldPath = ${fieldPath(parentPath)};
            var violation = ${violation(ReadVar("fieldPath"), fieldValue)};
            $violations.add(violation);
        }
        """.trimIndent()
    )

    private fun repeatedStringConstraint(
        fieldValue: Expression<List<String>>,
        validateRepeatedField: String
    ) = CodeBlock(
        """
        if (!$fieldValue.isEmpty()) {
            var fieldViolations = $validateRepeatedField($parentPath);
            $violations.addAll(fieldViolations);
        }
        """.trimIndent()
    )

    private fun validateRepeatedField(
        fieldValue: Expression<List<String>>,
        methodName: String
    ) = MethodDeclaration(
        """
        private $ImmutableListClass<$ConstraintViolationClass> $methodName($FieldPathClass parent) {
            var violations = $ImmutableListClass.<$ConstraintViolationClass>builder();
            for ($StringClass element : $fieldValue) {
                if (!element.isEmpty() && !${pattern.matches(ReadVar("element"))}) {
                    var fieldPath = ${fieldPath(ReadVar("parent"))};
                    var violation = ${violation(ReadVar("fieldPath"), ReadVar("element"))};
                    violations.add(violation);
                }
            }
            return violations.build();
        }
        """.trimIndent()
    )

    private fun compilePattern(): CompiledPattern {
        val modifiers = view.modifier
        val compilationArgs = listOf(
            StringLiteral(escapeJava(view.pattern)),
            Literal(modifiers.asFlagsMask())
        )
        val field = InitField<Pattern>(
            modifiers = "private static final",
            type = PatternClass,
            name = mangled("${camelFieldName}Pattern"),
            value = PatternClass.call("compile", compilationArgs)
        )
        return CompiledPattern(field, modifiers.partialMatch)
    }

    private fun CompiledPattern.matches(value: Expression<String>): Expression<Boolean> {
        val matcher = MethodCall<Matcher>(field.read(), "matcher", value)
        val operation = if (partialMatch) "find" else "matches"
        return matcher.chain(operation)
    }

    private fun fieldPath(parent: Expression<FieldPath>): Expression<FieldPath> =
        parent.toBuilder()
            .chainAdd("field_name", StringLiteral(field.name.value))
            .chainBuild()

    private fun violation(
        fieldPath: Expression<FieldPath>,
        fieldValue: Expression<String>,
    ): Expression<ConstraintViolation> {
        val qualifiedName = field.qualifiedName
        val placeholders = supportedPlaceholders(fieldPath, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, PATTERN, qualifiedName)
        return constraintViolation(errorMessage, declaringType, fieldPath, fieldValue)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        fieldValue: Expression<String>,
    ): Map<ErrorPlaceholder, Expression<String>> {
        val pathAsString = FieldPathsClass.call<String>("getJoined", fieldPath)
        return mapOf(
            FIELD_PATH to pathAsString,
            FIELD_VALUE to fieldValue,
            FIELD_TYPE to StringLiteral(fieldType.name),
            PARENT_TYPE to StringLiteral(declaringType.qualifiedName),
            REGEX_PATTERN to StringLiteral(escapeJava(view.pattern)),
            REGEX_MODIFIERS to StringLiteral(escapeJava("${view.modifier}")),
        )
    }
}

/**
 * Stores the compiled pattern field along with [partialMatch] modifier.
 *
 * This way, we have everything we need to yield an expression for [Pattern.matcher]
 * invocation under a single object. Otherwise, [PatternFieldGenerator.matches] would
 * have to accept [partialMatch] along the string value to check.
 */
private class CompiledPattern(val field: FieldDeclaration<Pattern>, val partialMatch: Boolean)

/**
 * Converts this [PatternOption.Modifier] to a Java [Pattern] bitwise mask.
 *
 * Note that [PatternOption.Modifier.getPartialMatch] is not handled by this method.
 * It is not a flag in Java. We take it into account when choosing which method to invoke
 * upon the [Matcher]: [Matcher.find] or [Matcher.matches].
 */
private fun PatternOption.Modifier.asFlagsMask(): Int {
    var mask = 0
    if (dotAll) {
        mask = mask or Pattern.DOTALL
    }
    if (caseInsensitive) {
        mask = mask or Pattern.CASE_INSENSITIVE
    }
    if (multiline) {
        mask = mask or Pattern.MULTILINE
    }
    if (unicode) {
        mask = mask or Pattern.UNICODE_CASE
    }
    return mask
}
