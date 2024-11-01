/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.test.tools.validate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@code (set_once)} constraint.
 *
 * <p>This class contains the most basic tests and aggregates (by inheritance) test cases
 * from {@link SetOnceFieldsTest} and {@link SetOnceIntegerFieldsTest} classes.
 */
@DisplayName("`(set_once)` constraint should be compiled so that")
class SetOnceConstraintTest extends SetOnceFieldsTest {

    @Test
    @DisplayName("not affect fields without the option")
    void notAffectFieldsWithoutOption() {
        assertValidationPasses(() -> SetOnceImplicitFalse.newBuilder()
                .setMessage(Name.newBuilder().setValue("MyName1").build())
                .setMessage(Name.newBuilder().setValue("MyName2").build())
                .setString("string-1")
                .setString("string-2")
                .setDouble(0.25)
                .setDouble(0.75)
                .setFloat(0.25f)
                .setFloat(0.75f)
                .setInt32(5)
                .setInt32(10)
                .setInt64(5)
                .setInt64(10)
                .setUint32(5)
                .setUint32(10)
                .setUint64(5)
                .setUint64(10)
                .setSint32(5)
                .setSint32(10)
                .setSint64(5)
                .setSint64(10)
                .setFixed32(5)
                .setFixed32(10)
                .setFixed64(5)
                .setFixed64(10)
                .setSfixed32(5)
                .setSfixed32(10)
                .setSfixed64(5)
                .setSfixed64(10)
                .build());
    }

    @Test
    @DisplayName("not affect fields with the option set to `false`")
    void notAffectFieldsWithTheOptionSetToFalse() {
        assertValidationPasses(() -> SetOnceExplicitFalse.newBuilder()
                .setMessage(Name.newBuilder().setValue("MyName1").build())
                .setMessage(Name.newBuilder().setValue("MyName2").build())
                .setString("string-1")
                .setString("string-2")
                .setDouble(0.25)
                .setDouble(0.75)
                .setFloat(0.25f)
                .setFloat(0.75f)
                .setInt32(5)
                .setInt32(10)
                .setInt64(5)
                .setInt64(10)
                .setUint32(5)
                .setUint32(10)
                .setUint64(5)
                .setUint64(10)
                .setSint32(5)
                .setSint32(10)
                .setSint64(5)
                .setSint64(10)
                .setFixed32(5)
                .setFixed32(10)
                .setFixed64(5)
                .setFixed64(10)
                .setSfixed32(5)
                .setSfixed32(10)
                .setSfixed64(5)
                .setSfixed64(10)
                .build());
    }

    @Test
    @DisplayName("show the default error message")
    void showDefaultErrorMessage() {

    }

    @Test
    @DisplayName("show the custom error message")
    void showCustomErrorMessage() {

    }
}
