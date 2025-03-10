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

import io.spine.protodata.ast.isAny
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.name
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.field
import io.spine.protodata.java.getDefaultInstance
import io.spine.validation.ValidateField
import io.spine.validation.java.expression.AnyClass
import io.spine.validation.java.expression.AnyPackerClass
import io.spine.validation.java.expression.MessageClass
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
internal class ValidateFieldGenerator(private val view: ValidateField) : FieldOptionGenerator {

    private val field = view.subject
    private val fieldType = field.type
    private val getter = message.field(field).getter<Any>()

    override fun generate(): FieldOptionCode = FieldOptionCode(constraint())

    private fun constraint() = when {
        fieldType.isMessage -> {
            val condition =
                if (fieldType.message.isAny)
                    "$getter != ${AnyClass.getDefaultInstance()} && $AnyPackerClass.unpack($getter) instanceof $ValidatableMessageClass $validatable"
                else
                    "(($MessageClass) $getter) instanceof $ValidatableMessageClass $validatable"
            CodeBlock(
                """
                if ($condition) {
                    $VALIDATE_VALIDATABLE
                }
                """.trimIndent()
            )
        }

        fieldType.isList -> {
            val condition =
                if (fieldType.list.isAny)
                    "element != ${AnyClass.getDefaultInstance()} && $AnyPackerClass.unpack(element) instanceof $ValidatableMessageClass $validatable"
                else
                    "(($MessageClass) element) instanceof $ValidatableMessageClass $validatable"
            CodeBlock(
                """
                for (var element : $getter) {
                    if ($condition) {
                        $VALIDATE_VALIDATABLE
                    }
                }
                """.trimIndent()
            )
        }

        fieldType.isMap -> {
            val condition =
                if (fieldType.map.valueType.isAny)
                    "element != ${AnyClass.getDefaultInstance()} && $AnyPackerClass.unpack(element) instanceof $ValidatableMessageClass $validatable"
                else
                    "(($MessageClass) element) instanceof $ValidatableMessageClass $validatable"
            CodeBlock(
                """
                for (var element : $getter.values()) {
                    if ($condition) {
                        $VALIDATE_VALIDATABLE
                    }
                }     
                """.trimIndent()
            )
        }

        else -> error(
            "The field type `${fieldType.name}` is not supported by `ValidateFieldGenerator`." +
                    " Please ensure that the supported field types in this generator match those" +
                    " used by `ValidatePolicy` when validating the `ValidateFieldDiscovered` event."
        )
    }
}

/**
 * The name of a variable containing an instance of [io.spine.validate.ValidatableMessage].
 */
private const val validatable = "validatable"

/**
 * Invokes [io.spine.validate.ValidatableMessage.validate] method upon [validatable]
 * instance and appends all discovered violations to [violations] list.
 */
private val VALIDATE_VALIDATABLE = """
    $validatable.validate()
        .map($ValidationErrorClass::getConstraintViolationList)
        .ifPresent($violations::addAll);
"""
