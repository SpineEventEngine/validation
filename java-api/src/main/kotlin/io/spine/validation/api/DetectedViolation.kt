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

package io.spine.validation.api

import io.spine.base.FieldPath
import io.spine.validate.TemplateString

/**
 * Abstract base for violations detected by [MessageValidator]s.
 *
 * @param message The error message describing the violation.
 * @param fieldPath The path to the field where the violation occurred, if applicable.
 * @param fieldValue The field value that caused the violation, if any.
 */
public abstract class DetectedViolation(
    public val message: TemplateString,
    public val fieldPath: FieldPath?,
    public val fieldValue: Any?,
)

/**
 * A [MessageValidator] violation associated with a specific field.
 *
 * @param message The error message describing the violation.
 * @param fieldPath The path to the field where the violation occurred.
 * @param fieldValue The field value that caused the violation, if any.
 */
public class FieldViolation(
    message: TemplateString,
    fieldPath: FieldPath,
    fieldValue: Any? = null,
) : DetectedViolation(message, fieldPath, fieldValue)

/**
 * A [MessageValidator] violation related to the message level (not tied to a specific field).
 *
 * @param message The error message describing the violation.
 */
public class MessageViolation(
    message: TemplateString
) : DetectedViolation(message, fieldPath = null, fieldValue = null)
