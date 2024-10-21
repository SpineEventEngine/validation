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

import io.spine.validate.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.protobuf.ByteString.copyFromUtf8;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`(set_once)` constraint should be compiled so that")
class SetOnceConstraintTest {

    @Nested
    @DisplayName("when set, the field is prohibited for overriding")
    class WhenSet {

        @Test
        @DisplayName("of a string value")
        void stringValue() {
            var student = Student.newBuilder()
                    .setId("student-id-1")
                    .build();
            assertThrows(
                    ValidationException.class,
                    () -> student.toBuilder()
                            .setId("student-id-2")
                            .build()
            );
        }

        @Test
        @DisplayName("of a string bytes value")
        void stringBytesValue() {
            var student = Student.newBuilder()
                    .setIdBytes(copyFromUtf8("student-id-1"))
                    .build();
            assertThrows(
                    ValidationException.class,
                    () -> student.toBuilder()
                            .setIdBytes(copyFromUtf8("student-id-2"))
                            .build()
            );
        }

        @Test
        @DisplayName("of a bytes string to string value")
        void stringBytesToStringValue() {
            var student = Student.newBuilder()
                    .setIdBytes(copyFromUtf8("student-id-1"))
                    .build();
            assertThrows(
                    ValidationException.class,
                    () -> student.toBuilder()
                            .setId("student-id-2")
                            .build()
            );
        }

        @Test
        @DisplayName("of a string value to string bytes")
        void stringValueToStringBytes() {
            var student = Student.newBuilder()
                    .setId("student-id-1")
                    .build();
            assertThrows(
                    ValidationException.class,
                    () -> student.toBuilder()
                            .setIdBytes(copyFromUtf8("student-id-2"))
                            .build()
            );
        }
    }
}
