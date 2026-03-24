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

import com.google.protobuf.NullValue
import com.google.protobuf.Timestamp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.protobuf.unpackKnownType
import io.spine.validation.diags.ViolationText
import io.spine.validation.given.StubExceptionFactory
import io.spine.validation.given.plainString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ExceptionFactory` should")
internal class ExceptionFactorySpec {

    private val violation = constraintViolation {
        typeName = "example.org/example.InvalidMessage"
        message = plainString("Field value is missing")
    }
    private val factory = StubExceptionFactory(Timestamp.getDefaultInstance(), listOf(violation))

    @Test
    fun `include the error text in the exception message`() {
        val exception = factory.newException()
        exception.message shouldContain StubExceptionFactory.ERROR_TEXT
    }

    @Test
    fun `include the message class in the exception message`() {
        val exception = factory.newException()
        exception.message shouldContain Timestamp::class.java.name
    }

    @Test
    fun `include the violation text in the exception message`() {
        val exception = factory.newException()
        exception.message shouldContain ViolationText.of(violation).toString()
    }

    @Test
    fun `set the error code number in the produced error`() {
        val exception = factory.newException()
        exception.error.code shouldBe NullValue.NULL_VALUE.number
    }

    @Test
    fun `set the error type using the error code descriptor`() {
        val exception = factory.newException()
        exception.error.type shouldBe NullValue.NULL_VALUE.descriptorForType.fullName
    }

    @Test
    fun `pack a 'ValidationError' into the error details`() {
        val exception = factory.newException()
        val expected = validationError { constraintViolation.add(violation) }
        exception.error.details.unpackKnownType() shouldBe expected
    }

    @Test
    fun `include the violation text in the error message`() {
        val exception = factory.newException()
        exception.error.message shouldContain ViolationText.of(violation).toString()
    }
}
