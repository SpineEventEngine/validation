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

import com.google.protobuf.Message
import com.intellij.psi.PsiJavaFile
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.jvm.JavaValueConverter
import io.spine.tools.compiler.jvm.file.hasJavaRoot
import io.spine.tools.compiler.jvm.javaClassName
import io.spine.tools.compiler.jvm.render.JavaRenderer
import io.spine.tools.compiler.jvm.render.findClass
import io.spine.tools.compiler.jvm.render.findMessageTypes
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.compiler.render.SourceFileSet
import io.spine.string.ti
import io.spine.tools.code.Java
import io.spine.validation.java.generate.MessageValidationCode
import io.spine.validation.api.generate.OptionGenerator
import io.spine.validation.java.generate.ValidationCodeInjector
import io.spine.validation.java.generate.option.DistinctGenerator
import io.spine.validation.java.generate.option.GoesGenerator
import io.spine.validation.java.generate.option.bound.MaxGenerator
import io.spine.validation.java.generate.option.bound.MinGenerator
import io.spine.validation.java.generate.option.PatternGenerator
import io.spine.validation.java.generate.option.bound.RangeGenerator
import io.spine.validation.java.generate.option.ChoiceGenerator
import io.spine.validation.java.generate.option.RequireOptionGenerator
import io.spine.validation.java.generate.option.RequiredGenerator
import io.spine.validation.java.generate.option.ValidateGenerator
import io.spine.validation.java.generate.option.WhenGenerator
import io.spine.validation.java.generate.MessageClass
import io.spine.validation.java.generate.ValidatorClass
import io.spine.validation.java.generate.ValidatorGenerator

/**
 * The main Java renderer of the validation library.
 *
 * This renderer is applied to every compilation [Message],
 * even if the message does not have any declared constraints.
 */
internal class JavaValidationRenderer(
    customGenerators: List<OptionGenerator>,
    private val validators: Map<MessageClass, ValidatorClass>
) : JavaRenderer() {

    private val codeInjector = ValidationCodeInjector()
    private val querying = this@JavaValidationRenderer
    private val validatorGenerator by lazy {
        ValidatorGenerator(validators, typeSystem)
    }
    private val optionGenerators by lazy {
        (buildInGenerators() + customGenerators)
            .onEach { it.inject(querying) }
    }

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        findMessageTypes()
            .forEach { message ->
                checkDoesNotHaveValidator(message)
                val code = generateCode(message)
                val file = sources.javaFileOf(message)
                file.render(code)
            }
    }

    /**
     * Returns code generators for the built-in options.
     *
     * Note that some generators cannot be created outside of [JavaRenderer] because
     * they need [JavaValueConverter], which in turn needs [JavaRenderer.typeSystem].
     *
     * When [validation #199](https://github.com/SpineEventEngine/validation/issues/199)
     * is addressed, all generators must be created outside of [JavaValidationRenderer],
     * and just passed to the renderer.
     */
    private fun buildInGenerators(): List<OptionGenerator> {
        val valueConverter = JavaValueConverter(typeSystem)
        return listOf(
            RequiredGenerator(valueConverter),
            PatternGenerator(),
            GoesGenerator(valueConverter),
            DistinctGenerator(),
            ValidateGenerator(valueConverter),
            RangeGenerator(),
            MaxGenerator(),
            MinGenerator(),
            ChoiceGenerator(),
            WhenGenerator(valueConverter),
            RequireOptionGenerator(valueConverter),
        )
    }

    private fun generateCode(message: MessageType): MessageValidationCode {
        val fieldOptions = optionGenerators.flatMap { it.codeFor(message.name) }
        val validatorFields = validatorGenerator.codeFor(message)
        val messageCode = MessageValidationCode(
            message = message.javaClassName(typeSystem),
            constraints = fieldOptions.map { it.constraint } + validatorFields,
            fields = fieldOptions.flatMap { it.fields },
            methods = fieldOptions.flatMap { it.methods }
        )
        return messageCode
    }

    private fun SourceFile<Java>.render(code: MessageValidationCode) {
        val psiFile = psi() as PsiJavaFile
        val messageClass = psiFile.findClass(code.message)
        codeInjector.inject(code, messageClass)
        overwrite(psiFile.text)
    }

    /**
     * Ensures that the given compilation [message] does not have an assigned validator.
     *
     * Local messages are prohibited from having validators.
     */
    private fun checkDoesNotHaveValidator(message: MessageType) {
        val javaClass = message.javaClassName(typeSystem)
        val validator = validators[javaClass]
        check(validator == null) {
            """
            The validator `$validator` cannot be used to validate the `$javaClass` messages.
            Validators can be used only for external message types, which are not generated locally.
            Use built-in or custom validation options to declare constraints for the local messages.
            """.ti()
        }
    }
}
