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

package io.spine.validation.required

import io.spine.core.Subscribe
import io.spine.protodata.ast.FieldRef
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.validation.RequiredField
import io.spine.validation.event.IfMissingOptionDiscovered
import io.spine.validation.event.RequiredIdFieldDiscovered
import io.spine.validation.event.RequiredFieldDiscovered

/**
 * A view of a field that is marked with `(required) = true` option.
 */
internal class RequiredFieldView : View<FieldRef, RequiredField, RequiredField.Builder>() {

    @Subscribe
    fun on(e: RequiredFieldDiscovered) {
        val currentMessage = state().errorMessage
        val message = currentMessage.ifEmpty { e.defaultErrorMessage }
        alter {
            subject = e.subject
            errorMessage = message
        }
    }

    @Subscribe
    fun on(e: IfMissingOptionDiscovered) = alter {
        errorMessage = e.customErrorMessage
    }

    @Subscribe
    fun on(e: RequiredIdFieldDiscovered) = alter {
        subject = e.subject
        errorMessage = e.errorMessage
    }
}
