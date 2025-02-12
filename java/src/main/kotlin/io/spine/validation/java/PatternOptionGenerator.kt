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

import com.google.common.collect.ImmutableList
import com.google.protobuf.Message
import io.spine.base.FieldPath
import io.spine.option.PatternOption
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.camelCase
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.backend.SecureRandomString
import io.spine.protodata.java.ClassName
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
import io.spine.validation.IF_MISSING
import io.spine.validation.PATTERN
import io.spine.validation.PatternField
import io.spine.validation.isRepeatedString
import io.spine.validation.isSingularString
import io.spine.validation.java.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.java.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.java.ErrorPlaceholder.PARENT_TYPE
import java.util.regex.Matcher
import java.util.regex.Pattern

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
        val errorMessage = view.errorMessage

        return when {
            fieldType.isSingularString -> {
                val fieldValue = fieldAccess.getter<String>()
                val constraint = CodeBlock(
                    """
                    if (!$fieldValue.isEmpty() && !${pattern.matches(fieldValue, partialMatch)}) {
                        var fieldPath = ${fieldPath(field, parent)};
                        var violation = ${violation(field, ReadVar("fieldPath"), errorMessage)};
                        $violations.add(violation);
                    }
                    """.trimIndent()
                )
                FieldOptionCode(constraint, pattern)
            }

            fieldType.isRepeatedString -> {
                val fieldValue = fieldAccess.getter<List<String>>()
                val validateRepeatedField = "validate${field.name.camelCase}_${hash()}"
                val validateRepeatedFieldDecl = MethodDeclaration(
                    """
                    private $ImmutableListClass $validateRepeatedField($FieldPathClass parent) {
                        var violations = ${ImmutableListClass.call<ImmutableList.Builder<*>>("builder")}
                        for ($StringClass element : $fieldValue) {
                            if (!element.isEmpty() && !${pattern.matches(ReadVar("element"), view.modifier.partialMatch)}) {
                                var fieldPath = ${fieldPath(field, ReadVar("parent"))};
                                var violation = ${violation(field, ReadVar("fieldPath"), view.errorMessage)};
                                violations.add(violation);
                            }
                        }
                        return violations;
                    }
                    """.trimIndent()
                )
                val constraint = CodeBlock(
                    """
                    if (!$fieldValue.isEmpty()) {
                        var fieldViolations = $validateRepeatedField($parent);
                        $violations.add(fieldViolations);
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
            StringLiteral(view.pattern),
            Literal(view.modifier.asFlagsMask())
        )
        return InitField(
            modifiers = "private static final",
            type = PatternClass,
            name = "${view.subject.name}_PATTERN",
            value = PatternClass.call("compile", compilationArgs)
        )
    }

    private fun FieldDeclaration<Pattern>.matches(
        value: Expression<String>,
        partialMatch: Boolean
    ): Expression<Boolean> {
        val matcher = MethodCall<Matcher>(this, "matcher", value)
        val operation = if (partialMatch) "find" else "matches"
        return matcher.chain(operation)
    }

    private fun fieldPath(field: Field, parent: Expression<FieldPath>): Expression<FieldPath> =
        parent.toBuilder()
            .chainAdd("field_name", StringLiteral(field.name.value))
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

/**
 * Converts this [PatternOption.Modifier] into a bitwise mask built
 * from Java [Pattern] flags constants.
 *
 * Note that [PatternOption.Modifier.getPartialMatch] is not handled by this method.
 * For Java patterns, it is not a flag. We handle it when choosing which method
 * to invoke upon the resulting matcher: `matcher.find()` or `matcher.matches()`.
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

private const val RANDOM_STR_LENGTH = 10

private fun hash(): String =
    SecureRandomString.generate(RANDOM_STR_LENGTH)
        .filter(Char::isJavaIdentifierPart)
