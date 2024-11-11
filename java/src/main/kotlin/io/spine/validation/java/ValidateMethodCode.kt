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
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.call
import io.spine.protodata.java.newBuilder
import io.spine.protodata.render.InsertionPoint
import io.spine.protodata.render.codeLine
import io.spine.tools.code.Java
import io.spine.tools.java.codeBlock
import io.spine.tools.java.methodSpec
import io.spine.validate.ConstraintViolation
import io.spine.validate.ValidationError
import io.spine.validation.java.ValidationCode.Companion.OPTIONAL_ERROR
import io.spine.validation.java.ValidationCode.Companion.VALIDATE
import io.spine.validation.java.ValidationCode.Companion.VIOLATIONS
import java.lang.System.lineSeparator
import java.util.*
import javax.lang.model.element.Modifier.PUBLIC

/**
 * Wraps the passed constraints code into a method.
 */
internal class ValidateMethodCode(
    private val messageType: TypeName,
    private val constraintsCode: CodeBlock
) {

    fun generate(): MethodSpec {
        val code = codeBlock {
            addStatement(newAccumulator())
            add(constraintsCode)
            add(extraInsertionPoint())
            add(generateValidationError())
        }
        return methodSpec(VALIDATE) {
            returns(OPTIONAL_ERROR)
            addModifiers(PUBLIC)
            addCode(code)
        }
    }

    private fun extraInsertionPoint(): CodeBlock {
        val insertionPoint: InsertionPoint = ExtraValidation(messageType)
        val line = Java.comment(insertionPoint.codeLine) + lineSeparator()
        return CodeBlock.of(line)
    }

    companion object {
        private const val RETURN_LITERAL = "return \$L"

        private fun newAccumulator(): CodeBlock = CodeBlock.of(
            "var \$L = new \$T<\$T>()",
            VIOLATIONS,
            ArrayList::class.java,
            ConstraintViolation::class.java
        )

        private fun generateValidationError(): CodeBlock = codeBlock {
            beginControlFlow("if (!\$L.isEmpty())", VIOLATIONS)
            val errorBuilder = ClassName(ValidationError::class.java).newBuilder()
                .chainAddAll("constraint_violation", VIOLATIONS).chainBuild<Any>()
            val optional = ClassName(Optional::class.java)
            val optionalOf = optional.call<Any>("of", ImmutableList.of(errorBuilder))
            addStatement(RETURN_LITERAL, optionalOf)
            nextControlFlow("else")
            val optionalEmpty = optional.call<Any>("empty")
            addStatement(RETURN_LITERAL, optionalEmpty)
            endControlFlow()
        }
    }
}
