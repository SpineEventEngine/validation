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

package io.spine.validation.assertions

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Message
import io.kotest.matchers.string.shouldContain
import io.spine.tools.validate.IsValid.assertInvalid
import io.spine.validate.ConstraintViolation
import org.junit.jupiter.api.Assertions.fail

private val fieldName: Correspondence<ConstraintViolation, String> =
    Correspondence.transforming(
        { it.fieldPath.getFieldName(0) },
        "field name"
    )

fun checkViolation(
    message: Message.Builder,
    field: String,
    errorMessagePart: String = "must be set"
) {
    val violations = assertInvalid(message)
    assertThat(violations)
        .comparingElementsUsing(fieldName)
        .contains(field)

    val violation = violations.atField(field)

    violation.msgFormat shouldContain errorMessagePart
}

private fun List<ConstraintViolation>.atField(fieldName: String): ConstraintViolation {
    return find { it.fieldPath.fieldNameList[0] == fieldName }
        ?: fail("No violation for field `$fieldName`. Violations: `$this`.")
}
