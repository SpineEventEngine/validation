/*
 * Copyright 2026, TeamDev. All rights reserved.
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

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import com.google.protobuf.util.Timestamps.MAX_VALUE
import com.google.protobuf.util.Timestamps.MIN_VALUE
import io.spine.base.fieldPath
import io.spine.validation.RuntimeErrorPlaceholder.FIELD_PATH
import io.spine.validation.RuntimeErrorPlaceholder.RANGE_VALUE

/**
 * Validates [Timestamp] messages.
 *
 * Uses [Timestamps.MIN_VALUE] and [Timestamps.MAX_VALUE] to ensure
 * the fields of the timestamp are valid.
 */
@Validator(Timestamp::class)
public class TimestampValidator : MessageValidator<Timestamp> {

    override fun validate(message: Timestamp): List<DetectedViolation> {
        if (Timestamps.isValid(message)) {
            return emptyList()
        }
        val violations = mutableListOf<DetectedViolation>()
        if (message.seconds < MIN_VALUE.seconds ||
            message.seconds > MAX_VALUE.seconds) {
            violations.add(invalidSeconds(message.seconds))
        }
        if (message.nanos !in 0..MAX_VALUE.nanos) {
            violations.add(invalidNanos(message.nanos))
        }
        return violations
    }

    private companion object {

        /**
         * Creates a violation for invalid seconds.
         */
        fun invalidSeconds(seconds: Long): FieldViolation {
            return FieldViolation(
                message = templateString {
                    withPlaceholders =
                        "The ${FIELD_PATH.value} value is out of range" +
                                " (${RANGE_VALUE.value}): $seconds."
                    placeholderValue.put(FIELD_PATH.value, "seconds")
                    placeholderValue.put(RANGE_VALUE.value,
                        "${MIN_VALUE.seconds}..${MAX_VALUE.seconds}")

                },
                fieldPath = fieldPath {
                    fieldName.add("seconds")
                },
                fieldValue = seconds
            )
        }

        /**
         * Creates a violation for invalid nanos.
         */
        fun invalidNanos(nanos: Int): FieldViolation {
            return FieldViolation(
                message = templateString {
                    withPlaceholders = "The ${FIELD_PATH.value} value is out of range" +
                            ":  (${RANGE_VALUE.value})$nanos."
                    placeholderValue.put(FIELD_PATH.value, "nanos")
                    placeholderValue.put(RANGE_VALUE.value, "0..${MAX_VALUE.nanos}")
                },
                fieldPath = fieldPath {
                    fieldName.add("nanos")
                },
                fieldValue = nanos
            )
        }
    }
}
