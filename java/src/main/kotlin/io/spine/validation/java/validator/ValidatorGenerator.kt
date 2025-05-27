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

package io.spine.validation.java.validator

import com.google.protobuf.Message
import io.spine.base.FieldPath
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.extractMessageType
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.isSingular
import io.spine.protodata.ast.name
import io.spine.protodata.ast.refersToMessage
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.call
import io.spine.protodata.java.field
import io.spine.protodata.java.javaClassName
import io.spine.protodata.type.TypeSystem
import io.spine.string.simply
import io.spine.string.ti
import io.spine.validate.ConstraintViolation
import io.spine.validate.TemplateString
import io.spine.validation.api.ValidatorViolation
import io.spine.validation.api.expression.mergeFrom
import io.spine.validation.api.expression.orElse
import io.spine.validation.api.expression.resolve
import io.spine.validation.api.expression.stringify
import io.spine.validation.api.generate.MessageScope.message
import io.spine.validation.api.generate.SingleOptionCode
import io.spine.validation.api.generate.ValidateScope.parentName
import io.spine.validation.api.generate.ValidateScope.parentPath
import io.spine.validation.api.generate.ValidateScope.violations
import io.spine.validation.api.generate.mangled

internal typealias ValidatorClass = ClassName
internal typealias MessageClass = ClassName

internal class ValidatorGenerator(
    private val validators: Map<MessageClass, ValidatorClass>,
    private val typeSystem: TypeSystem
) {

    /*
    // TODO:2025-05-23:yevhenii.nadtochii: Check out.

         - Should we handle `repeated` and `map` fields?
           Yes -> We support.

         - Do we allow custom validators for local messages?
           No -> Error.

         - Can one message have several validators?
           No -> Error.

         + Validator must be a top-level class or nested.

         + Validator must have a public, no args constructor.

         + Validator authors are fully responsible for the instance of `ConstraintViolation`.

         + Specs of this feature must go to the interface `MessageValidator`.

     */

    fun codeFor(type: MessageType): List<SingleOptionCode> {
        val messageFields = type.fieldList.filter { it.type.refersToMessage() }
            .associateWith { it.type.extractMessageType(typeSystem)!! }
        val validatorsToApply = messageFields.mapNotNull { (field, message) ->
            val javaClass = message.javaClassName(typeSystem)
            if (validators.containsKey(javaClass)) {
                field to validators[javaClass]!!
            } else {
                null
            }
        }
        return validatorsToApply.map { (field, validator) ->
            ApplyValidator(field, validator).code()
        }
    }
}

private class ApplyValidator(
    private val field: Field,
    private val validator: ValidatorClass
) {

    private val discovered = mangled("discovered")
    private val getter = message.field(field).getter<Any>()
    private val fieldType = field.type

    @Suppress("UNCHECKED_CAST") // The cast is guaranteed due to the field type checks.
    fun code(): SingleOptionCode = when {
        fieldType.isSingular -> validate(getter as Expression<Message>)

        fieldType.isList ->
            CodeBlock(
                """
                for (var element : $getter) {
                    ${validate(ReadVar("element"))}
                }
                """.trimIndent()
            )

        fieldType.isMap ->
            CodeBlock(
                """
                for (var element : $getter.values()) {
                    ${validate(ReadVar("element"))}
                }     
                """.trimIndent()
            )

        else -> error(
            """
            The field type `${fieldType.name}` is not supported by `${simply<ValidatorGenerator>()}`.
            This generator supports singular message fields, repeated and maps of messages.
            """.ti()
        )
    }.run { SingleOptionCode(this) }

    private fun validate(message: Expression<Message>): CodeBlock {
        val vv = ReadVar<ValidatorViolation>("vv")
        val constraint = CodeBlock("""
                var $discovered = new $validator()
                    .validate($message)
                    .stream()
                    .map($vv -> ${vv.toConstraintViolation()})
                    .toList();
                $violations.addAll($discovered);
            """.trimIndent())
        return constraint
    }

    private fun Expression<ValidatorViolation>.toConstraintViolation():
            Expression<ConstraintViolation> {
        val message = call<TemplateString>("getMessage")
        val fieldValue = call<Any?>("getFieldValue")
        val fieldPath = resolveFieldPath()
        val typeName = parentName.orElse(field.declaringType).stringify()
        return io.spine.validation.api.expression.constraintViolation(
            message, typeName, fieldPath, fieldValue
        )
    }

    @Suppress("UNCHECKED_CAST") // After the null-check, the cast is safe.
    private fun Expression<ValidatorViolation>.resolveFieldPath(): Expression<FieldPath> {
        val local = call<FieldPath?>("getFieldPath")
        val resolved = parentPath.resolve(field.name)
        val merged = resolved.mergeFrom(local as Expression<FieldPath>)
        return Expression("($local == null ? $resolved : $merged)")
    }
}
