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
import io.spine.protodata.ast.Option
import io.spine.protodata.ast.event.FieldOptionDiscovered

/**
 * A view of a field marked with `(validate)`.
 *
 * Note: this option [does not have][NO_ERROR_MESSAGE] an error message. It triggers
 * in-depth validation and directly propagates the occurred errors.
 */
internal class ValidatedFieldView :
    BoolFieldOptionView<ValidatedField, ValidatedField.Builder>(NO_ERROR_MESSAGE) {

    @Subscribe
    override fun onConstraint(
        @External @Where(field = OPTION_NAME, equals = VALIDATE) e: FieldOptionDiscovered
    ) = super.onConstraint(e)

    override fun enableValidation() {
        builder()!!.setValidate(true)
    }

    override fun extractErrorMessage(option: Option): String = error(
        "Cannot extract custom error message for `($VALIDATE)` option using `$option`. " +
                "`($VALIDATE)` does not support custom error messages."
    )

    override fun saveErrorMessage(errorMessage: String) {
        // No op.
    }

    private companion object {
        const val NO_ERROR_MESSAGE = ""
    }
}
