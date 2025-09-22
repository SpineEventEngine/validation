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
import io.spine.option.PatternOption
import io.spine.protobuf.restoreProtobufEscapes
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.camelCase
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.jvm.CodeBlock
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.FieldDeclaration
import io.spine.tools.compiler.jvm.Literal
import io.spine.tools.compiler.jvm.MethodCall
import io.spine.tools.compiler.jvm.MethodDeclaration
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.jvm.call
import io.spine.tools.compiler.jvm.field
import io.spine.server.query.select
import io.spine.validate.ConstraintViolation
import io.spine.validation.PATTERN
import io.spine.validation.PatternField
import io.spine.validation.isRepeatedString
import io.spine.validation.isSingularString
import io.spine.validation.api.expression.ConstraintViolationClass
import io.spine.validation.api.expression.FieldPathClass
import io.spine.validation.api.expression.ImmutableListClass
import io.spine.validation.api.expression.PatternClass
import io.spine.validation.api.expression.StringClass
import io.spine.validation.api.expression.TypeNameClass
import io.spine.validation.api.expression.joinToString
import io.spine.validation.api.expression.orElse
import io.spine.validation.api.expression.resolve
import io.spine.validation.api.expression.stringify
import io.spine.validation.api.generate.SingleOptionCode
import io.spine.validation.api.generate.OptionGenerator
import io.spine.validation.api.generate.MessageScope.message
import io.spine.validation.api.generate.ValidateScope.parentName
import io.spine.validation.api.generate.ValidateScope.parentPath
import io.spine.validation.api.generate.ValidateScope.violations
import io.spine.validation.api.generate.mangled
import io.spine.validation.ErrorPlaceholder
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.ErrorPlaceholder.REGEX_MODIFIERS
import io.spine.validation.ErrorPlaceholder.REGEX_PATTERN
import io.spine.validation.api.expression.constraintViolation
import io.spine.validation.java.expression.templateString
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * The generator for `(pattern)` option.
 */
internal class PatternGenerator : OptionGenerator() {

    /**
     * All `(pattern)` fields in the current compilation process.
     */
    private val allPatternFields by lazy {
        querying.select<PatternField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allPatternFields
            .filter { it.id.type == type }
            .map { GeneratePattern(it).code() }
}

/**
 * Stores the generated Java field that contains the compiled [Pattern]
 * along with [partialMatch] modifier.
 */
private class CompiledPattern(val field: FieldDeclaration<Pattern>, val partialMatch: Boolean)

/**
 * Generates code for a single application of the `(pattern)` option
 * represented by the [view].
 */
private class GeneratePattern(private val view: PatternField) {

    private val field = view.subject
    private val fieldType = field.type
    private val fieldAccess = message.field(field)
    private val declaringType = field.declaringType
    private val camelFieldName = field.name.camelCase
    private val pattern = compilePattern()

    /**
     * Returns the generated code.
     */
    fun code(): SingleOptionCode = when {
        fieldType.isSingularString -> {
            val fieldValue = fieldAccess.getter<String>()
            val constraint = singularStringConstraint(fieldValue)
            SingleOptionCode(constraint, listOf(pattern.field))
        }

        fieldType.isRepeatedString -> {
            val fieldValues = fieldAccess.getter<List<String>>()
            val validateRepeatedField = mangled("validate$camelFieldName")
            val validateRepeatedFieldDecl = validateRepeated(fieldValues, validateRepeatedField)
            val constraint = repeatedStringConstraint(fieldValues, validateRepeatedField)
            SingleOptionCode(constraint, listOf(pattern.field), listOf(validateRepeatedFieldDecl))
        }

        else -> error(
            "The field type `${fieldType.name}` is not supported by `PatternFieldGenerator`." +
                    " Please ensure that the supported field types in this generator match those" +
                    " used by `PatternPolicy` when validating the `PatternFieldDiscovered` event."
        )
    }

    /**
     * Returns a [CodeBlock] that checks if the [fieldValue] matches the [pattern].
     */
    private fun singularStringConstraint(fieldValue: Expression<String>) = CodeBlock(
        """
        if (!$fieldValue.isEmpty() && !${pattern.matches(fieldValue)}) {
            var fieldPath = ${parentPath.resolve(field.name)};
            var typeName =  ${parentName.orElse(declaringType)};
            var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), fieldValue)};
            $violations.add(violation);
        }
        """.trimIndent()
    )

