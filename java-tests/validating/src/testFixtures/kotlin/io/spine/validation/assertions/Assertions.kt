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

@file:JvmName("Assertions")

package io.spine.validation.assertions

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.Message
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.type.toJson
import io.spine.validate.ConstraintViolation
import io.spine.validate.ValidationException
import io.spine.validate.text.format
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.function.Executable

/**
 * Assert the given [builder] produces a valid message.
 *
 * @param builder The message builder.
 */
fun assertValid(builder: Message.Builder) {
    val msg = builder.build()
    msg shouldNotBe null
}

/**
 * Assert the given `builder` produces an invalid message.
 *
 * @param builder The message builder.
 * @return Violations received from building the message.
 */
@CanIgnoreReturnValue
fun assertInvalid(builder: Message.Builder): List<ConstraintViolation> {
    try {
        val msg = builder.build()
        return fail("Expected an invalid message but got: ${msg.toJson()}")
    } catch (e: ValidationException) {
        return e.constraintViolations
    }
}

private val fieldName: Correspondence<ConstraintViolation, String> =
    Correspondence.transforming(
        { it.fieldPath.getFieldName(0) },
        "field name"
    )

/**
 * Asserts that the builder of the message produces a violation for the field
 * with the given name, and that the error message contains the given part.
 */
fun assertViolation(
    message: Message.Builder,
    field: String,
    errorMessagePart: String = "must have a value"
) {
    val violations = assertInvalid(message)
    assertThat(violations)
        .comparingElementsUsing(fieldName)
        .contains(field)

    val violation = violations.atField(field)
    violation.message.format() shouldContain errorMessagePart
}

/**
 * Obtains a violation for the field with the given name.
 */
fun List<ConstraintViolation>.atField(fieldName: String): ConstraintViolation {
    return find { it.fieldPath.fieldNameList[0] == fieldName }
        ?: fail("Cannot find a violation for the field `$fieldName`. Violations: `$this`.")
}

/**
 * Asserts that the given `executable` throws [ValidationException].
 */
fun assertValidationFails(executable: Executable) {
    assertThrows(ValidationException::class.java, executable)
}

/**
 * Asserts that the given `executable` doesn't throw anything.
 */
fun assertValidationPasses(executable: Executable) {
    assertDoesNotThrow(executable)
}
