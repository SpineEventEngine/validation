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

import io.spine.base.FieldPath
import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.option.RangeOption
import io.spine.protodata.Compilation
import io.spine.protodata.ast.FieldRef
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.ref
import io.spine.protodata.ast.unpack
import io.spine.protodata.check
import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.ErrorPlaceholder.RANGE_VALUE
import io.spine.validation.RANGE
import io.spine.validation.api.OPTION_NAME
import io.spine.validation.bound.BoundFieldSupport.checkFieldType
import io.spine.validation.defaultMessage
import io.spine.validation.bound.event.RangeFieldDiscovered
import io.spine.validation.bound.event.rangeFieldDiscovered
import io.spine.validation.checkPlaceholders

/**
 * Controls whether a field should be validated with the `(range)` option.
 *
 * Whenever a field marked with the `(range)` option is discovered, emits
 * [RangeFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. Either `..` or ` .. ` is used as a range delimiter.
 * 3. Either `()` for exclusive or `[]` for inclusive bounds is used.
 * 4. Both lower and upper bounds must be specified.
 * 5. The error message does not contain unsupported placeholders.
 *
 * Conditions for number-based bounds:
 *
 * 1. The provided number has `.` for floating-point fields, and does not have `.`
 *    for integer fields.
 * 2. The provided number fits into the range of the target field type.
 * 3. If both bounds are numbers, the lower bound must be strictly less than the upper one.
 *
 * Conditions for field-based bounds:
 *
 * 1. The specified field path must point to an exciting field.
 * 2. The field must be of numeric type. Repeated fields are not supported.
 *    Strict consistency between target and bound fields is not required,
 *    floating-point fields can be used as bounds for integer fields and vice versa.
 * 3. The field doesn't specify self as its bound.
 *
 * Any violation of the above conditions leads to a compilation error.
 *
 * Examples of valid number ranges:
 *  - `[0..1]`
 *  - `( -17.3 .. +146.0 ]`
 *  - `[+1..+100)`
 *
 * Examples of invalid number ranges:
 *  - `1..5` - missing brackets.
 *  - `[0 - 1]` - wrong divider.
 *  - `[0 . . 1]` - wrong delimiter.
 *  - `( .. 0)` - missing lower bound.
 */
internal class RangePolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = RANGE)
        event: FieldOptionDiscovered
    ): Just<RangeFieldDiscovered> {
        val field = event.subject
        val file = event.file
        val fieldType = checkFieldType(field, file, RANGE)

        val option = event.option.unpack<RangeOption>()
        val metadata = RangeOptionMetadata(option.value, field, fieldType, file, typeSystem)
        val delimiter = metadata.checkDelimiter()

        val (lower, upper) = metadata.range.split(delimiter)
        val (isLowerExclusive, isUpperExclusive) = metadata.checkBrackets(lower, upper)

        val boundParser = NumericBoundParser(metadata)
        val lowerBound = lower.substring(1) // Removes a leading bracket.
        val lowerKBound = boundParser.parse(lowerBound, isLowerExclusive)
        val upperBound = upper.dropLast(1) // Removes a trailing bracket.
        val upperKBound = boundParser.parse(upperBound, isUpperExclusive)

        // Check `lower < upper` only if both bounds are numbers.
        if (lowerKBound.value !is FieldPath && upperKBound.value !is FieldPath) {
            metadata.checkRelation(lowerKBound, upperKBound)
        }

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS,  field, file, RANGE)

        return rangeFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            this.range = metadata.range
            this.lowerBound = lowerKBound.toProto()
            this.upperBound = upperKBound.toProto()
            this.file = file
        }.just()
    }
}

/**
 * A view of a field that is marked with the `(range)` option.
 */
internal class RangeFieldView : View<FieldRef, RangeField, RangeField.Builder>() {

    @Subscribe
    fun on(e: RangeFieldDiscovered) = alter {
        subject = e.subject
        errorMessage = e.errorMessage
        range = e.range
        lowerBound = e.lowerBound
        upperBound = e.upperBound
        file = e.file
    }
}

private fun RangeOptionMetadata.checkDelimiter(): String =
    DELIMITER.find(range)?.value
        ?: Compilation.error(file, field.span) {
            """
            The `($RANGE)` option could not parse the passed range value.
            Value: `$range`.
            Target field: `${field.qualifiedName}`.
            Reason: the lower and upper bounds must be separated either with `..` or ` .. `.
            Examples of correct ranges: `(0..10]`, `[0 .. 10)`.
            """.trimIndent()
        }

private fun RangeOptionMetadata.checkBrackets(
    lower: String,
    upper: String
): Pair<Boolean, Boolean> {
    val lowerExclusive = when (lower.first()) {
        '(' -> true
        '[' -> false
        else -> Compilation.error(file, field.span) {
            """
            The `($RANGE)` option could not parse the passed range value.
            Value: `$range`.
            Target field: `${field.qualifiedName}`.
            Reason: the lower bound must begin either with `(` for exclusive or `[` for inclusive values.
            Examples: `(5`, `[3`.
            """.trimIndent()
        }
    }
    val upperExclusive = when (upper.last()) {
        ')' -> true
        ']' -> false
        else -> Compilation.error(file, field.span) {
            """
            The `($RANGE)` option could not parse the passed range value.
            Value: `$range`.
            Target field: `${field.qualifiedName}`.
            Reason: the upper bound must end either with `)` for exclusive or `]` for inclusive values.
            Examples: `5)`, `3]`.
            """.trimIndent()
        }
    }
    return lowerExclusive to upperExclusive
}

private fun RangeOptionMetadata.checkRelation(lower: KNumericBound, upper: KNumericBound) {
    Compilation.check(lower <= upper, file, field.span) {
        """
        The `($RANGE)` option could not parse the passed range value.
        Value: `$range`.
        Target field: `${field.qualifiedName}`.
        Reason: the lower bound `${lower.value}` must be less than the upper bound `${upper.value}`.
        Examples of the correct ranges: `(-5..5]`, `[0 .. 10)`.
        """.trimIndent()
    }
}

/**
 * Finds `..` or ` .. ` delimiter only when it is used between identifiers or numbers.
 *
 * Additionally, `-+_` symbols are allowed on the right side form the delimiter
 * for the following reasons:
 *
 * 1. A number may begin with the minus or plus symbol.
 * 2. A Protobuf identifier may begin with the underscore.
 */
private val DELIMITER = Regex("""(?<=[\p{Alnum}_])\s?\.\.\s?(?=[\p{Alnum}-+_])""")

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_TYPE,
    FIELD_VALUE,
    PARENT_TYPE,
    RANGE_VALUE,
)
