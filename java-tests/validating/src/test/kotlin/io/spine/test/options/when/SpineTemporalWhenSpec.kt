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

import io.spine.test.tools.validate.anySpineTemporal
import io.spine.test.tools.validate.anySpineTemporals
import io.spine.test.tools.validate.futureSpineTemporal
import io.spine.test.tools.validate.futureSpineTemporals
import io.spine.test.tools.validate.pastSpineTemporal
import io.spine.test.tools.validate.pastSpineTemporals
import io.spine.time.LocalDateTimes
import io.spine.validation.assertions.assertValidationFails
import io.spine.validation.assertions.assertValidationPasses
import java.time.Instant
import java.time.LocalDateTime.ofInstant
import java.time.ZoneOffset.UTC
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.spine.time.LocalDateTime as SpineTimeLocalDateTime

@DisplayName("If used with Spine `Temporal`, `(when)` constrain should")
internal class SpineTemporalWhenSpec {

    @Nested inner class
    `when given a temporal denoting` {

        @Nested inner class
        `the past` {

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                futureSpineTemporal {
                    value = pastTime()
                }
            }

            @Test
            fun `pass, if restricted to be in past`() = assertValidationPasses {
                pastSpineTemporal {
                    value = pastTime()
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anySpineTemporal {
                    value = pastTime()
                }
            }
        }

        @Nested inner class
        `the future` {

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                pastSpineTemporal {
                    value = futureTime()
                }
            }

            @Test
            fun `pass, if restricted to be in future`() = assertValidationPasses {
                futureSpineTemporal {
                    value = futureTime()
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anySpineTemporal {
                    value = futureTime()
                }
            }
        }
    }

    @Nested inner class
    `when given several times` {

        @Nested inner class
        `denoting only the past` {

            private val severalPastTimes = listOf(pastTime(), pastTime(), pastTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                futureSpineTemporals {
                    value.addAll(severalPastTimes)
                }
            }

            @Test
            fun `pass, if restricted to be in past`() = assertValidationPasses {
                pastSpineTemporals {
                    value.addAll(severalPastTimes)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anySpineTemporals {
                    value.addAll(severalPastTimes)
                }
            }
        }

        @Nested inner class
        `denoting only the future` {

            private val severalFutureTimes = listOf(futureTime(), futureTime(), futureTime())

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                pastSpineTemporals {
                    value.addAll(severalFutureTimes)
                }
            }

            @Test
            fun `pass, if restricted to be in future`() = assertValidationPasses {
                futureSpineTemporals {
                    value.addAll(severalFutureTimes)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anySpineTemporals {
                    value.addAll(severalFutureTimes)
                }
            }
        }

        @Nested inner class
        `with a single past time within the future times` {

            private val severalFutureAndPast = listOf(futureTime(), pastTime(), futureTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                futureSpineTemporals {
                    value.addAll(severalFutureAndPast)
                }
            }

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                pastSpineTemporals {
                    value.addAll(severalFutureAndPast)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anySpineTemporals {
                    value.addAll(severalFutureAndPast)
                }
            }
        }

        @Nested inner class
        `with a single future time within the past times` {

            private val severalPastAndFuture = listOf(pastTime(), futureTime(), pastTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                futureSpineTemporals {
                    value.addAll(severalPastAndFuture)
                }
            }

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                pastSpineTemporals {
                    value.addAll(severalPastAndFuture)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                anySpineTemporals {
                    value.addAll(severalPastAndFuture)
                }
            }
        }
    }
}

private fun pastTime(): SpineTimeLocalDateTime {
    val current = Instant.now() // It is a UTC stamp.
    val past = current.minusMillis(FIFTY_MILLIS)
    return LocalDateTimes.of(ofInstant(past, UTC))
}

private fun futureTime(): SpineTimeLocalDateTime {
    val current = Instant.now() // It is a UTC stamp.
    val past = current.plusMillis(FIFTY_MILLIS)
    return LocalDateTimes.of(ofInstant(past, UTC))
}

/**
 * Fifty milliseconds.
 *
 * To shift the time into the past or future, we add or subtract a difference of this amount.
 *
 * There are two reasons for choosing fifty milliseconds:
 *
 * 1. The generated code uses `io.spine.base.Time.currentTime()` to get the current timestamp
 * for comparison. In turn, this method relies on `io.spine.base.Time.SystemTimeProvider`
 * by default, which has millisecond precision.
 * 2. Adding too small amount of time to make the stamp denote "future" might be unreliable.
 * As it could catch up `now` by the time `Time.currentTime()` is invoked.
 */
private const val FIFTY_MILLIS: Long = 50
