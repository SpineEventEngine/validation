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

import com.google.common.reflect.TypeToken
import com.intellij.psi.PsiClass
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.This
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.createInterfaceReference
import io.spine.tools.psi.java.implement
import io.spine.validate.ConstraintViolation
import io.spine.validate.ValidatableMessage
import io.spine.validate.ValidationError
import io.spine.validation.CompilationMessage
import io.spine.validation.Rule
import java.lang.reflect.Type
import java.util.*

/**
 * Generates validation code for the given [CompilationMessage].
 *
 * Serves as a method object for the [JavaValidationRenderer] passed
 * to the constructor.
 *
 * In particular, this class does the following:
 *
 * 1. Makes the [message] implement [ValidatableMessage] interface.
 * 2. Adds implementation of [ValidatableMessage.validate] method.
 * 3. Declares supporting members, if any.
 */
internal class ValidationCode(
    private val renderer: JavaValidationRenderer,
    private val message: CompilationMessage,
) {
    private val messageType: TypeName = message.name
    private val constraints = mutableListOf<CodeBlock>()
    private val supportingFields = mutableListOf<FieldSpec>()
    private val supportingMethods = mutableListOf<MethodSpec>()

    fun generate(psiClass: PsiClass) {
        message.ruleList.forEach(::generateRule)
        with(psiClass) {
            implementInterface()
            implementMethod()
            declareSupportingFields()
            declareSupportingMethods()
        }
    }

    private fun PsiClass.implementInterface() {
        val qualifiedName = ValidatableMessage::class.java.canonicalName
        val reference = elementFactory.createInterfaceReference(qualifiedName)
        implement(reference)
    }

    private fun PsiClass.implementMethod() {
        val validateMethod = ValidateMethodCode(constraints)
        addLast(validateMethod.generate(this))
    }

    private fun PsiClass.declareSupportingFields() =
        supportingFields.forEach {
            addLast(elementFactory.createFieldFromText(it.toString(), this))
        }

    private fun PsiClass.declareSupportingMethods() =
        supportingMethods.forEach {
            addLast(elementFactory.createMethodFromText(it.toString(), this))
        }

    private fun generateRule(rule: Rule) {
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
            violationList = VIOLATIONS
        )

    companion object {

        const val VALIDATE: String = "validate"

        @JvmField
        val OPTIONAL_ERROR: Type = object : TypeToken<Optional<ValidationError>>() {}.type

        @JvmField
        val VIOLATIONS: Expression<MutableList<ConstraintViolation>> = ReadVar("violations")
    }
}
