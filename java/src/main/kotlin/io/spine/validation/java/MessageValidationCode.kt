/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.validation.java

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.This
import io.spine.protodata.java.javaClassName
import io.spine.protodata.java.render.findClass
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.annotate
import io.spine.tools.psi.java.createInterfaceReference
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.implement
import io.spine.tools.psi.java.method
import io.spine.validate.ConstraintViolation
import io.spine.validate.NonValidated
import io.spine.validate.ValidatableMessage
import io.spine.validate.Validated
import io.spine.validate.ValidatingBuilder
import io.spine.validation.CompilationMessage
import io.spine.validation.Rule
import io.spine.validation.java.psi.addBefore
import io.spine.validation.java.psi.createStatementsFromText
import io.spine.validation.java.psi.deepSearch
import io.spine.validation.java.psi.nested

private typealias MessagePsiClass = PsiClass
private typealias BuilderPsiClass = PsiClass

/**
 * Generates validation code for the given [CompilationMessage].
 *
 * This class modifies both the generated Java class of the message and its builder.
 *
 * All validation logic is injected into the [ValidatableMessage.validate] method
 * within the message class itself. This allows a message instance to be validated even
 * after it has been built. Note that this method does not throw exceptions directly;
 * instead, it returns any detected violations, if any.
 *
 * The message builder is modified to invoke the [ValidatableMessage.validate] just
 * before returning from its [build][com.google.protobuf.Message.Builder.build] method.
 * If one or more violations are detected, the builder will throw an exception.
 */
@Suppress("TooManyFunctions") // Small methods representing atomic PSI modifications.
internal class MessageValidationCode(
    private val renderer: JavaValidationRenderer,
    private val message: CompilationMessage,
) {
    private val messageType: TypeName = message.name
    private val messageClassName = messageType.javaClassName(renderer.typeSystem)
    private val supportingFields = mutableListOf<FieldSpec>()
    private val supportingMethods = mutableListOf<MethodSpec>()
    private val constraints = mutableListOf<CodeBlock>()

    /**
     * Renders the message validation code into the provided [psiFile].
     *
     * @param psiFile A source file with the generated Java class for the [message].
     *
     * @throws IllegalStateException if [psiFile] doesn't contain a Java class for the [message].
     */
    fun render(psiFile: PsiJavaFile) {
        message.ruleList.forEach(::generate)
        val messageClass = psiFile.findClass(messageClassName)
        val builderClass = messageClass.nested("Builder")
        execute {
            messageClass.apply {
                implementValidatableMessage()
                declareValidateMethod()
                declareSupportingFields()
                declareSupportingMethods()
            }
            builderClass.apply {
                implementValidatingBuilder()
                injectValidationIntoBuildMethod()
                annotateBuildReturnType()
                annotateBuildPartialReturnType()
            }
        }
    }

    /**
     * Generates a Java [constraint][constraints], [supportingFields] and [supportingMethods]
     * for the passed [rule].
     *
     * A single rule always generates a single Java constraint represented by a [CodeBlock].
     * The number of supported members is not restricted. A single rule may generate zero,
     * one, or more supporting members.
     */
    private fun generate(rule: Rule) {
        val context = newContext(rule, message)
        val generator = generatorFor(context)
        constraints.add(generator.code())
        supportingFields.addAll(generator.supportingFields())
        supportingMethods.addAll(generator.supportingMethods())
    }

    private fun newContext(rule: Rule, message: CompilationMessage) =
        GenerationContext(
            client = renderer,
            typeSystem = renderer.typeSystem,
            rule = rule,
            msg = This(),
            validatedType = messageType,
            protoFile = message.type.file,
            violationList = violations
        )

    private fun MessagePsiClass.implementValidatableMessage() {
        val qualifiedName = ValidatableMessage::class.java.canonicalName
        val reference = elementFactory.createInterfaceReference(qualifiedName)
        implement(reference)
    }

    private fun MessagePsiClass.declareValidateMethod() {
        // `CodeBlock` is printed with a new line in the end, so no need to call `joinByLine()`.
        // And we have to trim because we don't need a trailing empty line.
        val formattedConstraints = constraints.joinToString(separator = "").trim()
        val psiMethod = elementFactory.createMethodFromText(
            """
            |public java.util.Optional<io.spine.validate.ValidationError> validate() {
            |    ${validateMethodBody(formattedConstraints)}
            |}
            """.trimMargin(), this
        )
        psiMethod.annotate(Override::class.java)
        addLast(psiMethod)
    }

    private fun validateMethodBody(formattedConstraints: String): String =
        if (formattedConstraints.isEmpty()) {
            "return java.util.Optional.empty();"
        } else {
            """
            var $violations = new java.util.ArrayList<io.spine.validate.ConstraintViolation>();
            $formattedConstraints
            if (!$violations.isEmpty()) {
                var error = io.spine.validate.ValidationError.newBuilder()
                    .addAllConstraintViolation($violations)
                    .build();
                return java.util.Optional.of(error);
            } else {
                return java.util.Optional.empty();
            }
            """.trimIndent()
        }

    private fun MessagePsiClass.declareSupportingFields() =
        supportingFields.forEach {
            addLast(elementFactory.createFieldFromText(it.toString(), this))
        }

    private fun MessagePsiClass.declareSupportingMethods() =
        supportingMethods.forEach {
            addLast(elementFactory.createMethodFromText(it.toString(), this))
        }

    private fun BuilderPsiClass.implementValidatingBuilder() {
        val qualifiedName = ValidatingBuilder::class.java.canonicalName
        val genericParameter = messageClassName.canonical
        val reference = elementFactory.createInterfaceReference(qualifiedName, genericParameter)
        implement(reference)
    }

    /**
     * Injects an invocation of [ValidatableMessage.validate] method into the end
     * of `build()` method body of this [PsiClass].
     *
     * The validation code is executed right before returning from the `build()`.
     * If one or more constraints are violated, the injected snippet will throw.
     */
    private fun BuilderPsiClass.injectValidationIntoBuildMethod() = method("build").run {
        val returningResult = deepSearch("return result;")
        val runValidation = elementFactory.createStatementsFromText(
            """
            java.util.Optional<io.spine.validate.ValidationError> error = result.validate();
            if (error.isPresent()) {
                var violations = error.get().getConstraintViolationList();
                throw new io.spine.validate.ValidationException(violations);
            }
            """.trimIndent(), this
        )
        body!!.addBefore(runValidation, returningResult)
    }

    private fun BuilderPsiClass.annotateBuildReturnType() = method("build")
        .run { returnTypeElement!!.addAnnotation(Validated::class.qualifiedName!!) }

    private fun BuilderPsiClass.annotateBuildPartialReturnType() = method("buildPartial")
        .run { returnTypeElement!!.addAnnotation(NonValidated::class.qualifiedName!!) }

    private companion object {
        val violations = ReadVar<MutableList<ConstraintViolation>>("violations")
    }
}
