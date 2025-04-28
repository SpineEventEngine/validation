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

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.option.ChoiceOption
import io.spine.protodata.Compilation
import io.spine.protodata.ast.OneofRef
import io.spine.protodata.ast.event.OneofOptionDiscovered
import io.spine.protodata.ast.ref
import io.spine.protodata.ast.unpack
import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.event.ChoiceOneofDiscovered
import io.spine.validation.event.choiceOneofDiscovered

/**
 * Controls whether a `oneof` group should be validated with the `(choice)` option.
 *
 * Whenever a `oneof` groupd marked with `(choice)` option is discovered,
 * emits [ChoiceOneofDiscovered] event if the option has the `required` flag
 * set to `true`. Otherwise, the policy emits [NoReaction].
 *
 * Note that unlike the `(required)` constraint, this option supports any field type.
 * Protobuf encodes a non-set value as a special case, allowing for checking whether
 * the `oneof` group value is set without relying on default values of field types.
 */
internal class ChoicePolicy : Policy<OneofOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = CHOICE)
        event: OneofOptionDiscovered
    ): EitherOf2<ChoiceOneofDiscovered, NoReaction> {
        val option = event.option.unpack<ChoiceOption>()
        if (!option.required) {
            return ignore()
        }

        val oneof = event.subject
        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return choiceOneofDiscovered {
            id = oneof.ref
            subject = oneof
            errorMessage = message
        }.asA()
    }
}

/**
 * Reports a compilation warning if the deprecated `(is_required)` option is used.
 */
internal class IsRequiredPolicy : Policy<OneofOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = IS_REQUIRED)
        event: OneofOptionDiscovered
    ): Just<NoReaction> {
        Compilation.warning(event.file, event.subject.span) {
            "The `($IS_REQUIRED)` option is deprecated and no longer supported. " +
                    " Please use `(choice).required = true` instead."
        }
        return Just.noReaction
    }
}

/**
 * A view of a `oneof` group that is marked with `(choice).required = true` option.
 */
internal class ChoiceGroupView : View<OneofRef, ChoiceOneof, ChoiceOneof.Builder>() {

    @Subscribe
    fun on(e: ChoiceOneofDiscovered) = alter {
        subject = e.subject
        errorMessage = e.errorMessage
    }
}
