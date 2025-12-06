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

package io.spine.tools.validation.java.generate.option

import io.spine.base.FieldPath
import io.spine.server.query.select
import io.spine.string.lowerCamelCase
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.jvm.CodeBlock
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.tools.validation.java.expression.constraintViolation
import io.spine.tools.validation.java.expression.joinToString
import io.spine.tools.validation.java.expression.orElse
import io.spine.tools.validation.java.expression.resolve
import io.spine.tools.validation.java.expression.templateString
import io.spine.tools.validation.java.generate.OptionGenerator
import io.spine.tools.validation.java.generate.SingleOptionCode
import io.spine.validate.ConstraintViolation
import io.spine.validation.CHOICE
import io.spine.validation.ChoiceOneof
import io.spine.validation.ErrorPlaceholder
import io.spine.validation.ErrorPlaceholder.GROUP_PATH
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.jvm.expression.stringify
import io.spine.validation.jvm.generate.ValidateScope.parentName
import io.spine.validation.jvm.generate.ValidateScope.parentPath
import io.spine.validation.jvm.generate.ValidateScope.violations

/**
 * The generator for the `(choice)` option.
 */
internal class ChoiceGenerator : OptionGenerator() {

    /**
     * All `oneof` groups with `(choice).enabled = true` in the current compilation process.
     */
    private val allChoiceOneofs by lazy {
        querying.select<ChoiceOneof>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allChoiceOneofs
            .filter { it.id.type == type }
            .map { GenerateChoice(it).code() }
}

/**
 * Generates code for a single application of the `(choice)` option
 * represented by the [view].
 */
private class GenerateChoice(private val view: ChoiceOneof) {

    private val oneof = view.subject

    /**
     * Returns the generated code.
     */
    fun code(): SingleOptionCode {
        val groupName = oneof.name
        val caseField = "${groupName.value.lowerCamelCase()}Case_"
        val constraint = CodeBlock(
            """
            if ($caseField == 0) {
                var groupPath = ${parentPath.resolve(groupName)};
                var typeName =  ${parentName.orElse(oneof.declaringType)};
                var violation = ${violation(ReadVar("groupPath"), ReadVar("typeName"))};
                $violations.add(violation);
            }
            """.trimIndent()
        )
        return SingleOptionCode(constraint)
    }

    private fun violation(
        groupPath: Expression<FieldPath>,
        typeName: Expression<io.spine.type.TypeName>
    ): Expression<ConstraintViolation> {
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(groupPath, typeNameStr)
        val errorMessage = templateString(view.errorMessage, placeholders, CHOICE)
        return constraintViolation(errorMessage, typeNameStr, groupPath, fieldValue = null)
    }

    private fun supportedPlaceholders(
        groupPath: Expression<FieldPath>,
        typeName: Expression<String>,
    ): Map<ErrorPlaceholder, Expression<String>> = mapOf(
        GROUP_PATH to groupPath.joinToString(),
        PARENT_TYPE to typeName
    )
}
