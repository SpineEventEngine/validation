/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.validation

import io.spine.validation.LogicalOperator.AND
import io.spine.validation.LogicalOperator.LO_UNKNOWN
import io.spine.validation.LogicalOperator.OR
import io.spine.validation.LogicalOperator.XOR
import io.spine.validation.Placeholder.LEFT
import io.spine.validation.Placeholder.OPERATION
import io.spine.validation.Placeholder.OTHER
import io.spine.validation.Placeholder.RIGHT
import io.spine.validation.Placeholder.VALUE
import io.spine.validation.StringExpression.Companion.inQuotes

/**
 * A human-readable error message, describing a validation constraint violation.
 */
public class ErrorMessage
private constructor(private val value: String) {

    public companion object {

        /**
         * Produces an error message for a simple validation rule.
         *
         * @param format the message format
         * @param value the value of the field
         * @param other the value to which the field is compared
         */
        @JvmStatic
        @JvmOverloads
        public fun forRule(
            format: String,
            value: String = "",
            other: String = "",
            interpol: Interpolation
        ): ErrorMessage {
            var msg = interpol.inTemplate(inQuotes(format)) replace inQuotes(VALUE.fmt) with value
            msg = interpol.inTemplate(msg) replace inQuotes(OTHER.fmt) with other
            return ErrorMessage(msg.value)
        }

        /**
         * Produces an error message for a composite validation rule.
         *
         * @param format the message format
         * @param left the error message of the left rule
         * @param right the error message of the right rule
         * @param operation the operator which joins the rule conditions
         */
        @JvmStatic
        @JvmOverloads
        public fun forComposite(
            format: String,
            left: ErrorMessage,
            right: ErrorMessage,
            operation: LogicalOperator = LO_UNKNOWN,
            interpol: Interpolation
        ): ErrorMessage {
            var msg =
                interpol.inTemplate(inQuotes(format)) replace inQuotes(LEFT.fmt) with left.value
            msg = interpol.inTemplate(msg) replace inQuotes(RIGHT.fmt) with right.value
            msg = interpol.inTemplate(msg)
                .replace(inQuotes(OPERATION.fmt))
                .with(operation.printableString())
            return ErrorMessage(msg.value)
        }
    }

    override fun toString(): String = value
}

private fun LogicalOperator.printableString() = when (this) {
    AND, OR, XOR -> name.lowercase()
    else -> "<unknown operation>"
}

private enum class Placeholder {

    VALUE,
    OTHER,
    LEFT,
    RIGHT,
    OPERATION;

    val fmt: String
        get() = "{${name.lowercase()}}"
}
