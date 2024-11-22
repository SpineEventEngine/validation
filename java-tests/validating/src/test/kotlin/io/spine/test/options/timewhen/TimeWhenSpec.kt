/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.test.options.timewhen

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// TODO:2024-11-22:yevhenii.nadtochii: Make the difference to be 50 nanoseconds
//  from the expected, so not to introduce dedicated test cases for this.

@DisplayName("`(when)` constrain should")
internal class WhenSpec {

    @Nested inner class
    `when given a timestamp denoting` {

        @Nested
        inner class `the past time` {

            @Test
            fun `throw, if restricted to be in future`() {

            }

            @Test
            fun `pass, if restricted to be in past`() {

            }

            @Test
            fun `pass, if not restricted at all`() {

            }
        }

        @Nested
        inner class `the future time` {

            @Test
            fun `throw, if restricted to be in past`() {

            }

            @Test
            fun `pass, if restricted to be in future`() {

            }

            @Test
            fun `pass, if not restricted at all`() {

            }
        }
    }

    @Nested inner class
    `when given timestamps` {

        @Nested
        inner class `containing only past times` {

            @Test
            fun `throw, if restricted to be in future`() {

            }

            @Test
            fun `pass, if restricted to be in past`() {

            }

            @Test
            fun `pass, if not restricted at all`() {

            }
        }

        @Nested
        inner class `containing only future times` {

            @Test
            fun `throw, if restricted to be in past`() {

            }

            @Test
            fun `pass, if restricted to be in future`() {

            }

            @Test
            fun `pass, if not restricted at all`() {

            }
        }

        @Nested
        inner class `with a single past time within future times` {

            @Test
            fun `throw, if restricted to be in future`() {

            }

            @Test
            fun `throw, if restricted to be in past`() {

            }

            @Test
            fun `pass, if not restricted at all`() {

            }
        }

        @Nested
        inner class `with a single future time within past times` {

            @Test
            fun `throw, if restricted to be in future`() {

            }

            @Test
            fun `throw, if restricted to be in past`() {

            }

            @Test
            fun `pass, if not restricted at all`() {

            }
        }
    }
}
