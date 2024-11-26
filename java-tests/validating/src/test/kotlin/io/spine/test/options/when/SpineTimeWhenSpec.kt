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

import io.spine.test.tools.validate.alreadyHappenedSpineEvent
import io.spine.test.tools.validate.alreadyHappenedSpineEvents
import io.spine.test.tools.validate.nonTimedSpineEvent
import io.spine.test.tools.validate.nonTimedSpineEvents
import io.spine.test.tools.validate.notYetHappenedSpineEvent
import io.spine.test.tools.validate.notYetHappenedSpineEvents
import io.spine.time.LocalDateTimes
import io.spine.validation.assertions.assertValidationFails
import io.spine.validation.assertions.assertValidationPasses
import java.time.Instant
import java.time.LocalDateTime.ofInstant
import java.time.ZoneOffset.UTC
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.spine.time.LocalDateTime as SpineTimeLocalDateTime

internal class SpineTimeWhenSpec {

    @Nested
    inner class
    `when given a timestamp denoting` {

        @Nested
        inner class `the past time` {

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                notYetHappenedSpineEvent {
                    whenWillHappen = pastTime()
                }
            }

            @Test
            fun `pass, if restricted to be in past`() = assertValidationPasses {
                alreadyHappenedSpineEvent {
                    whenHappened = pastTime()
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedSpineEvent {
                    at = pastTime()
                }
            }
        }

        @Nested
        inner class `the future time` {

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                alreadyHappenedSpineEvent {
                    whenHappened = futureTime()
                }
            }

            @Test
            fun `pass, if restricted to be in future`() = assertValidationPasses {
                notYetHappenedSpineEvent {
                    whenWillHappen = futureTime()
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedSpineEvent {
                    at = futureTime()
                }
            }
        }
    }

    @Nested
    inner class
    `when given several timestamps` {

        @Nested
        inner class `containing only past times` {

            private val severalPastTimes = listOf(pastTime(), pastTime(), pastTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                notYetHappenedSpineEvents {
                    whenWillHappen.addAll(severalPastTimes)
                }
            }

            @Test
            fun `pass, if restricted to be in past`() = assertValidationPasses {
                alreadyHappenedSpineEvents {
                    whenHappened.addAll(severalPastTimes)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedSpineEvents {
                    at.addAll(severalPastTimes)
                }
            }
        }

        @Nested
        inner class `containing only future times` {

            private val severalFutureTimes = listOf(futureTime(), futureTime(), futureTime())

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                alreadyHappenedSpineEvents {
                    whenHappened.addAll(severalFutureTimes)
                }
            }

            @Test
            fun `pass, if restricted to be in future`() = assertValidationPasses {
                notYetHappenedSpineEvents {
                    whenWillHappen.addAll(severalFutureTimes)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedSpineEvents {
                    at.addAll(severalFutureTimes)
                }
            }
        }

        @Nested
        inner class `with a single past time within future times` {

            private val severalFutureAndPast = listOf(futureTime(), pastTime(), futureTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                notYetHappenedSpineEvents {
                    whenWillHappen.addAll(severalFutureAndPast)
                }
            }

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                alreadyHappenedSpineEvents {
                    whenHappened.addAll(severalFutureAndPast)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedSpineEvents {
                    at.addAll(severalFutureAndPast)
                }
            }
        }

        @Nested
        inner class `with a single future time within past times` {

            private val severalPastAndFuture = listOf(pastTime(), futureTime(), pastTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                notYetHappenedSpineEvents {
                    whenWillHappen.addAll(severalPastAndFuture)
                }
            }

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                alreadyHappenedSpineEvents {
                    whenHappened.addAll(severalPastAndFuture)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedSpineEvents {
                    at.addAll(severalPastAndFuture)
                }
            }
        }
    }
}

private fun pastTime(): SpineTimeLocalDateTime {
    val current = Instant.now() // Current UTC
    val past = current.minusMillis(FIFTY_MILLIS)
    val spinePast = LocalDateTimes.of(ofInstant(past, UTC))
    return spinePast
}

private fun futureTime(): SpineTimeLocalDateTime {
    val current = Instant.now() // Current UTC
    val past = current.plusMillis(FIFTY_MILLIS)
    val spineFuture = LocalDateTimes.of(ofInstant(past, UTC))
    return spineFuture
}

// Why not nanos?
// `io.spine.base.Time.currentTime()` is used by the generated code to get the current time,
// which in turn relies on `io.spine.base.Time.SystemTimeProvider` by default.
// `SystemTimeProvider` has millisecond precision.
private const val FIFTY_MILLIS: Long = 50
