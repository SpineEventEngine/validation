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

package io.spine.validate;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.base.Field;
import io.spine.base.Time;
import io.spine.code.proto.FieldContext;
import io.spine.test.type.PersonName;
import io.spine.test.type.Url;
import io.spine.test.validate.Passport;
import io.spine.test.validate.RequiredMsgFieldValue;
import io.spine.testing.UtilityClassTest;
import io.spine.testing.logging.mute.MuteLogging;
import io.spine.type.TypeName;
import io.spine.validate.diags.ViolationText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.validate.Validate.checkValidChange;
import static io.spine.validate.Validate.violationsOf;
import static io.spine.validate.Validate.violationsOfCustomConstraints;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Validate` utility class should")
class ValidateTest extends UtilityClassTest<Validate> {

    ValidateTest() {
        super(Validate.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(Message.class, Time.currentTime())
              .setDefault(FieldContext.class, FieldContext.empty());
    }

    @Test
    @DisplayName("run custom validation " +
            "and obtain no violations if there are no custom constraints")
    void customValidation() {
        var message = RequiredMsgFieldValue.getDefaultInstance();
        var violations = violationsOf(message);
        var customViolations = violationsOfCustomConstraints(message);
        assertThat(violations).hasSize(1);
        assertThat(customViolations).isEmpty();
    }

    @Test
    @DisplayName("format message from constraint violation")
    void formatMessageFromConstraintViolation() {
        var violation = ConstraintViolation.newBuilder()
                .setMsgFormat("test %s test %s")
                .addParam("1")
                .addParam("2")
                .build();
        var formatted = ViolationText.of(violation).toString();

        assertEquals("test 1 test 2", formatted);
    }

    @MuteLogging
    @Nested
    @DisplayName("test message changes upon `(set_once)` and")
    class SetOnce {

        private static final String BIRTHPLACE = "birthplace";

        @Test
        @DisplayName("throw `ValidationException` if a `(set_once)` field is overridden")
        void reportIllegalChanges() {
            var oldValue = Passport.newBuilder()
                    .setId("AB CDE-123")
                    .setBirthplace("Kyiv")
                    .build();
            var theBuilderToFail = oldValue.toBuilder()
                    .setBirthplace("Kharkiv");

            var newValue = theBuilderToFail.buildPartial();
            checkViolated(oldValue, newValue, BIRTHPLACE);
        }

        @Test
        @DisplayName("ignore ID changes by default")
        void reportIdChanges() {
            var oldValue = Passport.newBuilder()
                    .setId("MT 000100010001")
                    .build();
            var newValue = Passport.newBuilder()
                    .setId("JC 424242424242")
                    .build();
            checkValidChange(oldValue, newValue);
        }

        @Test
        @DisplayName("throw `ValidationException` with several violations")
        void reportManyFields() {
            var oldValue = Passport.newBuilder()
                    .setId("MT 111")
                    .setBirthplace("London")
                    .build();
            var newValue = Passport.newBuilder()
                    .setId("JC 424")
                    .setBirthplace("Edinburgh")
                    .build();
            checkViolated(oldValue, newValue, BIRTHPLACE);
        }

        @Test
        @DisplayName("allow overriding repeated fields")
        void ignoreRepeated() {
            var oldValue = Passport.newBuilder()
                    .setId("PT 123")
                    .addPhoto(Url.newBuilder().setSpec("foo.bar/pic1").build())
                    .build();
            var newValue = oldValue.toBuilder()
                    .addPhoto(Url.newBuilder().setSpec("foo.bar/pic2").build())
                    .build();
            checkValidChange(oldValue, newValue);
        }

        @Test
        @DisplayName("allow overriding if `(set_once) = false`")
        void ignoreNonSetOnce() {
            var id = "JB 007";
            var oldValue = Passport.newBuilder()
                    .setId(id)
                    .build();
            var name = PersonName.newBuilder()
                    .setGivenName("John")
                    .setFamilyName("Doe")
                    .build();
            var newValue = Passport.newBuilder()
                    .setId(id)
                    .setName(name)
                    .build();
            checkValidChange(oldValue, newValue);
        }

        private void checkViolated(Passport oldValue, Passport newValue, String... fields) {
            var exception = assertThrows(ValidationException.class,
                                 () -> checkValidChange(oldValue, newValue));
            var violations = exception.getConstraintViolations();
            assertThat(violations).hasSize(fields.length);

            for (var i = 0; i < fields.length; i++) {
                var violation = violations.get(i);
                var field = fields[i];

                assertThat(violation.getMsgFormat()).contains("(set_once)");

                var expectedTypeName = TypeName.of(newValue).value();
                assertThat(violation.getTypeName()).contains(expectedTypeName);

                assertThat(violation.getFieldPath())
                        .isEqualTo(Field.parse(field).path());
            }
        }
    }
}
