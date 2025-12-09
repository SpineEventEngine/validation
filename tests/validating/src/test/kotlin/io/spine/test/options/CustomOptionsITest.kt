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

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.ByteString
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.spine.base.fieldPath
import io.spine.test.tools.validate.ByteMatrix
import io.spine.tools.validate.rule.BytesAllRequiredFactory
import io.spine.validate.constraintViolation
import io.spine.validate.option.ValidatingOptionsLoader
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Custom validation options should")
internal class CustomOptionsITest {

    @Test
    fun `be discovered`() {
        val implementations = ValidatingOptionsLoader.INSTANCE.implementations()
        val classes = implementations.map { it::class.java }
        classes.contains(BytesAllRequiredFactory::class.java)
    }

    @Test
    @Disabled("https://github.com/SpineEventEngine/mc-java/issues/119")
    fun `be applied to validated messages`() {
        val matrix = ByteMatrix.newBuilder()
            .addValue(ByteString.copyFrom(byteArrayOf(42)))
            .addValue(ByteString.EMPTY)
            .buildPartial()

        val error = matrix.validate()
        error.shouldBePresent()

        val violations = error.get().constraintViolationList
        val expected = constraintViolation { fieldPath = fieldPath { fieldName.add("value") } }
        assertThat(violations)
            .comparingExpectedFieldsOnly()
            .containsExactly(expected)
    }

    @Test
    fun `be applied to valid messages and pass`() {
        val matrix = ByteMatrix.newBuilder()
            .addValue(ByteString.copyFrom(byteArrayOf(42)))
            .build()
        matrix.validate().shouldBeEmpty()
    }
}
