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

package io.spine.validate.option

import io.kotest.matchers.shouldBe
import io.spine.test.validate.Sauce
import io.spine.test.validate.fish
import io.spine.test.validate.meal
import io.spine.testing.TestValues.randomString
import io.spine.validate.ValidationException
import io.spine.validate.ValidationOfConstraintTest
import io.spine.validate.ValidationOfConstraintTest.Companion.VALIDATION_SHOULD
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName(VALIDATION_SHOULD + "analyze `(is_required)` oneof option and")
internal class IsRequiredSpec : ValidationOfConstraintTest() {

    @Test
    fun `throw if required field group is not set`() {
        val exception = assertThrows<ValidationException> {
            meal {
                cheese = Sauce.getDefaultInstance()
            }
        }
        val violation = exception.constraintViolations[0]

        // We here check the value of the `msgFormat`, and not the `fieldPath` because
        // `oneof` is not a field. As such, it does not have a `fieldPath`, and is not
        // added to validation constraint by the code generation.
        violation.msgFormat shouldBe "One of the fields in the `choice` group must be set."
    }

    @Test
    fun `not throw if required field group is set`() = assertValid {
        meal {
            cheese = Sauce.getDefaultInstance()
            fish = fish {
                description = randomString()
            }
        }
    }

    @Test
    fun `ignore non-required field groups`() = assertValid {
        meal {
            fish = fish {
                description = randomString()
            }
        }
    }
}
