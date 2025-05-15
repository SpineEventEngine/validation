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

package io.spine.validation.bound

import io.spine.protodata.Compilation
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_INT64
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT64
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT64
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.check
import io.spine.protodata.type.resolve
import io.spine.validation.bound.BoundFieldSupport.numericPrimitives

/**
 * Parses bound values specified in Protobuf options that restrict
 * the value range of numeric fields.
 *
 * The parser supports both number literals and field references.
 *
 * @param metadata The information about the numeric option and the field it constrains,
 *   used to interpret and validate the bound value.
 */
internal class NumericBoundParser(
    private val metadata: NumericOptionMetadata
) {

    /**
     * Parses the given raw [bound] value to a [KNumericBound].
     *
     * For number-based bounds, the method checks the following:
     *
     * 1) The bound value is not empty.
     * 2) The provided number has `.` for floating-point fields, and does not have `.`
     *    for integer fields.
     * 3) The provided number fits into the range of the target field type.
     *
     * For field-based bounds:
     *
     * 1) The specified field path points to an existing field.
     * 2) The field bound is not referencing the field it restricts (self-referencing).
     * 3) The referenced field is of singular numeric type.
     *
     * Any violation of the above conditions leads to a compilation error.
     *
     * @return The parsed numeric bound.
     */
    internal fun parse(bound: String, exclusive: Boolean): KNumericBound {
        with(metadata) {
            Compilation.check(bound.isNotEmpty(), file, field.span) {
                """
                The `($optionName)` option could not parse the passed bound value.
                Target field: `${field.qualifiedName}`.
                Reason: the value is empty.
                Please provide either a numeric literal or refer to a value of another field via its name or a path (for nested fields).
                """.trimIndent()
            }
        }
        return if (bound.isFieldReference()) {
            metadata.parseFieldReference(bound, exclusive)
        } else {
            metadata.parseNumber(bound, exclusive)
        }
    }

    private fun NumericOptionMetadata.parseNumber(
        number: String,
        exclusive: Boolean
    ): KNumericBound {
        if (fieldType in listOf(TYPE_FLOAT, TYPE_DOUBLE)) {
            Compilation.check(FLOAT.matches(number), file, field.span) {
                """
                The `($optionName)` option could not parse the passed bound value.
                Value: `$number`.
                Target field: `${field.qualifiedName}`.
                Field type: `${field.type.name}`.
                Reason: a floating-point number is required for this field type.
                Examples: `12.3`, `-0.1`, `6.02E2`.
                """.trimIndent()
            }
        } else {
            Compilation.check(INTEGER.matches(number), file, field.span) {
                """
                The `($optionName)` option could not parse the passed bound value.
                Value: `$number`.
                Target field: `${field.qualifiedName}`.
                Field type: `${field.type.name}`.
                Reason: an integer number is required for this field type.
                Examples: `123`, `-567823`.
                """.trimIndent()
            }
        }

        val parsed = when (fieldType) {
            TYPE_FLOAT -> number.toFiniteFloatOrNull()
            TYPE_DOUBLE -> number.toFiniteDoubleOrNull()
            TYPE_INT32, TYPE_SINT32, TYPE_SFIXED32 -> number.toIntOrNull()
            TYPE_INT64, TYPE_SINT64, TYPE_SFIXED64 -> number.toLongOrNull()
            TYPE_UINT32, TYPE_FIXED32 -> number.toUIntOrNull()
            TYPE_UINT64, TYPE_FIXED64 -> number.toULongOrNull()
            else -> unexpectedPrimitiveType(fieldType)
        }

        Compilation.check(parsed != null, file, field.span) {
            """
            The `($optionName)` option could not parse the passed bound value.
            Value: `$number`.
            Target field: `${field.qualifiedName}`.
            Field type: `${field.type.name}`.
            Reason: the value is out of range for this field type.
            """.trimIndent()
        }

        return KNumericBound(parsed!!, exclusive)
    }

    private fun NumericOptionMetadata.parseFieldReference(
        fieldPath: String,
        exclusive: Boolean
    ): KNumericBound {
        Compilation.check(fieldPath != field.name.value, file, field.span) {
            """
            The `($optionName)` option cannot use the specified field as a bound.
            Field path: `$fieldPath`.
            Target field: `${field.qualifiedName}`.
            Reason: self-referencing is not allowed.
            Please refer to a value of another field via its name or a path (for nested fields).
            """.trimIndent()
        }

        val boundFieldPath = io.spine.base.fieldPath {
            fieldName.addAll(fieldPath.split("."))
        }

        val boundField = try {
            val (messageType, _) = typeSystem.findMessage(field.declaringType)!!
            typeSystem.resolve(boundFieldPath, messageType)
        } catch (e: IllegalStateException) {
            Compilation.error(file, field.span) {
                """
                The `($optionName)` option could not resolve the specified field path.
                Field path: `$fieldPath`.
                Target field: `${field.qualifiedName}`.
                Reason: the specified field does not exist, or one of the components of the field path represents a non-message field.
                Please make sure the field path refers to a valid field.
                """.trimIndent()
            }
        }

        val boundFieldType = boundField.type.primitive
        Compilation.check(boundFieldType in numericPrimitives, file, field.span) {
            """
            The `($optionName)` option cannot use the specified field as a bound.
            Field path: `$fieldPath`.
            Field type: `${boundFieldType.name}`.
            Target field: `${field.qualifiedName}`.
            Reason: the specified field is not of a numeric type.
            Please use a field with a singular numeric type.
            """.trimIndent()
        }

        return KNumericBound(boundFieldPath, exclusive)
    }

    private companion object {
        private val INTEGER = Regex("[-+]?\\d+")
        private val FLOAT = Regex("[-+]?\\d+\\.\\d+([eE][-+]?\\d+)?")
    }
}

/**
 * Parses this [String] to a [Float], returning null if the string is not
 * a valid finite float.
 */
private fun String.toFiniteFloatOrNull(): Float? =
    toFloatOrNull()?.takeIf { it.isFinite() }

/**
 * Parses this [String] to a [Double], returning null if the string is not
 * a valid finite double.
 */
private fun String.toFiniteDoubleOrNull(): Double? =
    toDoubleOrNull()?.takeIf { it.isFinite() }


/**
 * Tells whether this [String] bound value contains a field reference.
 */
private fun String.isFieldReference() = first().run {
    isLetter() || this == '_'
}

private fun unexpectedPrimitiveType(primitiveType: PrimitiveType): Nothing =
    error(
        "`${KNumericBound::class.simpleName}` cannot be created for `$primitiveType` field type." +
                " Please make sure the policy correctly filtered unsupported field types."
    )
