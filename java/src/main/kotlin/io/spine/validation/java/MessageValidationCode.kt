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

package io.spine.validation.java

import com.google.protobuf.Message
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import io.spine.base.FieldPath
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.FieldDeclaration
import io.spine.protodata.java.MethodDeclaration
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.This
import io.spine.protodata.java.getDefaultInstance
import io.spine.protodata.java.render.findClass
import io.spine.string.joinByLines
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addBefore
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.annotate
import io.spine.tools.psi.java.createCodeBlockAdapterFromText
import io.spine.tools.psi.java.createInterfaceReference
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.getFirstByText
import io.spine.tools.psi.java.implement
import io.spine.tools.psi.java.method
import io.spine.tools.psi.java.nested
import io.spine.validate.ConstraintViolation
import io.spine.validate.NonValidated
import io.spine.validate.ValidatableMessage
import io.spine.validate.Validated
import io.spine.validate.ValidatingBuilder
import io.spine.validation.java.MessageValidationCode.ValidateScope.violations

private typealias MessagePsiClass = PsiClass
private typealias BuilderPsiClass = PsiClass

/**
 * Generates validation code for the given [TypeName].
 *
 * This class modifies both the generated Java class of the message and its builder.
 *
 * All validation logic is injected into the [ValidatableMessage.validate] method
 * within the message class itself. This allows a message instance to be validated even
 * after it has been built. Note that this method does not throw exceptions directly;
 * instead, it returns the detected violations, if any.
 *
 * The message builder is modified to invoke the [ValidatableMessage.validate] just before
 * returning the result from its [build][com.google.protobuf.Message.Builder.build] method.
 * If one or more violations are detected, the builder will throw an exception.
 *
 * @param [messageType] The message, for which the validation code is generated.
 * @param [messageClass] The Java class name of the processed message.
 * @param [generators] The generators to apply.
 */
@Suppress("TooManyFunctions") // Small methods representing atomic PSI modifications.
internal class MessageValidationCode(
    private val messageType: TypeName,
    private val messageClass: ClassName,
    private val generators: List<OptionGenerator>
) {

    /**
     * Renders the message validation code into the provided [psiFile].
     *
     * @param psiFile A source file that contains the Java class for the [messageType].
     *
     * @throws IllegalStateException if [psiFile] doesn't contain a Java class for [messageType].
     */
    fun render(psiFile: PsiJavaFile) {
        val optionsCode = generators.map { it.codeFor(messageType) }
        val constraints = optionsCode.flatMap { it.constraints }
        val fields = optionsCode.flatMap { it.fields }
        val methods = optionsCode.flatMap { it.methods }

        val messageClass = psiFile.findClass(messageClass)
        val builderClass = messageClass.nested("Builder")

        execute {
            messageClass.apply {
                implementValidatableMessage()
                declarePublicValidateMethod()
                declarePrivateValidateMethod(constraints)
                declareSupportingFields(fields)
                declareSupportingMethods(methods)
            }
            builderClass.apply {
                implementValidatingBuilder()
                injectValidationIntoBuildMethod()
                annotateBuildReturnType()
                annotateBuildPartialReturnType()
            }
        }
    }

    private fun MessagePsiClass.implementValidatableMessage() {
        val qualifiedName = ValidatableMessage::class.java.canonicalName
        val reference = elementFactory.createInterfaceReference(qualifiedName)
        implement(reference)
    }

    /**
     * Declares `validate()` method in this [MessagePsiClass].
     *
     * This is a `public` implementation of [ValidatableMessage.validate] with
     * [Override] annotation. The actual constraints are contained in its
     * private overloading, that accepts the field path.
     *
     *  @see declarePrivateValidateMethod
     */
    private fun MessagePsiClass.declarePublicValidateMethod() {
        val psiMethod = elementFactory.createMethodFromText(
            """
            public java.util.Optional<io.spine.validate.ValidationError> validate() {
                var noParent = ${FieldPathClass.getDefaultInstance()};
                return validate(noParent);
            }
            """.trimIndent(), this)
        psiMethod.annotate(Override::class.java)
        addLast(psiMethod)
    }

    /**
     * Declares a private `validate(FieldPath)` method that performs all constraint
     * checks for the message.
     *
     * This method implements the actual logic for verifying that the message’s
     * constraints are met. It takes a [FieldPath] parameter that represents the path
     * to the parent field, which triggered the validation, if any. This path is used
     * when constructing constraint violation errors.
     *
     * In typical (top-level) validations, the public `validate()` method is called,
     * which passes an empty field path. However, when validating a nested message
     * (a message field marked with `(validate) = true`), a non-empty field path
     * should be provided. In that case, any constraint violations reported by this
     * method will include the parent field, which actually triggered validation.
     */
    @Suppress("MaxLineLength") // Long method signature.
    private fun MessagePsiClass.declarePrivateValidateMethod(constraints: List<CodeBlock>) {
        val psiMethod = elementFactory.createMethodFromText(
            """
            private java.util.Optional<io.spine.validate.ValidationError> validate($FieldPathClass parent) {
                ${validateMethodBody(constraints)}
            }
            """.trimIndent(), this
        )
        addLast(psiMethod)
    }

    private fun validateMethodBody(constraints: List<CodeBlock>): String =
        if (constraints.isEmpty())
            """
            // This message does not have any validation constraints.
            return java.util.Optional.empty();
            """.trimIndent()
        else
            """
            var $violations = new java.util.ArrayList<io.spine.validate.ConstraintViolation>();
            
            ${constraints.joinByLines()}
            
            if (!$violations.isEmpty()) {
                var error = io.spine.validate.ValidationError.newBuilder()
                    .addAllConstraintViolation($violations)
                    .build();
                return java.util.Optional.of(error);
            } else {
                return java.util.Optional.empty();
            }
            """.trimIndent()

    private fun MessagePsiClass.declareSupportingFields(fields: List<FieldDeclaration<*>>) =
        fields.forEach {
            addLast(elementFactory.createFieldFromText(it.toString(), this))
        }

    private fun MessagePsiClass.declareSupportingMethods(methods: List<MethodDeclaration>) =
        methods.forEach {
            addLast(elementFactory.createMethodFromText(it.toString(), this))
        }

    private fun BuilderPsiClass.implementValidatingBuilder() {
        val qualifiedName = ValidatingBuilder::class.java.canonicalName
        val genericParameter = messageClass.canonical
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
    private fun BuilderPsiClass.injectValidationIntoBuildMethod() = method("build")
        .run {
            val returningResult = getFirstByText("return result;")
            val runValidation = elementFactory.createCodeBlockAdapterFromText(
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

    /**
     * Scope variables available within `validate(FieldPath)` method.
     */
    internal object ValidateScope {
        val violations = ReadVar<MutableList<ConstraintViolation>>("violations")
        val parentPath = ReadVar<FieldPath>("parent")
    }

    /**
     * Scope variables available within the whole message class.
     */
    internal object MessageScope {
        val message = This<Message>(explicit = false)
    }
}
