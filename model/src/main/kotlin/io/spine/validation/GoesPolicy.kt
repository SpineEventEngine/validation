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

import com.google.protobuf.kotlin.unpack
import io.spine.base.FieldPath
import io.spine.core.External
import io.spine.core.Where
import io.spine.option.GoesOption
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.plugin.Policy
import io.spine.protodata.type.resolve
import io.spine.server.event.Just
import io.spine.server.event.Just.Companion.just
import io.spine.server.event.React
import io.spine.validation.event.RuleAdded
import io.spine.validation.required.RequiredRule

/**
 * A policy to add a validation rule to a type whenever the `(goes)` field option
 * is discovered.
 *
 * This option, when being applied to a target field `A`, declares a dependency to
 * a field `B`. So, whenever `A` is set, `B` also must be set.
 *
 * Upon discovering a field with the mentioned option, the police emits the following
 * composite rule for `A`: `(A isNot Set) OR (B is Set)`.
 *
 * Please note, this police relies on implementation of `required` option to determine
 * whether the field is set. Thus, inheriting its behavior regarding the supported
 * field types and specification about when a field of a specific type is considered
 * to be set.
 */
internal class GoesPolicy : Policy<FieldOptionDiscovered>() {

    private companion object {
        const val NO_ERROR_MESSAGE = ""
    }

    override val typeSystem by lazy { super.typeSystem!! }

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = GOES)
        event: FieldOptionDiscovered
    ): Just<RuleAdded> {
        val target = event.subject
        val option = event.option.value.unpack<GoesOption>()
        val declaringMessage = typeSystem.findMessage(target.declaringType)!!.first
        val companionName = FieldPath(option.with)
        val companion = typeSystem.resolve(companionName, declaringMessage)
        checkDistinct(target, companion)
        val rule = compositeRule {
            left = targetFieldShouldBeUnset(target)
            operator = LogicalOperator.OR
            right = companionFieldShouldBeSet(companion)
            errorMessage = option.errorMessage()
            field = target.name
        }.wrap()
        return just(rule.toEvent(target.declaringType))
    }

    /**
     * Checks that the given [target] and [companion] fields are distinct.
     *
     * Please note, this method does not use `==` comparison between two objects
     * because the field returned from [FieldOptionDiscovered] event has an empty
     * [options list][Field.getOptionList].
     */
    private fun checkDistinct(target: Field, companion: Field) =
        check(target.qualifiedName != companion.qualifiedName) {
            "The `($GOES)` option can not use the target field as its own companion. " +
                    "Self-referencing is prohibited. Please specify another field. " +
                    "The invalid field: `${target.qualifiedName}`."
        }

    /**
     * Creates a simple rule that makes sure the given [target] field is NOT set.
     */
    private fun targetFieldShouldBeUnset(target: Field) =
        RequiredRule.forField(target, NO_ERROR_MESSAGE)!!
            .simple.toBuilder()
            .setOperator(ComparisonOperator.EQUAL)
            .build().wrap()

    /**
     * Creates a simple rule that makes sure the given [companion] field is SET.
     */
    private fun companionFieldShouldBeSet(companion: Field) =
        RequiredRule.forField(companion, NO_ERROR_MESSAGE)!!

    /**
     * Returns an error message for the outer composite rule.
     */
    private fun GoesOption.errorMessage() = errorMsg.ifEmpty { DEFAULT_MESSAGE }
}

private val DEFAULT_MESSAGE = DefaultErrorMessage.from(GoesOption.getDescriptor())
