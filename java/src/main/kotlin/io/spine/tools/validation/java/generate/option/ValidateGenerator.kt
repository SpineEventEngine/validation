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

import com.google.protobuf.Message
import io.spine.server.query.select
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.isAny
import io.spine.tools.compiler.ast.isList
import io.spine.tools.compiler.ast.isMap
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.jvm.CodeBlock
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.JavaValueConverter
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.tools.compiler.jvm.field
import io.spine.tools.compiler.jvm.getDefaultInstance
import io.spine.tools.validation.ValidateField
import io.spine.tools.validation.java.expression.AnyClass
import io.spine.tools.validation.java.expression.AnyPackerClass
import io.spine.tools.validation.java.expression.EmptyFieldCheck
import io.spine.tools.validation.java.expression.KnownTypesClass
import io.spine.tools.validation.java.expression.MessageClass
import io.spine.tools.validation.java.expression.TypeUrlClass
import io.spine.tools.validation.java.expression.ValidatableMessageClass
import io.spine.tools.validation.java.expression.ValidationErrorClass
import io.spine.tools.validation.java.expression.orElse
import io.spine.tools.validation.java.expression.resolve
import io.spine.tools.validation.java.generate.MessageScope.message
import io.spine.tools.validation.java.generate.OptionGenerator
import io.spine.tools.validation.java.generate.SingleOptionCode
import io.spine.tools.validation.java.generate.ValidateScope.parentName
import io.spine.tools.validation.java.generate.ValidateScope.parentPath
import io.spine.tools.validation.java.generate.ValidateScope.violations
import org.intellij.lang.annotations.Language

/**
 * The generator for `(validate)` option.
 */
internal class ValidateGenerator : OptionGenerator() {

    /**
     * All `(validate)` fields in the current compilation process.
     */
    private val allValidateFields by lazy {
        querying.select<ValidateField>()
            .all()
    }

    override fun codeFor(type: TypeName): List<SingleOptionCode> =
        allValidateFields
            .filter { it.id.type == type }
            .map { GenerateValidate(it, converter).code() }
}

/**
 * Generates code for a single application of the `(validate)` option
 * represented by the [view].
 */
private class GenerateValidate(
    private val view: ValidateField,
    override val converter: JavaValueConverter
) : EmptyFieldCheck {

    private val field = view.subject
    private val fieldType = field.type
    private val declaringType = field.declaringType
    private val getter = message.field(field).getter<Any>()

    /**
     * Returns the generated code.
     */
    @Suppress("UNCHECKED_CAST") // The cast is guaranteed due to the field type checks.
    fun code(): SingleOptionCode = when {
        fieldType.isMessage -> validate(getter as Expression<Message>, fieldType.message.isAny)

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
                    " used by `ValidateReaction` when validating" +
                    " the `ValidateFieldDiscovered` event."
        )
    }.run { SingleOptionCode(this) }

    /**
     * Yields an expression to validate the provided [message] if it implements
     * [io.spine.validation.ValidatableMessage] interface.
     *
     * The reported violations are appended to [violations] list, if any.
     *
     * If the passed [message] represents [com.google.protobuf.Any], the method will firstly
     * unpack the enclosed message, and only then validate it.
     *
     * Note that not all instances of [com.google.protobuf.Any] can be unpacked:
     *
     * 1) Unpacking of the default instance is impossible.
     * 2) Unpacking of instances with unknown type URLs is also impossible.
     *
     * Such instances are always considered valid.
     *
     * The default instances of [com.google.protobuf.Message] are considered valid
     * for singular fields even if the checked message has one or more required fields.
     * In this case, it is impossible to determine whether the field itself is set
     * with an invalid message instance or just empty.
     *
     * The above statement doesn't apply to repeated and map fields. Within collections,
     * default instances are considered invalid.
     *
     * @param message An instance of a Protobuf message to validate.
     * @param isAny Must be `true` if the provided [message] is [com.google.protobuf.Any].
     *  In this case, the method will do the unpacking in the first place.
     */
    @Suppress(
        "MaxLineLength", "NewClassNamingConvention", "PackageVisibleField", "EmptyClass",
        "LongMethod"
    ) // To support highlighting and layout of the generated code blocks.
    private fun validate(message: Expression<Message>, isAny: Boolean): CodeBlock {
        @Language("java")
        val isNotDefault =
            if ((field.isMap || field.isList) && !isAny)
                // Avoid having unnecessary comparison with an empty list or map.
                // The validation goes over an element inside a `for()` loop.
                //
                // The `null` value avoids the need for the `if()` clause with the comparison.
                //
                // Do the comparison for `Any` `element` with `Any.getDefaultInstance()`
                // so that we can unpack a real message of interest.
                //
                null
            else
                if (isAny)
                    "$message != ${AnyClass.getDefaultInstance()}"
                else
                    "!${field.hasDefaultValue()}"

        @Language("java")
        val isValidatable =
            if (isAny)
                " $KnownTypesClass.instance().contains($TypeUrlClass.ofEnclosed($message)) &&" +
                        " unpacked instanceof $ValidatableMessageClass validatable"
            else
                " (($MessageClass) $message) instanceof $ValidatableMessageClass validatable"

        @Language("java")
        val validationBlock =
            if (isAny)
                """
                var fieldPath = ${parentPath.resolve(field.name)};
                var typeName =  ${parentName.orElse(declaringType)};
                var unpacked = $AnyPackerClass.unpack($message);
                if ($isValidatable) {                
                    validatable.validate(fieldPath, typeName)
                        .map($ValidationErrorClass::getConstraintViolationList)
                        .ifPresent($violations::addAll);
                }
                var byRegistry = io.spine.validation.ValidatorRegistry.validate(unpacked, fieldPath, typeName);
                if (!byRegistry.isEmpty()) {  
                    $violations.addAll(byRegistry);
                }    
                """.trimIndent()
            else
                """
                var fieldPath = ${parentPath.resolve(field.name)};
                var typeName =  ${parentName.orElse(declaringType)};
                if ($isValidatable) {                
                    validatable.validate(fieldPath, typeName)
                        .map($ValidationErrorClass::getConstraintViolationList)
                        .ifPresent($violations::addAll);
                }
                var byRegistry = io.spine.validation.ValidatorRegistry.validate($message, fieldPath, typeName);
                if (!byRegistry.isEmpty()) {  
                    $violations.addAll(byRegistry);
                }    
                """.trimIndent()
        return if (isNotDefault == null) {
            CodeBlock(validationBlock)
        } else {
            CodeBlock(
                """
                if ($isNotDefault) {
                    $validationBlock
                }
                """.trimIndent()
            )
        }
    }
}
