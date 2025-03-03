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

package io.spine.validation.java.rule

import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.FieldDeclaration
import io.spine.protodata.java.MethodDeclaration
import io.spine.protodata.java.This
import io.spine.protodata.type.TypeSystem
import io.spine.server.query.Querying
import io.spine.server.query.select
import io.spine.validation.CompilationMessage
import io.spine.validation.Rule
import io.spine.validation.java.generate.FieldOptionCode
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.violations
import io.spine.validation.java.generate.OptionGenerator
import com.squareup.javapoet.CodeBlock as PoetCodeBlock
import com.squareup.javapoet.FieldSpec as PoetField
import com.squareup.javapoet.MethodSpec as PoetMethod

/**
 * Generates rule-based constraints.
 *
 * Despite implementing [OptionGenerator], this generator is not responsible
 * for a specific option. It generates code for constraints represented by `Rule`.
 * Having rule generation as [OptionGenerator] allows [JavaValidationRenderer] treat
 * all generators in a unified manner, significantly simplifying the class implementation.
 */
@Deprecated("Will be removed as we complete our migration to standalone generators.")
internal class RuleGenerator(
    private val querying: Querying,
    private val typeSystem: TypeSystem
) : OptionGenerator {

    /**
     * All compilation messages along with their rules.
     */
    private val allMessagesWithRules by lazy {
        querying.select<CompilationMessage>()
            .all()
    }

    override fun codeFor(type: TypeName): List<FieldOptionCode> {
        val message = allMessagesWithRules.first { it.type.name == type }
        val generatedRules = message.ruleList.map { generate(message.type, it) }
        return generatedRules
    }

    /**
     * Generates a Java constraint and supporting members for the passed [rule].
     *
     * A single rule always generates a single Java constraint represented by a [CodeBlock].
     * The number of supported members is not restricted. A single rule may generate zero,
     * one, or more supporting members.
     *
     * Note: [CodeGenerator] returns JavaPoet's elements, which we convert to counterparts
     * from ProtoData Expression API as required by [OptionGenerator] interface.
     */
    private fun generate(messageType: MessageType, rule: Rule): FieldOptionCode {
        val context = newContext(rule, messageType)
        val generator = generatorFor(context)
        val constraints = generator.code()
        val fields = generator.supportingFields()
        val methods = generator.supportingMethods()
        return FieldOptionCode(
            constraints.toCodeBlock(),
            fields.map(PoetField::toFieldDeclaration),
            methods.map(PoetMethod::toMethodDeclaration),
        )
    }

    private fun newContext(rule: Rule, messageType: MessageType) =
        GenerationContext(
            typeSystem = typeSystem,
            rule = rule,
            msg = This(),
            validatedType = messageType.name,
            protoFile = messageType.file,
            violationList = violations
        )
}

private fun PoetCodeBlock.toCodeBlock() = CodeBlock("$this".trim())

private fun PoetMethod.toMethodDeclaration() = MethodDeclaration("$this")

private fun PoetField.toFieldDeclaration() = FieldDeclaration<Any>(name, "$this")
