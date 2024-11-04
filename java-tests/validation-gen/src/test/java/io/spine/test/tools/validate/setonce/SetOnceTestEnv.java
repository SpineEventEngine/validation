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

package io.spine.test.tools.validate.setonce;

import com.google.protobuf.ByteString;
import io.spine.test.tools.validate.Name;
import io.spine.test.tools.validate.YearOfStudy;

/**
 * Test environment for {@code (set_once)} option.
 *
 * <p>This class declares two distinct constants for each Java field type.
 * Tests use them to test whether a field with a non-default value can be
 * overridden by another non-default value.
 *
 * <p>Although Protobuf has 10 integer types, in Java, all 32bit numbers are
 * mapped to {@code int}, and all 64bit numbers are mapped to {@code long}.
 */
class SetOnceTestEnv {

    static final String STUDENT1 = "student-1";
    static final String STUDENT2 = "student-2";

    static final Name JACK = Name.newBuilder()
            .setValue("Jack")
            .build();
    static final Name DONALD = Name.newBuilder()
            .setValue("Donald")
            .build();

    static final double METER_AND_HALF = 1.55;
    static final double METER_AND_EIGHT = 1.88;

    static final float FIFTY_KG = 55.5f;
    static final float EIGHTY_KG = 88.8f;

    static final boolean NO = false;
    static final boolean YES = true;

    static final ByteString FULL_SIGNATURE = ByteString.copyFromUtf8("full");
    static final ByteString SHORT_SIGNATURE = ByteString.copyFromUtf8("short");

    static final YearOfStudy FIRST_YEAR = YearOfStudy.YOS_FIRST;
    static final YearOfStudy THIRD_YEAR = YearOfStudy.YOS_THIRD;

    static final int sixteen = 16;
    static final int sixty = 60;

    static final long eighteen = 18;
    static final long eighty = 80;

    /**
     * Prevents instantiation of this utility class.
     */
    private SetOnceTestEnv() {
    }
}
