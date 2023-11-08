/*
 * Copyright 2023, TeamDev. All rights reserved.
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
package io.spine.validate

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Message
import io.spine.validate.Validate.violationsOf
import io.spine.validate.given.MessageValidatorTestEnv.assertFieldPathIs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * The abstract base for test suites of validation constraints.
 */
abstract class ValidationOfConstraintTest {

    private var violations: List<ConstraintViolation>? = null

    protected fun validate(msg: Message) {
        violations = violationsOf(msg)
    }

    protected fun firstViolation(): ConstraintViolation = violations!![0]

    protected fun singleViolation(): ConstraintViolation {
        assertThat(violations).hasSize(1)
        return violations!![0]
    }

    protected fun assertValid(setup: () -> Message) {
        assertDoesNotThrow {
            val msg = setup()
            assertValid(msg)
        }
    }

    /**
     * Asserts that calling the `build()` method of the passed builder throws `ValidationException`.
     */
    protected fun assertDoesNotBuild(builder: Message.Builder) {
        assertThrows<ValidationException> {
            builder.build()
        }
    }

    /**
     * Asserts that the creation of the message fails when the [setup] function is invoked.
     *
     * The most common use case is when the message is created via a Kotlin DSL block.
     *
     * @see assertDoesNotBuild
     */
    protected fun assertDoesNotBuild(setup: () -> Message) {
        assertThrows<ValidationException> {
            setup()
        }
    }

    /**
     * Asserts that the give message fails the [validation check][Validate.check].
     *
     * Use this method for checking validation constraints against
     * [default instances][Message.getDefaultInstanceForType].
     */
    protected fun assertCheckFails(message: Message) {
        assertThrows<ValidationException> {
            Validate.check(message)
        }
    }

    protected fun assertValid(msg: Message) {
        validate(msg)
        assertIsValid(true)
    }

    protected fun assertNotValid(msg: Message) {
        validate(msg)
        assertIsValid(false)
    }

    protected fun assertNotValid(msg: Message, checkFieldPath: Boolean) {
        validate(msg)
        assertIsValid(false, checkFieldPath)
    }

    @JvmOverloads
    protected fun assertIsValid(isValid: Boolean, checkFieldPath: Boolean = true) {
        if (isValid) {
            assertThat(violations).isEmpty()
        } else {
            assertViolations(violations, checkFieldPath)
        }
    }

    protected fun assertSingleViolation(
        message: Message,
        expectedErrMsg: String,
        invalidFieldName: String
    ) {
        assertNotValid(message)
        assertThat(violations).hasSize(1)
        assertSingleViolation(expectedErrMsg, invalidFieldName)
    }

    /** Checks that a message is not valid and has a single violation.  */
    protected fun assertSingleViolation(expectedErrMsg: String, invalidFieldName: String) {
        val violation = firstViolation()
        val actualErrorMessage = String.format(
            violation.msgFormat, *violation.paramList.toTypedArray()
        )
        assertThat(actualErrorMessage).isEqualTo(expectedErrMsg)
        assertFieldPathIs(violation, invalidFieldName)
        assertThat(violation.violationList).isEmpty()
    }

    protected fun assertSingleViolation(message: Message, invalidFieldName: String) {
        assertNotValid(message)
        assertThat(violations).hasSize(1)
        assertFieldPathIs(firstViolation(), invalidFieldName)
    }

    companion object {

        const val VALIDATION_SHOULD: String = "Validation should "

        private fun assertViolations(
            violations: List<ConstraintViolation?>?,
            checkFieldPath: Boolean
        ) {
            assertThat(violations)
                .isNotEmpty()
            for (violation in violations!!) {
                assertHasCorrectFormat(violation)
                if (checkFieldPath) {
                    assertHasFieldPath(violation)
                }
            }
        }

        private fun assertHasCorrectFormat(violation: ConstraintViolation?) {
            val format = violation!!.msgFormat
            Assertions.assertFalse(format.isEmpty())
            val noParams = violation.paramList.isEmpty()
            if (noParams) {
                assertThat(format)
                    .doesNotContain("%s")
            } else {
                assertThat(format)
                    .contains("%s")
            }
        }

        private fun assertHasFieldPath(violation: ConstraintViolation?) {
            assertThat(violation!!.fieldPath.fieldNameList)
                .isNotEmpty()
        }
    }
}
