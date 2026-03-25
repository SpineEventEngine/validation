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

package io.spine.validation.given

import com.google.protobuf.NullValue
import com.google.protobuf.Timestamp
import com.google.protobuf.Value
import io.spine.base.Error
import io.spine.type.MessageClass
import io.spine.validation.ConstraintViolation
import io.spine.validation.ExceptionFactory
import java.io.Serial

/**
 * A stub exception that exposes the [error] passed by [ExceptionFactory] to
 * [ExceptionFactory.createException] for assertion purposes.
 */
internal class StubException(message: String, val error: Error) : Exception(message) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 0L
    }
}

/**
 * A stub [ExceptionFactory] for testing, using [Timestamp] as the invalid message type
 * and [NullValue] as the error code type.
 */
internal class StubExceptionFactory(
    message: Timestamp,
    violations: Iterable<ConstraintViolation>
) : ExceptionFactory<StubException, Timestamp, MessageClass<Timestamp>, NullValue>(
    message, violations
) {
    @Suppress("serial") // OK for this stub.
    override fun getMessageClass(): MessageClass<Timestamp> =
        object : MessageClass<Timestamp>(Timestamp::class.java) {}

    override fun getErrorCode(): NullValue = NullValue.NULL_VALUE

    override fun getErrorText(): String = ERROR_TEXT

    override fun getMessageTypeAttribute(message: Timestamp): Map<String, Value> = emptyMap()

    override fun createException(
        exceptionMsg: String,
        message: Timestamp,
        error: Error
    ): StubException = StubException(exceptionMsg, error)

    internal companion object {
        const val ERROR_TEXT = "Test validation error"
    }
}
