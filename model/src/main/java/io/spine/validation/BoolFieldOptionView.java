/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.validation;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Descriptors.Descriptor;
import io.spine.base.EntityState;
import io.spine.core.ContractFor;
import io.spine.core.Subscribe;
import io.spine.protodata.FieldOptionDiscovered;
import io.spine.protodata.Option;
import io.spine.protodata.plugin.View;
import io.spine.validate.ValidatingBuilder;

import static io.spine.protobuf.AnyPacker.unpack;

abstract class BoolFieldOptionView<
        I extends FieldId,
        S extends EntityState<I>,
        B extends ValidatingBuilder<S>>
        extends View<I, S, B> {

    private final String defaultMessage;

    protected BoolFieldOptionView(Descriptor optionDescriptor) {
        this.defaultMessage = DefaultErrorMessage.from(optionDescriptor);
    }

    @ContractFor(handler = Subscribe.class)
    void onConstraint(FieldOptionDiscovered e) {
        errorMessage(defaultMessage);
        boolean value = unpack(e.getOption()
                                .getValue(), BoolValue.class).getValue();
        if (value) {
            enableValidation();
        }
    }

    protected abstract void errorMessage(String errorMessage);

    protected abstract void enableValidation();

    @ContractFor(handler = Subscribe.class)
    void onErrorMessage(FieldOptionDiscovered e) {
        String message = extractErrorMessage(e.getOption());
        errorMessage(message);
    }

    protected abstract String extractErrorMessage(Option option);
}
