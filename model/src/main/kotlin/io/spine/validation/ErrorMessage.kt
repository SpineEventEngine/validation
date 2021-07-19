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

/**
 * A human-readable error message, describing a validation constraint violation.
 *
 * The error message can contain dynamic references to values in the generated code.
 * If such references are present, they are inserted into the message via the string plus (`+`)
 * operators.
 *
 * If the target language uses a different way of concatenating strings (e.g. dots in PHP, etc.),
 * or if it defines string literals in another way rather than enclosing them in quotation marks,
 * renderers for such a language mustn't use this class and instead compile the error message on
 * their own.
 */
public class ErrorMessage
private constructor(private val expression: String) {

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
            other: String = ""
        ): ErrorMessage {
            val msg = Template(format).apply {
                formatDynamic(VALUE, value)
                formatStatic(OTHER, other)
            }
            return ErrorMessage(msg.joinExpression())
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
        ): ErrorMessage {
            val msg = Template(format).apply {
                formatStatic(OPERATION, operation.printableString())
                formatDynamic(LEFT, left.expression)
                formatDynamic(RIGHT, right.expression)
            }
            return ErrorMessage(msg.joinExpression())
        }
    }

    override fun toString(): String = expression
}

private fun LogicalOperator.printableString() = when (this) {
    AND, OR, XOR -> name.lowercase()
    else -> "<unknown operation>"
}

/**
 * A placeholder which can be present in a validation error message.
 */
internal enum class Placeholder {

    /**
     * The actual value of the validated field.
     */
    VALUE,

    /**
     * The value to which the validated field is compared.
     */
    OTHER,

    /**
     * In a composite validation rule, the left condition.
     */
    LEFT,

    /**
     * In a composite validation rule, the right condition.
     */
    RIGHT,

    /**
     * In a composite validation rule, the boolean operator which joins the two conditions.
     *
     * @see LogicalOperator
     */
    OPERATION;

    /**
     * The placeholder as it appears in the error message template.
     */
    val fmt: String
        get() = "{${name.lowercase()}}"
}
