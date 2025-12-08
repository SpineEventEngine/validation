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

package io.spine.tools.validation.test

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.spine.base.FieldPath
import io.spine.base.fieldPath
import io.spine.validate.DetectedViolation
import io.spine.validate.FieldViolation
import io.spine.validate.MessageValidator
import io.spine.validate.TemplateString
import io.spine.validate.Validator
import io.spine.validate.templateString

/**
 * Validates [com.google.protobuf.Timestamp] messages, treating all instances as invalid
 * except for [ValidTimestamp].
 */
@Validator(Timestamp::class)
public class TheOnlyTimeValid : MessageValidator<Timestamp> {

    public override fun validate(message: Timestamp): List<DetectedViolation> {
        if (message === ValidTimestamp) {
            return emptyList()
        }

        val violation = FieldViolation(
            message = Violation.message,
            fieldPath = Violation.fieldPath,
            fieldValue = message.seconds
        )

        return listOf(violation)
    }

    @VisibleForTesting
    public companion object {

        /**
         * The [TheOnlyTimeValid] considers only this instance as valid.
         */
        public val ValidTimestamp: Timestamp = Timestamps.fromMillis(893755250000L)
    }

    @VisibleForTesting
    public object Violation {

        /**
         * The error message used for the reported violations.
         */
        public val message: TemplateString = templateString {
            withPlaceholders = "Invalid timestamp."
        }

        /**
         * The field path used for the reported violations.
         */
        public val fieldPath: FieldPath = fieldPath {
            fieldName.add("seconds")
        }
    }
}
