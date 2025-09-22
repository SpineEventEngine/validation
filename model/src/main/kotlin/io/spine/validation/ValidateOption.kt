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

package io.spine.validation

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.FieldRef
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.boolValue
import io.spine.tools.compiler.ast.event.FieldOptionDiscovered
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.ast.ref
import io.spine.tools.compiler.ast.refersToMessage
import io.spine.tools.compiler.check
import io.spine.tools.compiler.plugin.Policy
import io.spine.tools.compiler.plugin.View
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.api.OPTION_NAME
import io.spine.validation.event.ValidateFieldDiscovered
import io.spine.validation.event.validateFieldDiscovered

/**
 * Controls whether a field with the `(validate)` option should be
 * validated in-depth.
 *
 * Whenever a field marked with `(validate)` option is discovered, emits
 * [ValidateFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is supported by the option.
 * 2. The option value is `true`.
 *
 * If (1) is violated, the policy reports a compilation error.
 *
 * Violation of (2) means that the `(validate)` option is applied correctly,
 * but effectively disabled. [ValidateFieldDiscovered] is not emitted for
 * disabled options. In this case, the policy emits [NoReaction] meaning
 * that the option is ignored.
 */
internal class ValidatePolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = VALIDATE)
        event: FieldOptionDiscovered,
    ): EitherOf2<ValidateFieldDiscovered, NoReaction> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        if (!event.option.boolValue) {
            return ignore()
        }

        return validateFieldDiscovered {
            id = field.ref
            subject = field
        }.asA()
    }
}

/**
 * Reports a compilation warning if the deprecated `(if_invalid)` option is used.
 */
internal class IfInvalidPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = IF_INVALID)
        event: FieldOptionDiscovered
    ): Just<NoReaction> {
        Compilation.warning(event.file, event.subject.span) {
            "The `($IF_INVALID)` option is deprecated and should not longer be used. " +
                    " Applying this option has no effect. The `($VALIDATE)` option no longer" +
                    " accepts custom error messages. It only propagates messages from violations" +
                    " of the validated message itself."
        }
        return Just.noReaction
    }
}

/**
 * A view of a field that is marked with `(validate) = true` option.
 */
internal class ValidatedFieldView : View<FieldRef, ValidateField, ValidateField.Builder>() {

    @Subscribe
    fun on(e: ValidateFieldDiscovered) = alter {
        subject = e.subject
    }
}

private fun checkFieldType(field: Field, file: File) =
    Compilation.check(field.type.refersToMessage(), file, field.span) {
        "The field type `${field.type.name}` of `${field.qualifiedName}` is not supported" +
                " by the `($VALIDATE)` option. Supported field types: messages, repeated of" +
                " messages, and maps with message values."
    }