    /**
     * Returns a [CodeBlock] that invokes [validateRepeated] method to check
     * if each value from the [fieldValues] list matches the [pattern].
     */
    private fun repeatedStringConstraint(
        fieldValues: Expression<List<String>>,
        validateRepeated: String
    ) = CodeBlock(
        """
        if (!$fieldValues.isEmpty()) {
            var fieldViolations = $validateRepeated($parentPath, $parentName);
            $violations.addAll(fieldViolations);
        }
        """.trimIndent()
    )

    /**
     * Returns a [MethodDeclaration] of the method that goes through each element of
     * the [fieldValues] list making sure it matches the [pattern].
     *
     * The created method returns a list of [ConstraintViolation]s, containing one
     * violation per each invalid field value.
     */
    private fun validateRepeated(
        fieldValues: Expression<List<String>>,
        methodName: String
    ) = MethodDeclaration(
        """
        private $ImmutableListClass<$ConstraintViolationClass> $methodName($FieldPathClass $parentPath, $TypeNameClass $parentName) {
            var violations = $ImmutableListClass.<$ConstraintViolationClass>builder();
            for ($StringClass element : $fieldValues) {
                if (!element.isEmpty() && !${pattern.matches(ReadVar("element"))}) {
                    var fieldPath = ${parentPath.resolve(field.name)};
                    var typeName =  ${parentName.orElse(declaringType)};
                    var violation = ${violation(ReadVar("fieldPath"), ReadVar("typeName"), ReadVar("element"))};
                    violations.add(violation);
                }
            }
            return violations.build();
        }
        """.trimIndent()
    )

    /**
     * Creates a field containing a compiled Java [Pattern].
     *
     * The created field is wrapped in [CompiledPattern] instance along with
     * the [PatternOption.Modifier.getPartialMatch] value. This way, we have
     * everything we need to yield an expression for [Pattern.matcher] invocation
     * under a single object. Otherwise, the [matches] method would have to accept
     * `partialMatch` parameter along with the string value to check.
     *
     * Note that [PatternField.pattern_] string should not contain unprintable
     * characters to be used as a string literal. We have to [restore][restoreProtobufEscapes]
     * escape sequences for them, if any.
     */
    private fun compilePattern(): CompiledPattern {
        val modifiers = view.modifier
        val compilationArgs = listOf(
            StringLiteral(restoreProtobufEscapes(view.pattern)),
            Literal(modifiers.asFlagsMask())
        )
        val field = FieldDeclaration<Pattern>(
            modifiers = "private static final",
            type = PatternClass,
            name = mangled("${camelFieldName}Pattern"),
            value = PatternClass.call("compile", compilationArgs)
        )
        return CompiledPattern(field, modifiers.partialMatch)
    }

    /**
     * Yields a boolean expression that checks the given string [value]
     * matches this [CompiledPattern].
     */
    private fun CompiledPattern.matches(value: Expression<String>): Expression<Boolean> {
        val matcher = MethodCall<Matcher>(field.read(), "matcher", value)
        val operation = if (partialMatch) "find" else "matches"
        return matcher.chain(operation)
    }

    private fun violation(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<io.spine.type.TypeName>,
        fieldValue: Expression<String>,
    ): Expression<ConstraintViolation> {
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(fieldPath, typeNameStr, fieldValue)
        val errorMessage = templateString(view.errorMessage, placeholders, PATTERN)
        return constraintViolation(errorMessage, typeNameStr, fieldPath, fieldValue)
    }

    private fun supportedPlaceholders(
        fieldPath: Expression<FieldPath>,
        typeName: Expression<String>,
        fieldValue: Expression<String>,
    ): Map<ErrorPlaceholder, Expression<String>> = mapOf(
        FIELD_PATH to fieldPath.joinToString(),
        FIELD_VALUE to fieldValue,
        FIELD_TYPE to StringLiteral(fieldType.name),
        PARENT_TYPE to typeName,
        REGEX_PATTERN to StringLiteral(restoreProtobufEscapes(view.pattern)),
        REGEX_MODIFIERS to StringLiteral(restoreProtobufEscapes("${view.modifier}")),
    )
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
