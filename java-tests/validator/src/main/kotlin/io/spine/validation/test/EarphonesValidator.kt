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

package io.spine.validation.test

import com.google.common.annotations.VisibleForTesting
import io.spine.base.FieldPath
import io.spine.base.fieldPath
import io.spine.validate.TemplateString
import io.spine.validate.templateString
import io.spine.validation.api.DetectedViolation
import io.spine.validation.api.FieldViolation
import io.spine.validation.api.MessageValidator
import io.spine.validation.api.Validator

/**
 * Validates [Earphones]s, treating all instances as invalid except for [ValidEarphones].
 */
@Validator(Earphones::class)
public class EarphonesValidator : MessageValidator<Earphones> {

    public override fun validate(message: Earphones): List<DetectedViolation> {
        if (message === ValidEarphones) {
            return emptyList()
        }

        val violation = FieldViolation(
            message = Violation.message,
            fieldPath = Violation.fieldPath,
            fieldValue = message.price
        )

        return listOf(violation)
    }

    @VisibleForTesting
    public companion object {

        /**
         * The [EarphonesValidator] considers only this instance as valid.
         */
        public val ValidEarphones: Earphones = earphones {
            modelName = "SN532"
            manufacturer = "Earphones Inc."
            price = 100.0
        }
    }

    @VisibleForTesting
    public object Violation {

        /**
         * The error message used for the reported violations.
         */
        public val message: TemplateString = templateString {
            withPlaceholders = "Price is too high."
        }

        /**
         * The field path used for the reported violations.
         */
        public val fieldPath: FieldPath = fieldPath {
            fieldName.add("price")
        }
    }
}
