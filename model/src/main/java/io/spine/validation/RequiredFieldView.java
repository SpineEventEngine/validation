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
import io.spine.core.External;
import io.spine.core.Subscribe;
import io.spine.core.Where;
import io.spine.option.IfMissingOption;
import io.spine.protodata.FieldOptionDiscovered;
import io.spine.protodata.plugin.View;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.validation.EventFieldNames.OPTION_NAME;

/**
 * A view of a field that is marked as {@code required}.
 */
final class RequiredFieldView extends View<FieldId, RequiredField, RequiredField.Builder> {

    private static final String DEFAULT_ERROR_MESSAGE =
            DefaultErrorMessage.from(IfMissingOption.getDescriptor());

    @Subscribe
    void onConstraint(
            @External @Where(field = OPTION_NAME, equals = "required") FieldOptionDiscovered e
    ) {
        builder().setErrorMessage(DEFAULT_ERROR_MESSAGE);
        boolean value = unpack(e.getOption().getValue(), BoolValue.class).getValue();
        if (value) {
            builder().setRequired(true);
        }
    }

    @Subscribe
    void onErrorMessage(
            @External @Where(field = OPTION_NAME, equals = "if_missing") FieldOptionDiscovered e
    ) {
        IfMissingOption value = unpack(e.getOption().getValue(), IfMissingOption.class);
        String errorMessage = value.getMsgFormat();
        builder().setErrorMessage(errorMessage);
    }
}
