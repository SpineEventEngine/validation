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
import org.junit.jupiter.api.function.Executable;

import static com.google.protobuf.ByteString.copyFromUtf8;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`(set_once)` constraint should be compiled so that")
class SetOnceConstraintTest {

    @Nested
    @DisplayName("when set, prohibit overriding messages")
    class WhenSetProhibitOverridingMessages {

        private final Name donald = Name.newBuilder()
                .setValue("Donald")
                .build();
        private final Student studentJack = Student.newBuilder()
                .setName(Name.newBuilder().setValue("Jack").build())
                .build();
        private final Student studentDonald = Student.newBuilder()
                .setName(donald)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .setName(donald)
                    .build());
        }

        @Test
        @DisplayName("by builder")
        void byBuilder() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .setName(donald.toBuilder())
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("name"), donald)
                    .build());
        }

        @Test // Required additional check in `mergeName()`.
        @DisplayName("by field merge")
        void byFieldMerge() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .mergeName(donald)
                    .build());
        }

        @Test // Required additional check in `mergeName()`.
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .mergeFrom(studentDonald)
                    .build());
        }

        @Test // Requires changes to `mergeFrom(CodedInputStream, ExtensionRegistry)`.
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .mergeFrom(studentDonald.toByteArray())
                    .build());
        }
    }

    @Nested
    @DisplayName("when set, prohibit overriding string")
    class WhenSetProhibitOverridingString {

        private static final String STUDENT2 = "student-2";

        private final Student student1 = Student.newBuilder()
                .setId("student-1")
                .build();
        private final Student student2 = Student.newBuilder()
                .setId(STUDENT2)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> student1.toBuilder()
                    .setId(STUDENT2)
                    .build());
        }

        @Test
        @DisplayName("by bytes")
        void byBytes() {
            assertValidationFails(() -> student1.toBuilder()
                    .setIdBytes(copyFromUtf8(STUDENT2))
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> student1.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("id"), STUDENT2)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> student1.toBuilder()
                    .mergeFrom(student2)
                    .build());
        }

        @Test // Requires changes to `mergeFrom(CodedInputStream, ExtensionRegistry)`.
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> student1.toBuilder()
                    .mergeFrom(student2.toByteArray())
                    .build());
        }
    }

    @Nested
    @DisplayName("when set, prohibit overriding messages")
    class WhenSetProhibitOverridingMessagess {

        @Test
        @DisplayName("of double value")
        void doubleValue() {
            var student = Student.newBuilder()
                    .setHeight(16.8)
                    .build();
            assertValidationFails(() -> student.toBuilder()
                    .setHeight(16.9)
                    .build());
        }

        @Test
        @DisplayName("of float value")
        void floatValue() {
            var student = Student.newBuilder()
                    .setWeight(16.8f)
                    .build();
            assertValidationFails(() -> student.toBuilder()
                    .setWeight(16.9f)
                    .build());
        }

        @Test
        @DisplayName("of int32 value")
        void int32Value() {
            var student = Student.newBuilder()
                    .setAge(18)
                    .build();
            assertValidationFails(() -> student.toBuilder()
                    .setAge(19)
                    .build());
        }

        @Test
        @DisplayName("of int64 value")
        void int64Value() {
            var student = Student.newBuilder()
                    .setSubjects(5)
                    .build();
            assertValidationFails(() -> student.toBuilder()
                    .setSubjects(6)
                    .build());
            assertValidationFails(() -> student.toBuilder()
                    .mergeFrom(Student.newBuilder().setSubjects(6).build())
                    .build());
            assertValidationFails(() -> student.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("subjects"), 6)
                    .build());
        }
    }

    // TODO:2024-10-21:yevhenii.nadtochii: Assert the message.
    private static void assertValidationFails(Executable runnable) {
        assertThrows(ValidationException.class, runnable);
    }
}
