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

import com.google.protobuf.Message
import io.spine.protodata.ast.isAny
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.name
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.ReadVar
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
    private val getter = message.field(field).getter<Message>()

    override fun generate(): FieldOptionCode = when {
        fieldType.isMessage -> validate(getter, fieldType.message.isAny)

        fieldType.isList ->
            CodeBlock(
                """
                for (var element : $getter) {
                    ${validate(ReadVar("element"), fieldType.list.isAny)}
                }
                """.trimIndent()
            )

        fieldType.isMap ->
            CodeBlock(
                """
                for (var element : $getter.values()) {
                    ${validate(ReadVar("element"), fieldType.map.valueType.isAny)}
                }     
                """.trimIndent()
            )

        else -> error(
            "The field type `${fieldType.name}` is not supported by `ValidateFieldGenerator`." +
                    " Please ensure that the supported field types in this generator match those" +
                    " used by `ValidatePolicy` when validating the `ValidateFieldDiscovered` event."
        )
    }.run { FieldOptionCode(this) }

    /**
     * Yields an expression to validate the provided [message] if it implements
     * [io.spine.validate.ValidatableMessage] interface.
     *
     * The reported violations are appended to [violations] list, if any.
     *
     * If the passed [message] represents [com.google.protobuf.Any], the method will firstly
     * unpack the enclosed message, and only then validate it.
     *
     * Implementation notes are the following:
     *
     * 1) Unpacking of the default instance of [com.google.protobuf.Any] is impossible.
     *    Such an instance should always be considered valid.
     * 2) The default instances of [com.google.protobuf.Message] are not considered valid
     *    by default. They may have required fields.
     * 3) Cast of [message] to the parental [com.google.protobuf.Message] interface is required
     *    because the Java compiler will fail the compilation if the result of `instanceof`
     *    invocation is known at the compile time. Unfortunately, we cannot check it during
     *    the codegen. For example, if the field type is proto's `Timestamp`, it does not make
     *    sense to write `getTimestamp() instanceof ValidatableMessage`. The compilation will fail
     *    because this expression is always `false`.
     * 4) The returned expression uses an improved version of the `instanceof` that both tests
     *    the parameter and assigns it to a variable of the proper type. This eliminates the need
     *    of an additional cast to the tested type. This feature requires Java 14 and more.
     *
     * @param message An instance of a Protobuf message to validate.
     * @param isAny Must return `true` if the provided [message] is [com.google.protobuf.Any].
     *  In this case, the method will firstly do the unpacking.
     */
    private fun validate(message: Expression<Message>, isAny: Boolean): CodeBlock {
        val isValidatable =
            if (isAny)
                "$message != ${AnyClass.getDefaultInstance()} && $AnyPackerClass.unpack($message) instanceof $ValidatableMessageClass validatable"
            else
                "(($MessageClass) $message) instanceof $ValidatableMessageClass validatable"
        return CodeBlock(
            """
            if ($isValidatable) {
                validatable.validate()
                    .map($ValidationErrorClass::getConstraintViolationList)
                    .ifPresent($violations::addAll);
            }
            """.trimIndent()
        )
    }
}
