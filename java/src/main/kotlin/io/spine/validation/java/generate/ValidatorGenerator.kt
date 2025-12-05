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
import io.spine.base.FieldPath
import io.spine.string.simply
import io.spine.string.ti
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.extractMessageType
import io.spine.tools.compiler.ast.isList
import io.spine.tools.compiler.ast.isMap
import io.spine.tools.compiler.ast.isSingular
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.refersToMessage
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.CodeBlock
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.tools.compiler.jvm.call
import io.spine.tools.compiler.jvm.field
import io.spine.tools.compiler.jvm.javaClassName
import io.spine.tools.compiler.type.TypeSystem
import io.spine.validate.ConstraintViolation
import io.spine.validate.DetectedViolation
import io.spine.validate.TemplateString
import io.spine.validation.api.expression.constraintViolation
import io.spine.validation.api.expression.mergeFrom
import io.spine.validation.api.expression.orElse
import io.spine.validation.api.expression.resolve
import io.spine.validation.api.expression.stringify
import io.spine.validation.api.generate.MessageScope.message
import io.spine.validation.api.generate.ValidateScope.parentName
import io.spine.validation.api.generate.ValidateScope.parentPath
import io.spine.validation.api.generate.ValidateScope.violations
import io.spine.validation.api.generate.mangled

/**
 * A fully qualified Java class name of a validator class.
 */
internal typealias ValidatorClass = ClassName

/**
 * A fully qualified Java class name of a Protobuf message class.
 */
internal typealias MessageClass = ClassName

/**
 * Generates code to apply validators to the message fields,
 * for which there is a validator assigned.
 *
 * Please note that this generator is not
 * [OptionGenerator][io.spine.validation.api.generate.OptionGenerator] intentionally.
 * This is a dedicated implementation, handling codegen for a specific use case not
 * related to the validation options.
 */
internal class ValidatorGenerator(
    private val validators: Map<MessageClass, ValidatorClass>,
    private val typeSystem: TypeSystem
) {

    /**
     * Applies validators for the fields in the given message [type], for which
     * one is declared.
     *
     * The method inspects each field of the given [type], finding those that reference
     * message types with an available validator, and produces [CodeBlock]s that
     * invoke these validators.
     */
    fun codeFor(type: MessageType): List<CodeBlock> {
        val messageFields = type.fieldList.filter { it.type.refersToMessage() }
            .associateWith { it.type.extractMessageType(typeSystem)!! }
        val validatorsToApply = messageFields.mapNotNull { (field, type) ->
            val javaClass = type.javaClassName(typeSystem)
            validators[javaClass]?.let { field to it }
        }
        return validatorsToApply.map { (field, validator) ->
            ApplyValidator(field, validator).code()
        }
    }
}

/**
 * Applies the specified [validator] to the given [field].
 */
private class ApplyValidator(
    private val field: Field,
    private val validator: ValidatorClass
) {
    private val discovered = mangled("discovered")
    private val getter = message.field(field).getter<Any>()
    private val fieldType = field.type

    @Suppress("UNCHECKED_CAST") // The cast is guaranteed due to the field type checks.
    fun code(): CodeBlock = when {
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
    }

    /**
     * Yields an expression that invokes validator for the given [message] instance.
     *
     * The expression does the following:
     *
     * 1. Creates a new instances of [validator].
     * 2. Invokes [MessageValidator.validate][io.spine.validation.api.MessageValidator.validate]
     *   passing an instance of the [message].
     * 3. Converts each [DetectedViolation] to [ConstraintViolation].
     * 4. Puts all constraint violations to the list of discovered [violations].
     */
    private fun validate(message: Expression<Message>): CodeBlock {
        val vv = ReadVar<DetectedViolation>("vv")
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

    /**
     * Converts this [DetectedViolation] expression to the one
     * returning [ConstraintViolation].
     */
    private fun Expression<DetectedViolation>.toConstraintViolation():
            Expression<ConstraintViolation> {
        val message = call<TemplateString>("getMessage")
        val fieldValue = call<Any?>("getFieldValue")
        val fieldPath = resolveFieldPath()
        val typeName = parentName.orElse(field.declaringType).stringify()
        return constraintViolation(message, typeName, fieldPath, fieldValue)
    }

    /**
     * Resolves a [FieldPath] from this [DetectedViolation] against the parent path.
     *
     * The resolved path contains the [parentPath] plus the name of the [field], for which
     * [ApplyValidator] is invoked.
     *
     * If the validator returns a non-nullable path, it is also appended to the resulting path.
     * Otherwise, just the one with the field name is returned.
     */
    @Suppress("UNCHECKED_CAST") // After the null-check, the cast is safe.
    private fun Expression<DetectedViolation>.resolveFieldPath(): Expression<FieldPath> {
        val validatorPath = call<FieldPath?>("getFieldPath")
        val resolved = parentPath.resolve(field.name)
        val merged = resolved.mergeFrom(validatorPath as Expression<FieldPath>)
        return Expression("($validatorPath == null ? $resolved : $merged)")
    }
}
