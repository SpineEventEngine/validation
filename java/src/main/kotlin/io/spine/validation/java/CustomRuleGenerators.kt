/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import io.spine.option.PatternOption
import io.spine.protobuf.AnyPacker.unpack
import io.spine.protodata.Field.CardinalityCase.LIST
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.MessageReference
import io.spine.protodata.codegen.java.MethodCall
import io.spine.protodata.isMap
import io.spine.protodata.qualifiedName
import io.spine.validate.ValidationError
import io.spine.validation.DistinctCollection
import io.spine.validation.RecursiveValidation
import io.spine.validation.Regex
import java.util.*
import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE
import java.util.regex.Pattern.DOTALL
import java.util.regex.Pattern.MULTILINE
import java.util.regex.Pattern.UNICODE_CASE
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.STATIC

/**
 * Generates code for the [DistinctCollection] operator.
 *
 * A list or the values of a map containing a duplicate is a constraint violation.
 */
private class DistinctGenerator(ctx: GenerationContext) : SimpleRuleGenerator(ctx) {

    override fun condition(): Expression {
        val map = ctx.fieldFromSimpleRule!!.isMap()
        val fieldValue = ctx.fieldOrElement!!
        val comparisonCollection = if (map) MethodCall(fieldValue, "values") else fieldValue
        return equalsOperator(
            MethodCall(fieldValue, "size"),
            ClassName(ImmutableSet::class)
                .call("copyOf", listOf(comparisonCollection))
                .chain("size")
        )
    }

    private fun equalsOperator(left: Expression, right: Expression): Expression {
        return Literal(left.toCode() + " == " + right.toCode())
    }
}
/**
 * Generates code for the [RecursiveValidation] operator.
 */
private class ValidateGenerator(ctx: GenerationContext) : SimpleRuleGenerator(ctx) {

    private val violationsVariable =
        Literal("generated_validationError_${ctx.fieldFromSimpleRule!!.name.value}")

    init {
        val field = ctx.fieldFromSimpleRule!!
        val fieldType = field.type
        check(fieldType.hasMessage()) {
            "(validate) only supports message types but field " +
                    "`${field.declaringType.qualifiedName()}.${field.name.value}` " +
                    "has type `$fieldType`."
        }
    }

    override fun prologue(): CodeBlock {
        val violations = MethodCall(ctx.fieldOrElement!!, "validate")
        return CodeBlock
            .builder()
            .addStatement(
                "\$T<\$T> \$L = \$L",
                Optional::class.java, ValidationError::class.java, violationsVariable, violations
            ).build()
    }

    override fun condition(): Expression =
        MethodCall(violationsVariable, "isPresent")


    override fun createViolation(): CodeBlock {
        val validationError = MethodCall(violationsVariable, "get")
        val violations = MessageReference(validationError.toCode())
            .field("constraint_violation", LIST)
            .getter
        return error().createParentViolation(ctx, violations)
    }
}

/**
 * Generates code for the [Regex] operator.
 */
private class PatternGenerator(
    private val feature: Regex,
    ctx: GenerationContext
) : SimpleRuleGenerator(ctx) {

    private val fieldName = ctx.fieldFromSimpleRule!!.name
    private val patternConstantName = "${fieldName.value}_PATTERN"

    override fun condition(): Expression {
        val matcher = MethodCall(Literal(patternConstantName), "matcher", listOf(ctx.fieldOrElement!!))
        val matchingMethod = if (feature.modifier.partialMatch) {
            "find"
        } else {
            "matches"
        }
        return matcher.chain(matchingMethod)
    }

    override fun supportingMembers(): CodeBlock {
        val compileModifiers = feature.hasModifier() && feature.modifier.containsFlags()
        val field = FieldSpec.builder(
            Pattern::class.java,
            patternConstantName,
            PRIVATE,
            STATIC,
            FINAL
        )
        if (compileModifiers) {
            field.initializer(
                "\$T.compile(\$S, \$L)",
                Pattern::class.java,
                feature.pattern,
                feature.modifier.flagsMask()
            )
        } else {
            field.initializer("\$T.compile(\$S)", Pattern::class.java, feature.pattern)
        }
        return super.supportingMembers()
            .toBuilder()
            .add(field.build().toString())
            .build()
    }
}

/**
 * Checks if this pattern modifier contains flags matching to those in `java.util.regex.Pattern`.
 */
private fun PatternOption.Modifier.containsFlags() =
    dotAll || caseInsensitive || multiline || unicode

/**
 * Converts this modifier into a bitwise mask built from `java.util.regex.Pattern` constants.
 */
private fun PatternOption.Modifier.flagsMask(): Expression {
    var mask = 0
    if (dotAll) mask = mask or DOTALL
    if (caseInsensitive) mask = mask or CASE_INSENSITIVE
    if (multiline) mask = mask or MULTILINE
    if (unicode) mask = mask or UNICODE_CASE
    return Literal(mask)
}

/**
 * A null-value generator which never produces code.
 *
 * Use this generator when an unknown custom validation operator is encountered.
 */
private class UnsupportedRuleGenerator(
    private val ruleName: String,
    ctx: GenerationContext
) : CodeGenerator(ctx) {

    override val canGenerate: Boolean = false

    override fun condition(): Nothing = unsupported()

    override fun error(): Nothing = unsupported()

    override fun createViolation(): Nothing = unsupported()

    private fun unsupported(): Nothing {
        throw UnsupportedOperationException("Rule `$ruleName` is not supported.")
    }
}

/**
 * Creates a [CodeGenerator] for a custom validation operator for the given context.
 */
internal fun generatorForCustom(ctx: GenerationContext): CodeGenerator {
    @Suppress("MoveVariableDeclarationIntoWhen") // For better readability.
    val feature = unpack(ctx.rule.simple.customOperator.feature)
    return when (feature) {
        is DistinctCollection -> DistinctGenerator(ctx)
        is RecursiveValidation -> ValidateGenerator(ctx)
        is Regex -> PatternGenerator(feature, ctx)
        else -> UnsupportedRuleGenerator(feature::class.simpleName!!, ctx)
    }
}
