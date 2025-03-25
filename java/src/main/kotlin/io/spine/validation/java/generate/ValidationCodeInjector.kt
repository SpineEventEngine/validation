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

package io.spine.validation.java.generate

import com.google.protobuf.Message
import com.intellij.psi.PsiClass
import io.spine.base.FieldPath
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.FieldDeclaration
import io.spine.protodata.java.MethodDeclaration
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.This
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
import io.spine.type.TypeName
import io.spine.validate.ConstraintViolation
import io.spine.validate.NonValidated
import io.spine.validate.ValidatableMessage
import io.spine.validate.Validated
import io.spine.validate.ValidatingBuilder
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.violations
import io.spine.validation.java.expression.FieldPathClass
import io.spine.validation.java.expression.ObjectsClassName
import io.spine.validation.java.expression.TypeNameClass
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentName
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.parentPath

/**
 * A [PsiClass] holding an instance of [Message].
 */
private typealias MessagePsiClass = PsiClass

/**
 * A [PsiClass] holding an instance of [Message.Builder].
 */
private typealias BuilderPsiClass = PsiClass

/**
 * Injects the message validation code into the given PSI class.
 *
 * This class modifies both the Java class of the message and its builder.
 *
 * All validation logic is injected into the [ValidatableMessage.validate] method
 * within the message class itself. This allows a message instance to be validated even
 * after it has been built. Note that this method does not throw exceptions directly;
 * instead, it returns the detected violations, if any.
 *
 * The message builder is modified to invoke the [ValidatableMessage.validate] just before
 * returning the result from its [build][com.google.protobuf.Message.Builder.build] method.
 * If one or more violations are detected, the builder will throw an exception.
 */
internal class ValidationCodeInjector {

    /**
     * Injects the provided validation [code] into the PSI class of the message.
     */
    fun inject(code: MessageValidationCode, messageClass: PsiClass) {
        val builderClass = messageClass.nested("Builder")
        execute {
            messageClass.apply {
                implementValidatableMessage()
                declareDefaultValidateMethod()
                declareValidateMethod(code.constraints)
                declareSupportingFields(code.fields)
                declareSupportingMethods(code.methods)
            }
            builderClass.apply {
                implementValidatingBuilder(messageClass)
                injectValidationIntoBuildMethod()
                annotateBuildReturnType()
                annotateBuildPartialReturnType()
            }
        }
    }

    /**
     * Scope variables available within `validate(FieldPath)` method.
     */
    object ValidateScope {
        val violations = ReadVar<MutableList<ConstraintViolation>>("violations")
        val parentPath = ReadVar<FieldPath>("parentPath")
        val parentName = ReadVar<TypeName?>("parentName")
    }

    /**
     * Scope variables available within the whole message class.
     */
    object MessageScope {
        val message = This<Message>(explicit = false)
    }
}

/**
 * Makes this [MessagePsiClass] implement [ValidatableMessage] interface.
 */
private fun MessagePsiClass.implementValidatableMessage() {
    val qualifiedName = ValidatableMessage::class.java.canonicalName
    val reference = elementFactory.createInterfaceReference(qualifiedName)
    implement(reference)
}

/**
 * Declares the `validate()` method in this [MessagePsiClass].
 *
 * This is an implementation of [ValidatableMessage.validate] that doesn't accept
 * any parameters. The actual constraints are contained in its [overload][declareValidateMethod],
 * to which this method delegates.
 */
// TODO:2025-03-12:yevhenii.nadtochii: Remove it in a favour of the default implementation
//  provided in the `ValidatableMessage` interface.
//  See issue: https://github.com/SpineEventEngine/validation/issues/198
private fun MessagePsiClass.declareDefaultValidateMethod() {
    val psiMethod = elementFactory.createMethodFromText(
        """
        public java.util.Optional<io.spine.validate.ValidationError> validate() {
            var noParentPath = $FieldPathClass.getDefaultInstance();
            return validate(noParentPath, null);
        }
        """.trimIndent(), this)
    psiMethod.annotate(Override::class.java)
    addLast(psiMethod)
}

/**
 * Declares the `validate(parentPath, parentName)` method in this [MessagePsiClass].
 *
 * This method implements the logic for verifying that the message’s constraints are met.
 * It takes the parent path and name as arguments to preserve this information for cases
 * when in-depth validation takes place. This data is used to construct constraint violations.
 *
 * In typical use cases of validating the top-level messages, the [ValidatableMessage.validate]
 * method is called, which does not need the parent info. However, when validating a nested message
 * (a message field marked with `(validate) = true`), a non-empty field path and parent name should
 * be provided. In that case, the reported constraint violations will include the parent field
 * and name.
 */
private fun MessagePsiClass.declareValidateMethod(constraints: List<CodeBlock>) {
    val psiMethod = elementFactory.createMethodFromText(
        """
        public java.util.Optional<io.spine.validate.ValidationError> validate($FieldPathClass $parentPath, $TypeNameClass $parentName) {
            ${ObjectsClassName}.requireNonNull($parentPath);
            ${validateMethodBody(constraints)}
        }
        """.trimIndent(), this
    )
    psiMethod.annotate(Override::class.java)
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

/**
 * Adds declarations of the given [fields] to this [MessagePsiClass].
 */
private fun MessagePsiClass.declareSupportingFields(fields: List<FieldDeclaration<*>>) =
    fields.forEach {
        addLast(elementFactory.createFieldFromText(it.toString(), this))
    }

/**
 * Adds declarations of the given [methods] to this [MessagePsiClass].
 */
private fun MessagePsiClass.declareSupportingMethods(methods: List<MethodDeclaration>) =
    methods.forEach {
        addLast(elementFactory.createMethodFromText(it.toString(), this))
    }

/**
 * Makes this [BuilderPsiClass] implement [ValidatingBuilder] interface using
 * the provided [message] class name as its type parameter.
 */
private fun BuilderPsiClass.implementValidatingBuilder(message: PsiClass) {
    val qualifiedName = ValidatingBuilder::class.java.canonicalName
    val genericParameter = message.qualifiedName!!
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

/**
 * Annotates the return type of [Message.Builder.build] method of this [BuilderPsiClass]
 * with [Validated] annotation.
 */
private fun BuilderPsiClass.annotateBuildReturnType() = method("build")
    .run { returnTypeElement!!.addAnnotation(Validated::class.qualifiedName!!) }

/**
 * Annotates the return type of [Message.Builder.buildPartial] method of this
 * [BuilderPsiClass] with [NonValidated] annotation.
 */
private fun BuilderPsiClass.annotateBuildPartialReturnType() = method("buildPartial")
    .run { returnTypeElement!!.addAnnotation(NonValidated::class.qualifiedName!!) }
