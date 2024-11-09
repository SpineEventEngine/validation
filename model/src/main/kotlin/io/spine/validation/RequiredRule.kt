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

package io.spine.validation

import io.spine.option.IfMissingOption
import io.spine.option.OptionsProto
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.value.Value
import io.spine.validate.Diags.Required.collectionErrorMsg
import io.spine.validate.Diags.Required.singularErrorMsg
import io.spine.validation.DefaultErrorMessage.from

/**
 * A factory of [SimpleRule]s which represent the `(required)` constraint.
 */
internal object RequiredRule {

    /**
     * Creates a rule for the given field to be required.
     */
    @Suppress("ReturnCount")
    fun forField(field: Field, errorMessage: String): Rule? {
        val unsetValue = UnsetValue.forField(field)
        if (unsetValue == null) {
            return null
        }
        val integratedRule = rule(
            field, unsetValue, errorMessage, singularErrorMsg, false
        )
        if (!(field.isList || field.isMap)) {
            return integratedRule.wrap()
        }
        val type = field.type.extractType()
        UnsetValue.singular(type) ?: return integratedRule.wrap()
        val collectionRule = collectionRule(integratedRule, errorMessage)
        return collectionRule
    }

    private fun collectionRule(integratedRule: SimpleRule, errorMessage: String): Rule {
        val msg = collectionErrorMessage(errorMessage)
        val withCustomErrorMessage = integratedRule.toBuilder()
            .setErrorMessage(msg)
            .build()
        return withCustomErrorMessage.wrap()
    }

    /**
     * This method provides a separate default message for the case of a collection field
     * marked as `(required)`.
     *
     * Singular fields obtain the default error message as a value of the `(default_message)`
     * option set for `IfMissing` option type.
     *
     * Even if a custom error message is not set by a `(if_missing)` field option,
     * we want to have a different *default* message for collection fields,
     * so that the user can find an error quicker.
     *
     * If a custom error message is set, we use it as is.
     *
     * @param errorMessage The error message coming from the [RequiredPolicy] which is producing
     *   the rule while this method is called.
     * @return an error message to be used for the collection field
     */
    @Suppress("ReturnCount")
    private fun collectionErrorMessage(errorMessage: String): String {
        if (errorMessage.isEmpty()) {
            return collectionErrorMsg
        }
        val defaultMessage = from(IfMissingOption.getDescriptor())
        if (errorMessage == defaultMessage) {
            return collectionErrorMsg
        }
        return errorMessage
    }

    private fun rule(
        field: Field,
        value: Value,
        errorMessage: String,
        defaultErrorMessage: String,
        distibute: Boolean
    ): SimpleRule {
        val msg = errorMessage.ifEmpty { defaultErrorMessage }
        return simpleRule {
            this.errorMessage = msg
            this.field = field.name
            operator = ComparisonOperator.NOT_EQUAL
            otherValue = value
            this.distribute = distibute
        }
    }

    /**
     * Checks if the given field is required.
     *
     * @param field The field.
     * @param byDefault The default value
     * @return `true` if the field is marked with `(required) = true` or if
     *  the `byDefault` is `true`, `false` otherwise.
     */
    fun isRequired(field: Field, byDefault: Boolean): Boolean =
        field.optionList
            .firstOrNull { it.`is`(OptionsProto.required) }
            ?.boolValue
            ?: byDefault
}
