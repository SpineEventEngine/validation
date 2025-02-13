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
import io.spine.option.PatternOption
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.TypeName
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
import io.spine.protodata.java.This
import io.spine.protodata.java.call
import io.spine.protodata.java.field
import io.spine.protodata.java.toBuilder
import io.spine.server.query.Querying
import io.spine.server.query.select
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
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.lang.StringEscapeUtils.escapeJava

/**
 * The generator for `(pattern)` option.
 */
internal class PatternOptionGenerator(private val querying: Querying) : OptionGenerator {

    /**
     * All pattern fields in the current compilation process.
     */
    private val allPatternFields by lazy {
        querying.select<PatternField>()
            .all()
    }

    override fun codeFor(
        type: TypeName,
        parent: Expression<FieldPath>,
        violations: Expression<MutableList<ConstraintViolation>>
    ): MessageOptionCode {
        val patternFields = allPatternFields.filter { it.id.type == type }
        val fieldsCode = patternFields.map { codeFor(it, parent, violations) }
        return MessageOptionCode(fieldsCode)
    }

    private fun codeFor(
        view: PatternField,
        parent: Expression<FieldPath>,
        violations: Expression<MutableList<ConstraintViolation>>
    ): FieldOptionCode {
        val field = view.subject
        val fieldType = field.type
        val fieldAccess = This<Message>(explicit = false).field(field)

        val pattern = compilePattern(view)
        val partialMatch = view.modifier.partialMatch

        return when {
            fieldType.isSingularString -> {
                val fieldValue = fieldAccess.getter<String>()
                val constraint = CodeBlock(
                    """
                    if (!$fieldValue.isEmpty() && !${pattern.matches(fieldValue, partialMatch)}) {
                        var fieldPath = ${fieldPath(field, parent)};
                        var violation = ${violation(view, ReadVar("fieldPath"), fieldValue)};
                        $violations.add(violation);
                    }
                    """.trimIndent()
                )
                FieldOptionCode(constraint, pattern)
            }

            fieldType.isRepeatedString -> {
                val fieldValue = fieldAccess.getter<List<String>>()
                val validateRepeatedField = mangled("validate${field.name.camelCase}")
                val validateRepeatedFieldDecl = MethodDeclaration(
                    """
                    private $ImmutableListClass<$ConstraintViolationClass> $validateRepeatedField($FieldPathClass parent) {
                        var violations = $ImmutableListClass.<$ConstraintViolationClass>builder();
                        for ($StringClass element : $fieldValue) {
                            if (!element.isEmpty() && !${pattern.matches(ReadVar("element"), partialMatch)}) {
                                var fieldPath = ${fieldPath(field, ReadVar("parent"))};
                                var violation = ${violation(view, ReadVar("fieldPath"), ReadVar("element"))};
                                violations.add(violation);
                            }
                        }
                        return violations.build();
                    }
                    """.trimIndent()
                )
                val constraint = CodeBlock(
                    """
                    if (!$fieldValue.isEmpty()) {
                        var fieldViolations = $validateRepeatedField($parent);
                        $violations.addAll(fieldViolations);
                    }
                    """.trimIndent()
                )
                FieldOptionCode(constraint, pattern, validateRepeatedFieldDecl)
            }

            else -> error {
                "Unsupported field type: `${fieldType.name}`. The `(${PATTERN})` option can be" +
                        "applied only to singular or repeated string fields."
            }
        }
    }

    private fun compilePattern(view: PatternField): FieldDeclaration<Pattern> {
        val compilationArgs = listOf(
            StringLiteral(escapeJava(view.pattern)),
            Literal(view.modifier.asFlagsMask())
        )
        return InitField(
            modifiers = "private static final",
            type = PatternClass,
            name = "${view.subject.name.value}_PATTERN",
            value = PatternClass.call("compile", compilationArgs)
        )
    }

    private fun FieldDeclaration<Pattern>.matches(
        value: Expression<String>,
        partialMatch: Boolean
    ): Expression<Boolean> {
        val matcher = MethodCall<Matcher>(this.read(), "matcher", value)
        val operation = if (partialMatch) "find" else "matches"
        return matcher.chain(operation)
    }

    private fun fieldPath(field: Field, parent: Expression<FieldPath>): Expression<FieldPath> =
        parent.toBuilder()
            .chainAdd("field_name", StringLiteral(field.name.value))
            .chainBuild()

    private fun violation(
        view: PatternField,
        fieldPath: Expression<FieldPath>,
        fieldValue: Expression<String>,
    ): Expression<ConstraintViolation> {
        val field = view.subject
        val placeholders = supportedPlaceholders(view, fieldPath, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, PATTERN,
            field.qualifiedName)
        return constraintViolation(errorMessage, field.declaringType, fieldPath)
    }

    private fun supportedPlaceholders(
        view: PatternField,
        fieldPath: Expression<FieldPath>,
        fieldValue: Expression<String>,
    ): Map<ErrorPlaceholder, Expression<String>> {
        val pathAsString = FieldPathsClass.call<String>("getJoined", fieldPath)
        return mapOf(
            FIELD_PATH to pathAsString,
            FIELD_VALUE to fieldValue,
            FIELD_TYPE to StringLiteral(view.subject.type.name),
            PARENT_TYPE to StringLiteral(view.subject.declaringType.qualifiedName),
            REGEX_PATTERN to StringLiteral(escapeJava(view.pattern)),
            REGEX_MODIFIERS to StringLiteral(escapeJava("${view.modifier}")),
        )
    }
}

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
