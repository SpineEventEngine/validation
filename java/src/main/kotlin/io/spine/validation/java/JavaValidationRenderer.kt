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
import com.google.protobuf.Message
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.lines
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.tools.code.Java
import io.spine.tools.java.codeBlock
import io.spine.validate.NonValidated
import io.spine.validate.Validated
import io.spine.validate.ValidationError
import io.spine.validate.ValidationException
import io.spine.validation.CompiledMessage
import io.spine.validation.java.ValidationCode.Companion.OPTIONAL_ERROR
import io.spine.validation.java.ValidationCode.Companion.VALIDATE
import io.spine.validation.java.point.BuildMethodReturnTypeAnnotation
import io.spine.validation.java.point.BuildPartialReturnTypeAnnotation
import io.spine.validation.java.point.ValidateBeforeReturn
import java.util.*

/**
 * The main Java renderer of the validation library.
 *
 * This rendered is applied to every compiled [Message], even if the message does not
 * have any constraints applied.
 *
 * In particular, the renderer does the following:
 *
 * 1. Makes [Message] implement [io.spine.validate.ValidatableMessage].
 * 2. Declares `validate()` method in [Message] containing the constraints, if any.
 * 3. Declares [supporting members][CodeGenerator.supportingMembers] in [Message], if any,
 * 4. Inserts invocation of `validate()` into [Message.Builder.build] method.
 *
 * Also, it puts the following annotations:
 *
 * 1. [Validated] for [Message.Builder.build] method.
 * 2. [NonValidated] for [Message.Builder.buildPartial] method.
 *
 * Note: there is also [ImplementValidatingBuilder] renderer that makes [Message.Builder]
 * implement [io.spine.validate.ValidatingBuilder] interface. Logically, it is a part
 * of this renderer, but as for now, it is a standalone renderer before this class
 * migrates to PSI.
 */
public class JavaValidationRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        val allCompiledMessages = select(CompiledMessage::class.java).all()
            .associateWith { sources.javaFileOf(it.type) }

        // Adds `validate()` and `implements ValidatableMessage`.
        allCompiledMessages.forEach { (message, file) ->
            val validationCode = ValidationCode(renderer = this, message, file)
            validationCode.generate()
        }

        // Annotates `build()` and `buildPartial()` methods.
        // Adds invocation of `validate()` in `build()`.
        allCompiledMessages.values.distinct()
            .forEach {
                // Though, it seems logical to do this along with adding `validate()`
                // in the `forEach` above; we cannot do that. Insertion points used
                // for the codegen here do not contain the type name, while insertion
                // points for the codegen above contain them.
                //
                // Moving these calls to `forEach` above leads to generation of
                // the same annotations and "plugging code" several times if a compiled
                // message has one or more nested messages (so, several `Builder` classes)
                // within one Java file.
                //
                // Notice usage if `distinct()` before `forEach`. Several `MessageType`s
                // point to the same Java file if a root message declares nested messages.
                //
                // We are not going to address this inconsistency here.
                // This issue will not exist when the class migrates to PSI.
                //
                it.annotateBuildMethod()
                it.annotateBuildPartialMethod()
                it.plugValidationIntoBuild()
            }
    }
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
 *  1) the annotation is visible better;
 *  2) two or more annotations are separated.
 */
private fun annotation(annotationClass: Class<out Annotation>) = " @" + annotationClass.name

private fun SourceFile<Java>.plugValidationIntoBuild() {
    at(ValidateBeforeReturn())
        .withExtraIndentation(2)
        .add(validateBeforeBuild())
}

/**
 * Java code to insert into the end of [Message.Builder.build] method.
 *
 * The generated code invokes `validate()` method. This code assumes there
 * is a variable called `result`. The variable type is the type of the validated message,
 * holding the instance of the message to validate.
 *
 * If one or more validation constraints do not pass, the generated code throws
 * the [ValidationException][io.spine.validate.ValidationException].
 */
private fun validateBeforeBuild(): ImmutableList<String> = codeBlock {
    val result = ReadVar<Message>("result")
    addStatement(
        "\$T error = \$L",
        OPTIONAL_ERROR,
        MethodCall<Optional<ValidationError>>(result, VALIDATE)
    )
    beginControlFlow("if (error.isPresent())")
    addStatement(
        "throw new \$T(error.get().getConstraintViolationList())",
        ValidationException::class.java
    )
    endControlFlow()
}.lines()
