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

package io.spine.validation.test

import io.spine.server.query.select
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.jvm.CodeBlock
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.tools.compiler.jvm.field
import io.spine.tools.validation.java.expression.orElse
import io.spine.tools.validation.java.expression.stringValueOf
import io.spine.tools.validation.java.expression.withStringPlaceholders
import io.spine.validate.ConstraintViolation
import io.spine.validation.api.expression.constraintViolation
import io.spine.validation.api.expression.stringify
import io.spine.validation.api.generate.MessageScope.message
import io.spine.validation.api.generate.OptionGenerator
import io.spine.validation.api.generate.SingleOptionCode
import io.spine.validation.api.generate.ValidateScope.parentName
import io.spine.validation.api.generate.ValidateScope.violations
import io.spine.validation.test.money.CurrencyMessage

/**
 * The generator for the `(currency)` option.
 */
internal class CurrencyGenerator : OptionGenerator() {

    /**
     * All `(currency)`-marked messages in the current compilation process.
     */
    private val allCurrencyMessages by lazy {
        querying.select<CurrencyMessage>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> {
        val currencyMessage = allCurrencyMessages.find { it.type == type }
        if (currencyMessage == null) {
            return emptyList()
        }
        val code = GenerateCurrency(currencyMessage).code()
        return listOf(code)
    }
}

/**
 * Generates code for a single application of the `(currency)` option
 * represented by the [view].
 */
private class GenerateCurrency(private val view: CurrencyMessage) {

    private val minorField = view.minorUnitField
    private val minorThreshold = view.currency.minorUnits

    /**
     * Returns the generated code.
     */
    fun code(): SingleOptionCode {
        val minorGetter = message.field(minorField)
            .getter<Int>()
        val constraint = CodeBlock(
            """
            if ($minorGetter >= $minorThreshold) {
                var typeName =  ${parentName.orElse(view.type)};
                var violation = ${violation(ReadVar("typeName"), minorGetter)};
                $violations.add(violation);
            }
            """.trimIndent()
        )
        return SingleOptionCode(constraint)
    }

    private fun violation(
        typeName: Expression<TypeName>,
        minorValue: Expression<Int>
    ): Expression<ConstraintViolation> {
        val typeNameStr = typeName.stringify()
        val placeholders = supportedPlaceholders(minorValue)
        val errorMessage = withStringPlaceholders(view.errorMessage, placeholders, CURRENCY)
        return constraintViolation(errorMessage, typeNameStr, fieldPath = null, fieldValue = null)
    }

    private fun supportedPlaceholders(
        minorValue: Expression<Int>
    ): Map<String, Expression<String>> =
        mapOf("minor.value" to minorField.stringValueOf(minorValue))
}
