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
 * Whenever a field marked with `(range)` option is discovered, emits
 * [RangeFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. Either `..` or ` .. ` is used as a range delimiter.
 * 3. Either `()` for exclusive or `[]` for inclusive bounds is used.
 * 4. The provided number has `.` for floating-point fields, and does not have `.`
 *    for integer fields.
 * 5. The provided bounds fit into the range of the target field type.
 * 6. The lower bound is strictly less than the upper one.
 * 7. The error message does not contain unsupported placeholders.
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
        val (messageType, _) = typeSystem.findMessage(field.declaringType)!!
        val context = RangeContext(option.value, typeSystem, messageType, fieldType, field, file)
        val delimiter = context.checkDelimiter()

        val (left, right) = context.range.split(delimiter)
        val (lowerExclusive, upperExclusive) = context.checkBoundTypes(left, right)
        val lower = context.checkNumericBound(left.substring(1), lowerExclusive)
        val upper = context.checkNumericBound(right.dropLast(1), upperExclusive)
        context.checkRelation(lower, upper)

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS,  field, file, RANGE)

        return rangeFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            this.range = context.range
            lowerBound = lower.toProto()
            upperBound = upper.toProto()
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

private fun RangeContext.checkDelimiter(): String =
    DELIMITER.find(range)?.value
        ?: Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The lower and upper bounds should be" +
                    " separated either with `..` or ` .. ` delimiter. Examples of the correct " +
                    " ranges: `(0..10]`, `[0 .. 10)`."
        }

private fun RangeContext.checkBoundTypes(lower: String, upper: String): Pair<Boolean, Boolean> {
    val lowerExclusive = when (lower.first()) {
        '(' -> true
        '[' -> false
        else -> Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The lower bound should begin either" +
                    " with `(` for exclusive or `[` for inclusive values. Examples of" +
                    " the correct ranges: `(0..10]`, `(0..10)`, `[5..100]`."
        }
    }
    val upperExclusive = when (upper.last()) {
        ')' -> true
        ']' -> false
        else -> Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The upper bound should end either" +
                    " with `)` for exclusive or `]` for inclusive values. Examples of" +
                    " the correct ranges: `(0..10]`, `(0..10)`, `[5..100]`."
        }
    }
    return lowerExclusive to upperExclusive
}

private fun RangeContext.checkRelation(lower: KotlinNumericBound, upper: KotlinNumericBound) {
    Compilation.check(lower <= upper, file, field.span) {
        "The `($RANGE)` option could not parse the range value `$range` specified for" +
                " `${field.qualifiedName}` field. The lower bound `${lower.value}` should be" +
                " less than the upper `${upper.value}`."
    }
}

private val DELIMITER = Regex("""(?<=[\p{Alnum}_])\s?\.\.\s?(?=[\p{Alnum}-+_])""")

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_TYPE,
    FIELD_VALUE,
    PARENT_TYPE,
    RANGE_VALUE,
)
