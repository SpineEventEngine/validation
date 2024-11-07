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

import com.google.common.collect.ImmutableList
import io.spine.protodata.ast.MessageType
import io.spine.protodata.java.MessageReference
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.javaFile
import io.spine.protodata.java.lines
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.Java
import io.spine.tools.java.codeBlock
import io.spine.validate.NonValidated
import io.spine.validate.Validated
import io.spine.validate.ValidationException
import io.spine.validation.MessageValidation
import io.spine.validation.java.ValidationCode.Companion.OPTIONAL_ERROR
import io.spine.validation.java.ValidationCode.Companion.VALIDATE
import io.spine.validation.java.point.BuildMethodReturnTypeAnnotation
import io.spine.validation.java.point.BuildPartialReturnTypeAnnotation
import io.spine.validation.java.point.ValidateBeforeReturn

/**
 * A [Renderer][io.spine.protodata.render.Renderer] for the validation code in Java.
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
public class JavaValidationRenderer : JavaRenderer() {

    private lateinit var sources: SourceFileSet
    private lateinit var validations: Validations

    public fun typeSystem(): TypeSystem = typeSystem!!

    protected override fun render(sources: SourceFileSet) {
        this.sources = sources
        this.validations = findValidations()
        val messageTypes = findMessageTypes()
        messageTypes.forEach {
            generateCode(it)
        }
        sources.annotateGeneratedMessages(messageTypes)
        sources.plugValidationIntoBuild(messageTypes)
    }

    private fun findValidations(): Validations {
        val client = select(MessageValidation::class.java)
        return Validations(client)
    }

    private fun generateCode(type: MessageWithFile) {
        val message = type.message
        val javaFile = message.javaFile(type.fileHeader)
        sources.find(javaFile)?.let {
            @Suppress("UNCHECKED_CAST")
            (it as SourceFile<Java>).addValidationCode(message)
        }
    }

    private fun SourceFile<Java>.addValidationCode(type: MessageType) {
        val validation = validations[type]
        val validationCode = ValidationCode(this@JavaValidationRenderer, validation, this)
        validationCode.generate()
    }
}

/**
 * Locates source files for the given message types and
 * performs the given action for each file.
 */
private fun SourceFileSet.forEachSourceFile(
    messageTypes: Set<MessageWithFile>,
    action: SourceFile<Java>.() -> Unit
) {
    messageTypes
        .map { m -> m.message.javaFile(m.fileHeader) }
        .mapNotNull { path -> find(path) }
        .distinct()
        .map {
            @Suppress("UNCHECKED_CAST") // Safe as we look for Java files.
            it as SourceFile<Java>
        }
        .forEach(action)
}

private fun SourceFileSet.annotateGeneratedMessages(messageTypes: Set<MessageWithFile>) =
    forEachSourceFile(messageTypes, SourceFile<Java>::addAnnotations)

private fun SourceFile<Java>.addAnnotations() {
    annotateBuildMethod()
    annotateBuildPartialMethod()
}

private fun SourceFile<Java>.annotateBuildMethod() {
    val buildMethod = BuildMethodReturnTypeAnnotation()
    atInline(buildMethod)
        .add(annotation(Validated::class.java))
}

private fun SourceFile<Java>.annotateBuildPartialMethod() {
    val buildPartialMethod = BuildPartialReturnTypeAnnotation()
    atInline(buildPartialMethod)
        .add(annotation(NonValidated::class.java))
}

/**
 * Creates a string to be used in the code when using the given annotation class.
 *
 * Adds space before `@` so that when the type is fully qualified:
 *  1) the annotation is visible better,
 *  2) two or more annotations are separated.
 */
private fun annotation(annotationClass: Class<out Annotation>): String {
    return " @" + annotationClass.name
}

private fun SourceFileSet.plugValidationIntoBuild(messageTypes: Set<MessageWithFile>) =
    forEachSourceFile(messageTypes, SourceFile<Java>::insertBeforeBuild)

private fun SourceFile<Java>.insertBeforeBuild() {
    at(ValidateBeforeReturn())
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
