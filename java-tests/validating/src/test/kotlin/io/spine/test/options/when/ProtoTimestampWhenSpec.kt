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

package io.spine.test.options.`when`

import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Durations
import com.google.protobuf.util.Timestamps
import io.spine.test.tools.validate.anyProtoTimestamp
import io.spine.test.tools.validate.anyProtoTimestamps
import io.spine.test.tools.validate.futureProtoTimestamp
import io.spine.test.tools.validate.futureProtoTimestamps
import io.spine.test.tools.validate.pastProtoTimestamp
import io.spine.test.tools.validate.pastProtoTimestamps
import io.spine.validation.assertions.assertValidationFails
import io.spine.validation.assertions.assertValidationPasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("If used with Protobuf `Timestamp`, `(when)` constrain should")
internal class ProtoTimestampWhenSpec {

    @Nested inner class
    `when given a timestamp denoting` {

        @Nested inner class
        `the past` {

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                futureProtoTimestamp {
                    value = pastTime()
                }
            }

            @Test
            fun `pass, if restricted to be in past`() = assertValidationPasses {
                pastProtoTimestamp {
                    value = pastTime()
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anyProtoTimestamp {
                    value = pastTime()
                }
            }
        }

        @Nested inner class
        `the future` {

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                pastProtoTimestamp {
                    value = futureTime()
                }
            }

            @Test
            fun `pass, if restricted to be in future`() = assertValidationPasses {
                futureProtoTimestamp {
                    value = futureTime()
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anyProtoTimestamp {
                    value = futureTime()
                }
            }
        }
    }

    @Nested inner class
    `when given several timestamps` {

        @Nested inner class
        `denoting only the past` {

            private val severalPastTimes = listOf(pastTime(), pastTime(), pastTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                futureProtoTimestamps {
                    value.addAll(severalPastTimes)
                }
            }

            @Test
            fun `pass, if restricted to be in past`() = assertValidationPasses {
                pastProtoTimestamps {
                    value.addAll(severalPastTimes)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anyProtoTimestamps {
                    value.addAll(severalPastTimes)
                }
            }
        }

        @Nested inner class
        `denoting only the future` {

            private val severalFutureTimes = listOf(futureTime(), futureTime(), futureTime())

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                pastProtoTimestamps {
                    value.addAll(severalFutureTimes)
                }
            }

            @Test
            fun `pass, if restricted to be in future`() = assertValidationPasses {
                futureProtoTimestamps {
                    value.addAll(severalFutureTimes)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anyProtoTimestamps {
                    value.addAll(severalFutureTimes)
                }
            }
        }

        @Nested inner class
        `with a single past stamp within the future stamps` {

            private val severalFutureAndPast = listOf(futureTime(), pastTime(), futureTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                futureProtoTimestamps {
                    value.addAll(severalFutureAndPast)
                }
            }

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                pastProtoTimestamps {
                    value.addAll(severalFutureAndPast)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anyProtoTimestamps {
                    value.addAll(severalFutureAndPast)
                }
            }
        }

        @Nested inner class
        `with a single future stamp within the past stamps` {

            private val severalPastAndFuture = listOf(pastTime(), futureTime(), pastTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                futureProtoTimestamps {
                    value.addAll(severalPastAndFuture)
                }
            }

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                pastProtoTimestamps {
                    value.addAll(severalPastAndFuture)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anyProtoTimestamps {
                    value.addAll(severalPastAndFuture)
                }
            }
        }
    }
}

private fun pastTime(): Timestamp {
    val current = Timestamps.now()
    val past = Timestamps.subtract(current, FIFTY_MILLIS)
    return past
}

private fun futureTime(): Timestamp {
    val current = Timestamps.now()
    val future = Timestamps.add(current, FIFTY_MILLIS)
    return future
}

/**
 * Protobuf [Duration] of fifty milliseconds.
 *
 * To shift the time into the past or future, we add or subtract a difference of this amount.
 *
 * There are two reasons for choosing fifty milliseconds:
 *
 * 1. The generated code uses `io.spine.base.Time.currentTime()` to get the current timestamp
 *    for comparison. In turn, this method relies on `io.spine.base.Time.SystemTimeProvider`
 *    by default, which has millisecond precision.
 * 2. Adding too small amount of time to make the stamp denote "future" might be unreliable.
 *    As it could catch up `now` by the time `Time.currentTime()` is invoked.
 */
private val FIFTY_MILLIS: Duration = Durations.fromMillis(50)
