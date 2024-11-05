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
import com.google.protobuf.Empty
import io.spine.base.Identifier
import io.spine.test.tools.validate.AlwaysInvalid
import io.spine.test.tools.validate.Enclosed
import io.spine.test.tools.validate.Singulars
import io.spine.test.tools.validate.UltimateChoice
import io.spine.tools.validate.IsValid.assertValid
import io.spine.validation.assertions.checkViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("(required)` option in a message field should")
internal class RequiredMessageITest {

    @Test
    @DisplayName("cannot have a default instance")
    fun `prohibit a default message`() {
        val singulars = Singulars.newBuilder()
        checkViolation(singulars, "not_default")
    }

    @Test
    fun `accept a non-default instance`() {
        val singulars = Singulars.newBuilder()
            .setNotVegetable(UltimateChoice.CHICKEN)
            .setOneOrMoreBytes(ByteString.copyFromUtf8("lalala"))
            .setNotDefault(Enclosed.newBuilder().setValue(Identifier.newUuid()))
            .setNotEmptyString(" ")
        assertValid(singulars)
    }

    @Test
    fun `cannot be of type 'Empty'`() {
        val fieldName = "impossible"

        val unset = AlwaysInvalid.newBuilder()
        checkViolation(unset, fieldName)

        val set = AlwaysInvalid.newBuilder()
            .setImpossible(Empty.getDefaultInstance())
        checkViolation(set, fieldName)
    }
}
