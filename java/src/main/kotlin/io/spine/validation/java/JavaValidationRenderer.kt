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
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.java.render.findClass
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.type.TypeSystem
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.createInterfaceReference
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.implement
import io.spine.tools.psi.java.method
import io.spine.validate.NonValidated
import io.spine.validate.Validated
import io.spine.validate.ValidatingBuilder
import io.spine.validation.CompilationMessage
import io.spine.validation.java.psi.addBefore
import io.spine.validation.java.psi.createStatementsFromText
import io.spine.validation.java.setonce.deepSearch

/**
 * The main Java renderer of the validation library.
 *
 * This rendered is applied to every compiled [Message], even if the message does not
 * have any declared constraints.
 *
 * In particular, the renderer does the following:
 *
 * 1. Makes [Message] implement [io.spine.validate.ValidatableMessage] interface.
 * 2. Declares `validate()` method in [Message] containing the constraints, if any.
 * 3. Declares supporting fields and methods in [Message], if any.
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

    /**
     * Exposes [typeSystem] property, so that the code generation context could use it.
     */
    public override val typeSystem: TypeSystem
        get() = super.typeSystem

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        val allCompilationMessages = select(CompilationMessage::class.java).all()
            .associateWith { sources.javaFileOf(it.type) }

        // Adds `implements ValidatableMessage`, `validate()` and supporting members.
        allCompilationMessages.forEach { (message, file) ->
            val psiFile = file.psi() as PsiJavaFile

            val validationCode = MessageValidationCode(renderer = this, message)
            val messageClass = message.type.javaClassName(typeSystem)
            val psiMessageClass = psiFile.findClass(messageClass)
            execute {
                validationCode.generate(psiMessageClass)
            }

            val psiBuilderClass = psiMessageClass.innerClasses.first { it.name == "Builder" }
            execute {
                val validatingBuilder = ValidatingBuilder::class.java.canonicalName
                val reference = elementFactory.createInterfaceReference(
                    validatingBuilder, messageClass.canonical)
                psiBuilderClass.implement(reference)

                psiBuilderClass.method("build").run {
                    returnTypeElement!!.addAnnotation(Validated::class.qualifiedName!!)
                    val returnResult = deepSearch("return result;")
                    val runValidation = runValidation()
                    body!!.addBefore(runValidation, returnResult)
                }

                psiBuilderClass.method("buildPartial").run {
                    returnTypeElement!!.addAnnotation(NonValidated::class.qualifiedName!!)
                }
            }

            file.overwrite(psiFile.text)
        }
    }
}

private fun runValidation() = elementFactory.createStatementsFromText(
    """
    java.util.Optional<io.spine.validate.ValidationError> error = result.validate();
    if (error.isPresent()) {
        throw new io.spine.validate.ValidationException(error.get().getConstraintViolationList());
    }
""".trimIndent(), null
)
