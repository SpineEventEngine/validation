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
import io.spine.test.tools.validate.alreadyHappenedProtoEvent
import io.spine.test.tools.validate.alreadyHappenedProtoEvents
import io.spine.test.tools.validate.nonTimedEvent
import io.spine.test.tools.validate.nonTimedEvents
import io.spine.test.tools.validate.notYetHappenedProtoEvent
import io.spine.test.tools.validate.notYetHappenedProtoEvents
import io.spine.validation.assertions.assertValidationFails
import io.spine.validation.assertions.assertValidationPasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("If used with Protobuf timestamp, `(when)` constrain should")
internal class ProtoTimeWhenSpec {

    @Nested
    inner class
    `when given a timestamp denoting` {

        @Nested
        inner class `the past time` {

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                notYetHappenedProtoEvent {
                    whenWillHappen = pastTime()
                }
            }

            @Test
            fun `pass, if restricted to be in past`() = assertValidationPasses {
                alreadyHappenedProtoEvent {
                    whenHappened = pastTime()
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedEvent {
                    at = pastTime()
                }
            }
        }

        @Nested
        inner class `the future time` {

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                alreadyHappenedProtoEvent {
                    whenHappened = futureTime()
                }
            }

            @Test
            fun `pass, if restricted to be in future`() = assertValidationPasses {
                notYetHappenedProtoEvent {
                    whenWillHappen = futureTime()
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedEvent {
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
                notYetHappenedProtoEvents {
                    whenWillHappen.addAll(severalPastTimes)
                }
            }

            @Test
            fun `pass, if restricted to be in past`() = assertValidationPasses {
                alreadyHappenedProtoEvents {
                    whenHappened.addAll(severalPastTimes)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedEvents {
                    at.addAll(severalPastTimes)
                }
            }
        }

        @Nested
        inner class `containing only future times` {

            private val severalFutureTimes = listOf(futureTime(), futureTime(), futureTime())

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                alreadyHappenedProtoEvents {
                    whenHappened.addAll(severalFutureTimes)
                }
            }

            @Test
            fun `pass, if restricted to be in future`() = assertValidationPasses {
                notYetHappenedProtoEvents {
                    whenWillHappen.addAll(severalFutureTimes)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedEvents {
                    at.addAll(severalFutureTimes)
                }
            }
        }

        @Nested
        inner class `with a single past time within future times` {

            private val severalFutureAndPast = listOf(futureTime(), pastTime(), futureTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                notYetHappenedProtoEvents {
                    whenWillHappen.addAll(severalFutureAndPast)
                }
            }

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                alreadyHappenedProtoEvents {
                    whenHappened.addAll(severalFutureAndPast)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedEvents {
                    at.addAll(severalFutureAndPast)
                }
            }
        }

        @Nested
        inner class `with a single future time within past times` {

            private val severalPastAndFuture = listOf(pastTime(), futureTime(), pastTime())

            @Test
            fun `throw, if restricted to be in future`() = assertValidationFails {
                notYetHappenedProtoEvents {
                    whenWillHappen.addAll(severalPastAndFuture)
                }
            }

            @Test
            fun `throw, if restricted to be in past`() = assertValidationFails {
                alreadyHappenedProtoEvents {
                    whenHappened.addAll(severalPastAndFuture)
                }
            }

            @Test
            fun `pass, if not restricted at all`() = assertValidationPasses {
                nonTimedEvents {
                    at.addAll(severalPastAndFuture)
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

// By default, `io.spine.base.Time.currentTime()` uses `io.spine.base.Time.SystemTimeProvider`,
// which has millisecond precision.
private val FIFTY_MILLIS: Duration = Durations.fromMillis(50)
