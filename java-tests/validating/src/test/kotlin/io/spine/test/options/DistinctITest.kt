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
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.spine.protobuf.TypeConverter.toAny
import io.spine.test.tools.validate.ProtoSet
import io.spine.test.tools.validate.protoSet
import io.spine.validate.constraintViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@DisplayName("`(distinct)` option should be compiled, so that")
internal class DistinctITest {

    @Test
    fun `duplicated entries result in a violation`() {
        val msg = ProtoSet.newBuilder()
            .addElement(toAny("123"))
            .addElement(toAny("321"))
            .addElement(toAny("123"))
            .buildPartial()

        val error = msg.validate()
        error.shouldBePresent()

        val violations = error.get().constraintViolationList
        val expected = constraintViolation {
            fieldName = "element"
        }

        assertThat(violations)
            .comparingExpectedFieldsOnly()
            .containsExactly(expected)
    }

    @Test
    fun `unique elements do not result in a violation`() {
        val msg = assertDoesNotThrow {
            protoSet {
                element.add(toAny("42"))
                element.add(toAny(42))
            }
        }
        msg.validate().shouldBeEmpty()
    }

    @Test
    fun `empty list does not result in a violation`() {
        val msg = assertDoesNotThrow {
            ProtoSet.newBuilder().build()
        }
        msg.validate().shouldBeEmpty()
    }
}
