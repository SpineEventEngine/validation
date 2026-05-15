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

package io.spine.test.validation.time

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Durations.fromMillis
import com.google.protobuf.util.Timestamps
import io.spine.time.LocalDateTime
import io.spine.time.LocalDateTimes
import java.time.Instant
import java.time.LocalDateTime.ofInstant
import java.time.ZoneOffset.UTC

/**
 * Five hundred milliseconds.
 *
 * To shift the time into the past or future, we add or subtract a difference of this amount.
 *
 * There are two reasons for choosing 500 milliseconds:
 *
 * 1. The generated code uses `io.spine.base.Time.currentTime()` to get the current timestamp
 *    for comparison. In turn, this method relies on `io.spine.base.Time.SystemTimeProvider`
 *    by default, which has millisecond precision.
 * 2. Adding too small an amount of time to make the stamp denote "future" might be unreliable.
 *    As it could catch up `now` by the time `Time.currentTime()` is invoked.
 */
private const val HALF_OF_SECOND: Long = 500

internal object TemporalFixtures {

    fun pastTime(): LocalDateTime {
        val current = Instant.now() // It is a UTC stamp.
        return LocalDateTimes.of(ofInstant(current.minusMillis(HALF_OF_SECOND), UTC))
    }

    fun futureTime(): LocalDateTime {
        val current = Instant.now() // It is a UTC stamp.
        return LocalDateTimes.of(ofInstant(current.plusMillis(HALF_OF_SECOND), UTC))
    }
}

internal object TimestampFixtures {

    fun pastTime(): Timestamp =
        Timestamps.subtract(Timestamps.now(), fromMillis(HALF_OF_SECOND))

    fun futureTime(): Timestamp =
        Timestamps.add(Timestamps.now(), fromMillis(HALF_OF_SECOND))
}
