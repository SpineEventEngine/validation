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

package io.spine.tools.validation.bound

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.option.MaxOption
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.tools.compiler.ast.FieldRef
import io.spine.tools.compiler.ast.event.FieldOptionDiscovered
import io.spine.tools.compiler.ast.ref
import io.spine.tools.compiler.ast.unpack
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.compiler.plugin.View
import io.spine.tools.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.tools.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.tools.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.tools.validation.ErrorPlaceholder.MAX_OPERATOR
import io.spine.tools.validation.ErrorPlaceholder.MAX_VALUE
import io.spine.tools.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.tools.validation.OPTION_NAME
import io.spine.tools.validation.bound.BoundFieldSupport.checkFieldType
import io.spine.tools.validation.checkPlaceholders
import io.spine.tools.validation.defaultMessage
import io.spine.tools.validation.option.MAX
import io.spine.tools.validation.option.RANGE
import io.spine.validation.bound.MaxField
import io.spine.validation.bound.event.MaxFieldDiscovered
import io.spine.validation.bound.event.maxFieldDiscovered

/**
 * A reaction to add a validation rule to a type whenever the `(max)` field option
 * is discovered.
 *
 * The condition checks done by the reaction are similar to the ones performed by [RangeReaction].
 */
internal class MaxReaction : Reaction<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = MAX)
        event: FieldOptionDiscovered
    ): Just<MaxFieldDiscovered> {
        val field = event.subject
        val file = event.file
        val fieldType = checkFieldType(field, file, MAX)

        val option = event.option.unpack<MaxOption>()
        val metadata = NumericOptionMetadata(MAX, field, fieldType, file, typeSystem)
        val bound = NumericBoundParser(metadata)
            .parse(option.value, option.exclusive)

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS,  field, file, RANGE)

        return maxFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            this.max = option.value
            this.bound = bound.toProto()
            this.file = file
        }.just()
    }
}

/**
 * A view of a field that is marked with the `(max)` option.
 */
internal class MaxFieldView : View<FieldRef, MaxField, MaxField.Builder>() {

    @Subscribe
    fun on(e: MaxFieldDiscovered) = alter {
        subject = e.subject
        errorMessage = e.errorMessage
        max = e.max
        bound = e.bound
        file = e.file
    }
}

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_TYPE,
    FIELD_VALUE,
    MAX_OPERATOR,
    MAX_VALUE,
    PARENT_TYPE,
)
