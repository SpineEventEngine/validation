/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.validation.java.setonce

import io.spine.base.FieldPath
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.StringLiteral
import io.spine.protodata.java.listExpression
import io.spine.protodata.java.newBuilder
import io.spine.protodata.java.packToAny
import io.spine.validate.ConstraintViolation
import io.spine.validation.IF_SET_AGAIN
import io.spine.validation.java.setonce.MessageToken.CURRENT_VALUE
import io.spine.validation.java.setonce.MessageToken.Companion.TokenRegex
import io.spine.validation.java.setonce.MessageToken.Companion.forPlaceholder
import io.spine.validation.java.setonce.MessageToken.FIELD_NAME
import io.spine.validation.java.setonce.MessageToken.FIELD_TYPE
import io.spine.validation.java.setonce.MessageToken.PARENT_TYPE
import io.spine.validation.java.setonce.MessageToken.PROPOSED_VALUE
import io.spine.validation.java.setonce.MessageToken.values

/**
 * Builds a [ConstraintViolation] instance for the given field.
 *
 * @param errorMessage The error message pattern.
 * @param field The field, which was attempted to set twice.
 */
internal class SetOnceConstraintViolation(
    private val errorMessage: String,
    field: Field
) {

    private val fieldName = field.name.value
    private val fieldType = field.type.name
    private val qualifiedName = field.qualifiedName
    private val declaringMessage = field.declaringType.qualifiedName

    /**
     * Builds an expression that returns a new instance of [ConstraintViolation]
     * with the given values.
     *
     * @param currentValue The current field value as string.
     * @param newValue The proposed new value as string.
     * @param payload The violated value to be packed as Protobuf `Any` and set for
     *  [ConstraintViolation.getFieldValue] property. Usually, it is just [newValue].
     *  But for some field types, some conversion may take place.
     */
    fun withValues(
        currentValue: Expression<String>,
        newValue: Expression<String>,
        payload: Expression<*> = newValue
    ): Expression<ConstraintViolation> {
        val tokenValues = tokenValues(currentValue, newValue)
        val (format, params) = toPrintfString(errorMessage, tokenValues)
        val fieldPath = ClassName(FieldPath::class).newBuilder()
            .chainAdd("field_name", StringLiteral(fieldName))
            .chainBuild<FieldPath>()
        val violation = ClassName(ConstraintViolation::class).newBuilder()
            .chainSet("msg_format", StringLiteral(format))
            .chainAddAll("param", listExpression(params))
            .chainSet("type_name", StringLiteral(declaringMessage))
            .chainSet("field_path", fieldPath)
            .chainSet("field_value", payload.packToAny())
            .chainBuild<ConstraintViolation>()
        return violation
    }

    /**
     * Determines the value for each of the supported tokens.
     */
    private fun tokenValues(currentValue: Expression<String>, newValue: Expression<String>) =
        MessageToken.values().associateWith {
            when (it) {
                FIELD_NAME -> StringLiteral(fieldName)
                FIELD_TYPE -> StringLiteral(fieldType)
                CURRENT_VALUE -> currentValue
                PROPOSED_VALUE -> newValue
                PARENT_TYPE -> StringLiteral(declaringMessage)
            }
        }

    /**
     * Prepares `printf`-style format string and its parameters.
     *
     * This method does the following:
     *
     * 1. Finds all [MessageToken]s in the given [errorMessage] and replaces them with `%s`.
     * 2. For each found token, it finds its corresponding [value][tokenValues], and puts it
     *    to the list of parameters.
     */
    private fun toPrintfString(
        errorMessage: String,
        tokenValues: Map<MessageToken, Expression<String>>
    ): Pair<String, List<Expression<String>>> {
        val params = mutableListOf<Expression<String>>()
        val format = TokenRegex.replace(errorMessage) { matchResult ->
            val tokenName = matchResult.groupValues[1]
            val token = forPlaceholder(tokenName) ?: throwUnsupportedToken(tokenName)
            val param = tokenValues[token]!!
            "%s".also { params.add(param) }
        }
        return format to params
    }

    private fun throwUnsupportedToken(name: String): Nothing = throw IllegalArgumentException(
        "The `($IF_SET_AGAIN)` option doesn't support the token: `{$name}`. " +
                "The supported tokens: `${supportedTokens()}`. " +
                "The declared field: `${qualifiedName}`."
    )

    private fun supportedTokens(): String = values().joinToString { "{${it.placeholder}}" }
}

/**
 * Defines error message tokens that can be used in the error message pattern.
 *
 * These tokens are replaced with the actual values when the error instance
 * is constructed.
 *
 * The list of the supported tokens can be found in the option specification.
 * Take a look at `IfSetAgainOption` in `options.proto`.
 */
private enum class MessageToken(val placeholder: String) {

    FIELD_NAME("field.name"),
    FIELD_TYPE("field.type"),
    CURRENT_VALUE("field.value"),
    PROPOSED_VALUE("field.proposed_value"),
    PARENT_TYPE("parent.type");

    companion object {

        /**
         * A Regex pattern to find all present tokens in the message.
         */
        val TokenRegex = Regex("""\{(.*?)}""")

        /**
         * Returns a [MessageToken] for the given [placeholder], if any.
         */
        fun forPlaceholder(placeholder: String): MessageToken? =
            values().firstOrNull { it.placeholder == placeholder }
    }
}
