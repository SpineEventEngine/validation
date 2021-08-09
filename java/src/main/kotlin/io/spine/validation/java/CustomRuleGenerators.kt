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
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.MethodCall
import io.spine.protodata.isMap
import io.spine.validate.ValidationError
import io.spine.validation.DistinctCollection
import io.spine.validation.RecursiveValidation
import io.spine.validation.Regex
import java.util.regex.Pattern
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
        val comparisonCollection = if (map) fieldValue.chain("values") else fieldValue
        return equalsOperator(
            fieldValue.chain("size"),
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
private class ValidateGenerator(
    ctx: GenerationContext
) : SimpleRuleGenerator(ctx) {

    private val violationsVariable: Literal
        get() = Literal("generated_validationError_${ctx.fieldFromSimpleRule!!.name.value}")

    private val childViolations: MethodCall
        get() = MethodCall(violationsVariable, "getAllConstraintViolation")

    override fun condition(): Expression =
        childViolations.chain("isEmpty")

    override fun prologue(): CodeBlock =
        CodeBlock.builder()
            .addStatement(
                "\$T \$L = ${fieldValue.chain("validate")}",
                ValidationError::class.java, violationsVariable
            ).build()

    override fun createViolation(): CodeBlock =
        error().createParentViolation(
            field,
            fieldValue,
            ctx.violationsList,
            childViolations
        )
}

private class PatternGenerator(
    ctx: GenerationContext,
    private val feature: Regex
) : SimpleRuleGenerator(ctx) {

    private val patternConstantName = "${ctx.fieldFromSimpleRule!!.name.value}_PATTERN"

    override fun condition(): Expression {
        val matcher = MethodCall(Literal(patternConstantName), "matcher", listOf(fieldValue))
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
                feature.modifier.flagsString()
            )
        } else {
            field.initializer("\$T.compile(\$S)", Pattern::class.java, feature.pattern)
        }
        return CodeBlock.of(field.build().toString())
    }
}

private fun PatternOption.Modifier.containsFlags() =
    dotAll || caseInsensitive || multiline || unicode

private fun PatternOption.Modifier.flagsString(): String {
    val flags = mutableListOf<String>()
    if (dotAll) flags.add("DOTALL")
    if (caseInsensitive) flags.add("CASE_INSENSITIVE")
    if (multiline) flags.add("MULTILINE")
    if (unicode) flags.add("UNICODE_CASE")
    return flags.joinToString { "${Pattern::class.qualifiedName}.$it" }
}

/**
 * A null-value generator which never produces code.
 *
 * Use this generator when an unknown custom validation operator is encountered.
 */
private class UnsupportedRuleGenerator(
    ctx: GenerationContext,
    private val ruleName: String
) : CodeGenerator(ctx) {

    override val canGenerate: Boolean
        get() = false

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
        is Regex -> PatternGenerator(ctx, feature)
        else -> UnsupportedRuleGenerator(ctx, feature::class.simpleName!!)
    }
}
