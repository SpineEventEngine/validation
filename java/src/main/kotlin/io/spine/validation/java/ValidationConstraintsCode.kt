/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import com.google.common.base.Splitter
import com.google.common.collect.ImmutableList
import com.google.protobuf.Message
import com.squareup.javapoet.CodeBlock
import io.spine.protodata.ast.File
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.This
import io.spine.validation.CompiledMessage
import io.spine.validation.Rule
import io.spine.validation.java.ValidationCode.Companion.VIOLATIONS
import java.lang.System.lineSeparator

/**
 * Code generated for validation constraints specified in a message type.
 */
internal class ValidationConstraintsCode private constructor(

    /**
     * The parent renderer which this method object serves.
     */
    private val renderer: JavaValidationRenderer,

    /**
     * The compiled message to generate the code for.
     */
    private val message: CompiledMessage
) {

    /**
     * The name of the message type for which to generate the code.
     */
    private val messageType: TypeName = message.name

    /**
     * The file which declares the message type.
     */
    private val declaringFile: File = message.type.file

    /**
     * The expression for referencing the message in the code.
     */
    private val messageReference = This<Message>()

    /**
     * The builder of the code which performs validation.
     */
    private val code: CodeBlock.Builder = CodeBlock.builder()

    /**
     * Class-level declarations used in the validation code.
     */
    private val supportingMembers: ImmutableList.Builder<CodeBlock> =
        ImmutableList.builder()

    /**
     * Obtains the generated code block.
     */
    fun codeBlock(): CodeBlock = code.build()

    /**
     * Obtains class-level declarations used in the validation code as code lines.
     */
    fun supportingMembersCode(): Iterable<String> {
        return supportingMembers.build()
            .stream()
            .flatMap { code: CodeBlock -> onNewLine.splitToStream(code.toString()) }
            .collect(ImmutableList.toImmutableList())
    }

    private fun generate() {
        for (rule in message.ruleList) {
            addRule(rule)
        }
    }

    private fun addRule(rule: Rule) {
        val context = newContext(rule)
        val generator = generatorFor(context)
        val block = generator.code()
        code.add(block)
        supportingMembers.add(generator.supportingMembers())
    }

    private fun newContext(rule: Rule): GenerationContext = GenerationContext(
        client = renderer,
        typeSystem = renderer.typeSystem,
        rule,
        msg = messageReference,
        validatedType = messageType,
        protoFile = declaringFile,
        violationList = VIOLATIONS
    )

    companion object {

        private val onNewLine: Splitter = Splitter.on(lineSeparator())

        /**
         * Creates a new instance with the generated validation constraints code.
         */
        fun generate(r: JavaValidationRenderer, v: CompiledMessage): ValidationConstraintsCode {
            val result = ValidationConstraintsCode(r, v)
            result.generate()
            return result
        }
    }
}
