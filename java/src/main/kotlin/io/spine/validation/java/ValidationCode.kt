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

import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import com.squareup.javapoet.CodeBlock
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.TypedInsertionPoint
import io.spine.protodata.render.SourceAtLine
import io.spine.protodata.render.SourceFile
import io.spine.text.TextFactory.lineSplitter
import io.spine.tools.code.Java
import io.spine.validate.ConstraintViolation
import io.spine.validate.ValidatableMessage
import io.spine.validate.ValidationError
import io.spine.validation.CompiledMessage
import java.lang.System.lineSeparator
import java.lang.reflect.Type
import java.util.*

/**
 * Generates validation code for the given [CompiledMessage].
 *
 * Serves as a method object for the [JavaValidationRenderer]
 * passed to the constructor.
 */
internal class ValidationCode(
    private val renderer: JavaValidationRenderer,
    private val message: CompiledMessage,
    private val sourceFile: SourceFile<Java>
) {
    private val messageType: TypeName = message.name

    /**
     * Generates the code in the linked source file.
     */
    fun generate() {
        implementValidatableMessage()
        handleConstraints()
    }

    private fun implementValidatableMessage() {
        val atMessageImplements =
            sourceFile.at(TypedInsertionPoint.MESSAGE_IMPLEMENTS.forType(messageType))
                .withExtraIndentation(1)
        atMessageImplements.add(ClassName(ValidatableMessage::class.java).toString() + ",")
    }

    private fun handleConstraints() {
        classScope().apply {
            val constraints = ValidationConstraintsCode.generate(renderer, message)
            add(validateMethod(constraints.codeBlock()))
            add(constraints.supportingMembersCode())
        }
    }

    private fun classScope(): SourceAtLine =
        sourceFile.at(TypedInsertionPoint.CLASS_SCOPE.forType(this.messageType))

    private fun validateMethod(constraintsCode: CodeBlock): ImmutableList<String> {
        val validateMethod = ValidateMethodCode(messageType, constraintsCode)
        val methodSpec = validateMethod.generate()
        val lines = ImmutableList.builder<String>()
        lines.addAll(lineSplitter().split(methodSpec.toString()))
            .add(lineSeparator())
        return lines.build()
    }

    companion object {

        const val VALIDATE: String = "validate"

        @JvmField
        val OPTIONAL_ERROR: Type = object : TypeToken<Optional<ValidationError>>() {}.type

        @JvmField
        val VIOLATIONS: Expression<MutableList<ConstraintViolation>> = ReadVar("violations")
    }
}
