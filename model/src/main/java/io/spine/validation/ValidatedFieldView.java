/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.core.External;
import io.spine.core.Subscribe;
import io.spine.core.Where;
import io.spine.option.IfInvalidOption;
import io.spine.protodata.ast.event.FieldOptionDiscovered;
import io.spine.protodata.ast.Option;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.validation.EventFieldNames.OPTION_NAME;

/**
 * A view of a field that is marked with {@code validate}.
 */
final class ValidatedFieldView
        extends BoolFieldOptionView<FieldId, ValidatedField, ValidatedField.Builder> {

    ValidatedFieldView() {
        super(IfInvalidOption.getDescriptor());
    }

    @Override
    @Subscribe
    void onConstraint(
            @External @Where(field = OPTION_NAME, equals = "validate") FieldOptionDiscovered e
    ) {
        super.onConstraint(e);
    }

    @Override
    protected void errorMessage(String errorMessage) {
        builder().setErrorMessage(errorMessage);
    }

    @Override
    protected void enableValidation() {
        builder().setValidate(true);
    }

    @Override
    @Subscribe
    void onErrorMessage(
            @External @Where(field = OPTION_NAME, equals = "if_invalid") FieldOptionDiscovered e
    ) {
        super.onErrorMessage(e);
    }

    @Override
    protected String extractErrorMessage(Option option) {
        var value = unpack(option.getValue(), IfInvalidOption.class);
        var errorMessage = value.getErrorMsg();
        return errorMessage;
    }
}
