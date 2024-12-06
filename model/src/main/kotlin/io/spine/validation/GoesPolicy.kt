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
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.plugin.Policy
import io.spine.protodata.type.resolve
import io.spine.server.event.Just
import io.spine.server.event.Just.Companion.just
import io.spine.server.event.React
import io.spine.validation.event.RuleAdded

internal class GoesPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = GOES)
        event: FieldOptionDiscovered
    ): Just<RuleAdded> {
        val thisField = event.subject
        val option = event.option.value.unpack<GoesOption>()

        // TODO:2024-12-03:yevhenii.nadtochii: Create an overload for `resolve` with `TypeName`.
        val declaringMessage = typeSystem!!.findMessage(thisField.declaringType)!!.first

        val thisFieldShouldBeUnset = RequiredRule.forField(thisField, option.errorMessage())!!
            .simple.toBuilder()
            .setOperator(ComparisonOperator.EQUAL)
            .build()
            .wrap()

        val companionFieldName = FieldPath(option.with)
        val companionField = typeSystem!!.resolve(companionFieldName, declaringMessage)
        val companionFieldShouldBeSet = RequiredRule.forField(companionField, "Left side")!!

        val rule = compositeRule {
            left = thisFieldShouldBeUnset
            operator = LogicalOperator.OR
            right = companionFieldShouldBeSet
            errorMessage = option.errorMessage()
            field = thisField.name
        }.wrap()

        return just(rule.toEvent(thisField.declaringType))
    }

    private fun GoesOption.errorMessage() = errorMsg.ifEmpty { DEFAULT_MESSAGE }
}

private val DEFAULT_MESSAGE = DefaultErrorMessage.from(GoesOption.getDescriptor())
