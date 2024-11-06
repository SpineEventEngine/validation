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

package io.spine.test.setonce

import com.google.protobuf.ByteString
import io.spine.test.tools.validate.Name
import io.spine.test.tools.validate.YearOfStudy
import io.spine.test.tools.validate.name

/**
 * Test environment for `(set_once)` option.
 *
 * This object declares two distinct constants for each Java field type.
 * Tests use them to verify whether a field with a non-default value can be
 * overridden by another non-default value.
 *
 * Although Protobuf has 10 integer types, in Java, all 32-bit numbers are
 * mapped to `int`, and all 64bit numbers are mapped to `long`.
 */
internal object TestEnv {

    const val STUDENT1: String = "student-1"
    const val STUDENT2: String = "student-2"

    val JACK: Name = name { value = "Jack" }
    val DONALD: Name = name { value = "Donald" }

    const val METER_AND_HALF: Double = 1.55
    const val METER_AND_EIGHT: Double = 1.88

    const val FIFTY_KG: Float = 55.5f
    const val EIGHTY_KG: Float = 88.8f

    const val NO: Boolean = false
    const val YES: Boolean = true

    val FULL_SIGNATURE: ByteString = ByteString.copyFromUtf8("full")
    val SHORT_SIGNATURE: ByteString = ByteString.copyFromUtf8("short")

    val FIRST_YEAR: YearOfStudy = YearOfStudy.YOS_FIRST
    val THIRD_YEAR: YearOfStudy = YearOfStudy.YOS_THIRD

    const val SIXTEEN: Int = 16
    const val SIXTY: Int = 60

    const val EIGHTEEN: Long = 18
    const val EIGHTY: Long = 80
}
