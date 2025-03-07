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
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.java.render.findClass
import io.spine.protodata.java.render.findMessageTypes
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.tools.code.Java
import io.spine.validation.java.generate.MessageValidationCode
import io.spine.validation.java.generate.ValidationCodeInjector
import io.spine.validation.java.generate.option.DistinctGenerator
import io.spine.validation.java.generate.option.GoesGenerator
import io.spine.validation.java.generate.option.PatternGenerator
import io.spine.validation.java.generate.option.RequiredGenerator
import io.spine.validation.java.generate.option.ValidateGenerator
import io.spine.validation.java.rule.RuleGenerator

/**
 * The main Java renderer of the validation library.
 *
 * This renderer is applied to every compilation [Message],
 * even if the message does not have any declared constraints.
 */
public class JavaValidationRenderer : JavaRenderer() {

    private val valueConverter by lazy { JavaValueConverter(typeSystem) }
    private val codeInjector = ValidationCodeInjector()
    private val querying = this@JavaValidationRenderer
    private val generators by lazy {
        listOf(
            RuleGenerator(querying, typeSystem),
            RequiredGenerator(querying, valueConverter),
            PatternGenerator(querying),
            GoesGenerator(querying, valueConverter),
            DistinctGenerator(querying),
            ValidateGenerator(querying, valueConverter),
        )
    }

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        findMessageTypes()
            .forEach { message ->
                val code = generateCode(message.name)
                val file = sources.javaFileOf(message)
                file.render(code)
            }
    }

    private fun generateCode(message: TypeName): MessageValidationCode {
        val fieldOptions = generators.flatMap { it.codeFor(message) }
        val messageCode = with(fieldOptions) {
            MessageValidationCode(
                message = message.javaClassName(typeSystem),
                constraints = map { it.constraint },
                fields = flatMap { it.fields },
                methods = flatMap { it.methods }
            )
        }
        return messageCode
    }

    private fun SourceFile<Java>.render(code: MessageValidationCode) {
        val psiFile = psi() as PsiJavaFile
        val messageClass = psiFile.findClass(code.message)
        codeInjector.inject(code, messageClass)
        overwrite(psiFile.text)
    }
}
