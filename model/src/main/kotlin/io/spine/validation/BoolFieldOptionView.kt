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

import com.google.protobuf.Descriptors.Descriptor
import io.spine.base.EntityState
import io.spine.core.ContractFor
import io.spine.core.Subscribe
import io.spine.protodata.ast.Option
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.plugin.View
import io.spine.validate.ValidatingBuilder

/**
 * A view on a field marked with a boolean validation option.
 *
 * @param S The type of the view entity state.
 * @param B The type of the validating builder of the state.
 */
internal abstract class BoolFieldOptionView<
        S : EntityState<FieldId>,
        B : ValidatingBuilder<S>
        >(optionDescriptor: Descriptor) : View<FieldId, S, B>() {

    private val defaultMessage: String = DefaultErrorMessage.from(optionDescriptor)

    @ContractFor(handler = Subscribe::class)
    open fun onConstraint(e: FieldOptionDiscovered) {
        saveErrorMessage(defaultMessage)
        if (e.option.boolValue) {
            enableValidation()
        }
    }

    /**
     * Saves the given error message into the view.
     */
    protected abstract fun saveErrorMessage(errorMessage: String)

    /**
     * Enables the validation associated with the option.
     */
    protected abstract fun enableValidation()

    @ContractFor(handler = Subscribe::class)
    open fun onErrorMessage(e: FieldOptionDiscovered) {
        val message = extractErrorMessage(e.option)
        saveErrorMessage(message)
    }

    /**
     * Attempts to extract a custom error message from the given option.
     *
     * @throws io.spine.type.UnexpectedTypeException If the option value is of an unexpected type.
     */
    protected abstract fun extractErrorMessage(option: Option): String
}
