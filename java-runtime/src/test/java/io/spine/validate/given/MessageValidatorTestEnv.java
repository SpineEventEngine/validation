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

package io.spine.validate.given;

import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import io.spine.validate.ConstraintViolation;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;

public class MessageValidatorTestEnv {

    public static final String VALUE = "value";
    public static final String EMAIL = "email";
    public static final String ENCLOSED_FIELD_NAME = "enclosed";

    /** Prevent instantiation of this test environment. */
    private MessageValidatorTestEnv() {
    }

    public static void assertFieldPathIs(ConstraintViolation violation, String... expectedFields) {
        var path = violation.getFieldPath();
        var actualFields = path.getFieldNameList();
        assertThat(actualFields)
                .containsExactlyElementsIn(expectedFields);
    }

    public static StringValue newStringValue() {
        return StringValue.of(newUuid());
    }

    public static ByteString newByteString() {
        var bytes = ByteString.copyFromUtf8(newUuid());
        return bytes;
    }
}
