/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.codegen.java.MessageReference
import io.spine.protodata.codegen.java.MethodCall
import io.spine.protodata.codegen.java.javaFile
import io.spine.protodata.codegen.java.lines
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.type.TypeSystem
import io.spine.tools.java.codeBlock
import io.spine.validate.NonValidated
import io.spine.validate.Validated
import io.spine.validate.ValidationException
import io.spine.validation.MessageValidation
import io.spine.validation.java.ValidationCode.Companion.OPTIONAL_ERROR
import io.spine.validation.java.ValidationCode.Companion.VALIDATE
import java.nio.file.Path
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * A [Renderer][io.spine.protodata.renderer.Renderer] for the validation code in Java.
 *
 * Inserts code into the [ValidateBeforeReturn] insertion point.
 *
 * The generated code assumes there is a variable called `result`.
 * Its type is the type of the validated message.
 * The variable holds the value of the message to validate.
 *
 * The generated code is a number of code lines.
 * It does not contain declarations (classes, methods, etc.).
 *
 * If the validation rules are broken,
 * throws a [ValidationException][io.spine.validate.ValidationException].
 */
@Suppress("unused")
public class JavaValidationRenderer : JavaRenderer() {

    private lateinit var sources: SourceFileSet
    private lateinit var validations: Validations

    public fun typeSystem(): TypeSystem {
        return typeSystem!!
    }

    protected override fun render(sources: SourceFileSet) {
        this.sources = sources
        this.validations = findValidations()
        val messageTypes = queryMessageTypes()
        messageTypes.forEach(Consumer { type -> this.generateCode(type) })
        annotateGeneratedMessages(sources, messageTypes)
        plugValidationIntoBuild(sources, messageTypes)
    }

    private fun findValidations(): Validations {
        val client = select(MessageValidation::class.java)
        return Validations(client)
    }

    private fun queryMessageTypes(): Set<MessageWithFile> {
        return select(ProtobufSourceFile::class.java)
            .all()
            .stream()
            .flatMap { file -> messages(file) }
            .collect(Collectors.toSet())
    }

    private fun generateCode(type: MessageWithFile) {
        val message = type.message
        val javaFile = message.javaFile(type.declaredIn)
        sources.findFile(javaFile)
            .ifPresent { sourceFile -> addValidationCode(sourceFile, message) }
    }

    private fun addValidationCode(sourceFile: SourceFile, type: MessageType) {
        val validation = validations[type]
        val validationCode = ValidationCode(this, validation, sourceFile)
        validationCode.generate()
    }

    public companion object {

        private fun messages(file: ProtobufSourceFile): Stream<MessageWithFile> {
            return file
                .typeMap
                .values
                .stream()
                .map { m ->
                    MessageWithFile.newBuilder()
                        .setMessage(m)
                        .setDeclaredIn(file.file)
                        .build()
                }
        }

        private fun annotateGeneratedMessages(
            sources: SourceFileSet, messageTypes: Set<MessageWithFile>
        ) {
            messageTypes.stream()
                .map { m: MessageWithFile -> m.message.javaFile(m.declaredIn) }
                .flatMap { path -> sources.findFile(path).stream() }
                .distinct()
                .forEach { file -> addAnnotations(file) }
        }

        private fun addAnnotations(file: SourceFile) {
            annotateBuildMethod(file)
            annotateBuildPartialMethod(file)
        }

        private fun annotateBuildMethod(sourceFile: SourceFile) {
            val buildMethod = BuildMethodReturnTypeAnnotation()
            sourceFile.atInline(buildMethod)
                .add(annotation(Validated::class.java))
        }

        private fun annotateBuildPartialMethod(sourceFile: SourceFile) {
            val buildPartialMethod = BuildPartialReturnTypeAnnotation()
            sourceFile.atInline(buildPartialMethod)
                .add(annotation(NonValidated::class.java))
        }

        /**
         * Creates a string to be used in the code when using the given annotation class.
         *
         * @implNote Adds space before `@` so that when the type is fully qualified, the
         * annotation is: 1) visible better 2) two or more annotations are separated.
         */
        private fun annotation(annotationClass: Class<out Annotation?>): String {
            return " @" + annotationClass.name
        }

        private fun plugValidationIntoBuild(
            sources: SourceFileSet, messageTypes: Set<MessageWithFile>
        ) {
            messageTypes.stream()
                .map { m -> m.message.javaFile(m.declaredIn) }
                .flatMap { path ->
                    sources.findFile(path).stream()
                }
                .distinct()
                .forEach { sourceFile -> insertBeforeBuild(sourceFile) }
        }

        private fun insertBeforeBuild(sourceFile: SourceFile) {
            sourceFile.at(ValidateBeforeReturn())
                .withExtraIndentation(2)
                .add(validateBeforeBuild())
        }

        private fun validateBeforeBuild(): ImmutableList<String> = codeBlock {
            val result = MessageReference("result")
            addStatement(
                "\$T error = \$L",
                OPTIONAL_ERROR,
                MethodCall(result, VALIDATE)
            )
            beginControlFlow("if (error.isPresent())")
            addStatement(
                "throw new \$T(error.get().getConstraintViolationList())",
                ValidationException::class.java
            )
            endControlFlow()
        }.lines()
    }
}
