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
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.Java
import io.spine.tools.java.codeBlock
import io.spine.validate.NonValidated
import io.spine.validate.Validated
import io.spine.validate.ValidationError
import io.spine.validate.ValidationException
import io.spine.validation.MessageValidation
import io.spine.validation.java.ValidationCode.Companion.OPTIONAL_ERROR
import io.spine.validation.java.ValidationCode.Companion.VALIDATE
import io.spine.validation.java.point.BuildMethodReturnTypeAnnotation
import io.spine.validation.java.point.BuildPartialReturnTypeAnnotation
import io.spine.validation.java.point.ValidateBeforeReturn
import java.util.*

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

    // TODO:2025-01-16:yevhenii.nadtochii: Can we make it non-nullable in `Member`?
    //  It is probably third time I make such an override. Anyway, we always use
    //  `typeSystem` and `context` within `render()` invocation.
    public override val typeSystem: TypeSystem by lazy { super.typeSystem!! }

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        // Runs for each compiled message no matter whether it has validation constrains or not.
        val validations = select(MessageValidation::class.java).all()

        // Adds `validate()` and `implements ValidatableMessage`.
        val validationsWithFiles = validations.associateWith { sources.javaFileOf(it.type) }
        validationsWithFiles.forEach { (constraints, file) ->
            file.addValidationCode(constraints)
        }

        // Annotates `build()`, `buildPartial()`. Adds invocation of `validate()` in `build()`.
        validationsWithFiles.values.distinct()
            .forEach {
                // Though, it seems logical to do it along with adding `validate()`
                // in the `forEach` above; we cannot do that. Insertion points used
                // for the codegen below do not contain the type, while insertion points
                // for `validate()` contain. Moving these calls to `forEach` above leads
                // to annotations being applied several times if a compiled message has
                // one or more nested messages.
                it.annotateBuildMethod()
                it.annotateBuildPartialMethod()
                it.plugValidationIntoBuild()
            }
    }

    private fun SourceFile<Java>.addValidationCode(validation: MessageValidation) {
        val rendered = this@JavaValidationRenderer
        val validationCode = ValidationCode(rendered, validation, this)
        validationCode.generate()
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
 *  1) the annotation is visible better,
 *  2) two or more annotations are separated.
 */
private fun annotation(annotationClass: Class<out Annotation>) = " @" + annotationClass.name

private fun SourceFile<Java>.plugValidationIntoBuild() {
    at(ValidateBeforeReturn())
        .withExtraIndentation(2)
        .add(validateBeforeBuild())
}

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
