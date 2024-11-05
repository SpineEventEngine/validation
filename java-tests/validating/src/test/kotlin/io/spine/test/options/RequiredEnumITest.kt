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

package io.spine.test.options

import com.google.protobuf.ByteString
import io.spine.test.tools.validate.Enclosed
import io.spine.test.tools.validate.Singulars
import io.spine.test.tools.validate.UltimateChoice
import io.spine.tools.validate.IsValid.assertValid
import io.spine.validation.assertions.checkViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("(required)` option in an enum field should")
internal class RequiredEnumITest {

    @Test
    fun `not allow a zero-index enum item value`() {
        val msg = Singulars.newBuilder()
            .setNotVegetable(UltimateChoice.VEGETABLE)
        checkViolation(msg, "not_vegetable")
    }

    @Test
    fun `accept a non-zero index item value`() {
        val singulars = Singulars
            .newBuilder()
            .setOneOrMoreBytes(ByteString.copyFrom(byteArrayOf(0)))
            .setNotDefault(Enclosed.newBuilder().setValue("baz"))
            .setNotVegetable(UltimateChoice.CHICKEN)
            .setNotEmptyString("not empty")
        assertValid(singulars)
    }
}
