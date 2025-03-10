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

package io.spine.validation.java.generate.option

import com.google.rpc.Code
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.name
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.field
import io.spine.validation.ValidateField
import io.spine.validation.java.expression.AnyPackerClass
import io.spine.validation.java.expression.EmptyFieldCheck
import io.spine.validation.java.expression.ValidatableMessageClass
import io.spine.validation.java.expression.ValidationErrorClass
import io.spine.validation.java.generate.FieldOptionCode
import io.spine.validation.java.generate.FieldOptionGenerator
import io.spine.validation.java.generate.ValidationCodeInjector.MessageScope.message
import io.spine.validation.java.generate.ValidationCodeInjector.ValidateScope.violations

/**
 * The generator for `(validate)` option.
 *
 * Generates code for a single field represented by the provided [view].
 */
internal class ValidateFieldGenerator(
    private val view: ValidateField,
    override val converter: JavaValueConverter
) : FieldOptionGenerator, EmptyFieldCheck {

    private val field = view.subject
    private val fieldType = field.type
    private val getter = message.field(field).getter<Any>()

    override fun generate(): FieldOptionCode = when {
        fieldType.isAny -> {
            val constraint = CodeBlock(
                """
                if (!${field.hasDefaultValue()}) {
                    var unpacked = $AnyPackerClass.unpack($getter);
                    if (unpacked instanceof $ValidatableMessageClass) {
                        (($ValidatableMessageClass) unpacked).validate()
                            .map($ValidationErrorClass::getConstraintViolationList)
                            .ifPresent($violations::addAll);
                    }
                }
                """.trimIndent()
            )
            FieldOptionCode(CodeBlock(""))
        }

        fieldType.isMessage -> {
            val constraint = CodeBlock(
                """
                if (!${field.hasDefaultValue()}) {
                    $getter.validate()
                        .map($ValidationErrorClass::getConstraintViolationList)
                        .ifPresent($violations::addAll);
                }
                """.trimIndent()
            )
            FieldOptionCode(CodeBlock(""))
        }

        fieldType.isList -> {
            val constraint = CodeBlock(
                """
                if (!${field.hasDefaultValue()}) {
                    for (var element : $getter) {
                        element.validate()
                               .map($ValidationErrorClass::getConstraintViolationList)
                               .ifPresent($violations::addAll);
                    }
                }
                """.trimIndent()
            )
            FieldOptionCode(CodeBlock(""))
        }

        fieldType.isMap -> {
            FieldOptionCode(CodeBlock(""))
        }

        else -> error {
            "The field type `${fieldType.name}` is not supported by `ValidateFieldGenerator`." +
                    " Please ensure that the supported field types in this generator match those" +
                    " used by `ValidatePolicy` when validating the `ValidateFieldDiscovered` event."
        }
    }
}
