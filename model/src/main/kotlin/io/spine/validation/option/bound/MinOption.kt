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

package io.spine.validation.option.bound

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.option.MinOption
import io.spine.protodata.ast.FieldRef
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.ref
import io.spine.protodata.ast.unpack
import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.MIN_OPERATOR
import io.spine.validation.ErrorPlaceholder.MIN_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.MIN
import io.spine.validation.RANGE
import io.spine.validation.api.OPTION_NAME
import io.spine.validation.bound.MinField
import io.spine.validation.option.bound.BoundFieldSupport.checkFieldType
import io.spine.validation.defaultMessage
import io.spine.validation.bound.event.MinFieldDiscovered
import io.spine.validation.bound.event.minFieldDiscovered
import io.spine.validation.checkPlaceholders

/**
 * A policy to add a validation rule to a type whenever the `(min)` field option
 * is discovered.
 *
 * The condition checks done by the policy are similar to the ones performed by [RangePolicy].
 */
internal class MinPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = MIN)
        event: FieldOptionDiscovered
    ): Just<MinFieldDiscovered> {
        val field = event.subject
        val file = event.file
        val primitiveType = checkFieldType(field, file, MIN)

        val option = event.option.unpack<MinOption>()
        val context = BoundContext(MIN, primitiveType, field, file)
        val kotlinBound = context.checkNumericBound(option.value, option.exclusive)

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS,  field, file, RANGE)

        return minFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            this.min = option.value
            bound = kotlinBound.toProto()
            this.file = file
        }.just()
    }
}

/**
 * A view of a field that is marked with the `(min)` option.
 */
internal class MinFieldView : View<FieldRef, MinField, MinField.Builder>() {

    @Subscribe
    fun on(e: MinFieldDiscovered) = alter {
        subject = e.subject
        errorMessage = e.errorMessage
        min = e.min
        bound = e.bound
        file = e.file
    }
}

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_TYPE,
    FIELD_VALUE,
    MIN_OPERATOR,
    MIN_VALUE,
    PARENT_TYPE,
)
