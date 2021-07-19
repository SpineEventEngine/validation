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

private const val VALUE = "value"
private const val OTHER = "other"
private const val LEFT = "left"
private const val RIGHT = "right"
private const val OPERATION = "operation"

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
        public fun forRule(format: String, value: String = "", other: String = ""): ErrorMessage =
            ErrorMessage(
                format
                    .replacePlaceholder(VALUE, value)
                    .replacePlaceholder(OTHER, other)
            )

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
        public fun forComposite(format: String,
                         left: ErrorMessage,
                         right: ErrorMessage,
                         operation: LogicalOperator = LO_UNKNOWN): ErrorMessage =
            ErrorMessage(
                format
                    .replacePlaceholder(LEFT, left.value)
                    .replacePlaceholder(RIGHT, right.value)
                    .replacePlaceholder(OPERATION, operation.printableString())
            )
    }

    override fun toString(): String = value
}

private fun String.replacePlaceholder(placeholder: String, newValue: String): String {
    val formattedPlaceholder = "{$placeholder}"
    return replace(formattedPlaceholder, newValue)
}

private fun LogicalOperator.printableString() = when(this) {
    AND, OR, XOR -> name.lowercase()
    else -> "<unknown operation>"
}
