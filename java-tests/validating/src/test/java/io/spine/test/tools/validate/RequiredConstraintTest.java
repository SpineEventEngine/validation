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

import com.google.common.truth.Correspondence;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.test.tools.validate.command.CreateProject;
import io.spine.test.tools.validate.entity.Project;
import io.spine.test.tools.validate.entity.Task;
import io.spine.test.tools.validate.event.ProjectCreated;
import io.spine.test.tools.validate.rejection.TestRejections.CannotCreateProject;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.Diags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Correspondence.transforming;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.test.tools.validate.IsValid.assertInvalid;
import static io.spine.test.tools.validate.IsValid.assertValid;
import static io.spine.test.tools.validate.UltimateChoice.CHICKEN;
import static io.spine.test.tools.validate.UltimateChoice.FISH;
import static io.spine.test.tools.validate.UltimateChoice.VEGETABLE;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("`(required)` constraint should be compiled so that")
class RequiredConstraintTest {

    private static final Correspondence<ConstraintViolation, String> fieldName = transforming(
            violation -> violation.getFieldPath().getFieldName(0),
            "field name"
    );

    @Test
    @DisplayName("a number field can have any value")
    void ignoreNumbers() {
        var singulars = Singulars.newBuilder()
                .setOneOrMoreBytes(ByteString.copyFromUtf8("qwerty"))
                .setNotVegetable(CHICKEN)
                .setNotEmptyString("   ")
                .setNotDefault(Enclosed.newBuilder().setValue("  "));
        assertValid(singulars);
    }

    @Nested
    @DisplayName("a `string` field")
    class StringField {

        private static final String FIELD = "not_empty_string";

        @Test
        @DisplayName("cannot be empty")
        void empty() {
            var singulars = Singulars.newBuilder();
            checkViolation(singulars, FIELD);
        }

        @Test
        @DisplayName("must have a non-empty value")
        void acceptNonEmptyString() {
            var singulars = Singulars.newBuilder()
                    .setNotEmptyString(" ")
                    .setNotVegetable(FISH)
                    .setNotDefault(Enclosed.newBuilder().setValue("  "))
                    .setOneOrMoreBytes(ByteString.copyFromUtf8("foobar"));
            assertValid(singulars);
        }
    }

    @Nested
    @DisplayName("a `bytes` field")
    class BytesField {

        private static final String FIELD = "one_or_more_bytes";

        @Test
        @DisplayName("cannot be empty")
        void empty() {
            var singulars = Singulars.newBuilder();
            checkViolation(singulars, FIELD);
        }

        @Test
        @DisplayName("must have bytes, allowing all zeros")
        void nonEmpty() {
            var nonZeros = Singulars.newBuilder()
                    .setNotDefault(Enclosed.newBuilder().setValue("non-default enclosed"))
                    .setNotVegetable(CHICKEN)
                    .setOneOrMoreBytes(ByteString.copyFromUtf8("non-empty"))
                    .setNotEmptyString("str");
            assertValid(nonZeros);

            byte[] zeros = {0};
            var withZeroes = Singulars.newBuilder()
                    .setOneOrMoreBytes(ByteString.copyFrom(zeros))
                    .setNotVegetable(CHICKEN)
                    .setNotDefault(Enclosed.newBuilder().setValue("   "))
                    .setNotEmptyString("  ");
            assertValid(withZeroes);
        }
    }

    @Nested
    @DisplayName("an enum field")
    class EnumField {

        private static final String FIELD = "not_vegetable";

        @Test
        @DisplayName("cannot have a zero-index enum item value")
        void zeroValue() {
            var singulars = Singulars
                    .newBuilder()
                    .setNotVegetable(VEGETABLE);
            checkViolation(singulars, FIELD);
        }

        @Test
        @DisplayName("must have a non-zero index item value")
        void acceptNonDefaultEnum() {
            var singulars = Singulars
                    .newBuilder()
                    .setOneOrMoreBytes(ByteString.copyFrom(new byte[]{0}))
                    .setNotDefault(Enclosed.newBuilder().setValue("baz"))
                    .setNotVegetable(CHICKEN)
                    .setNotEmptyString("not empty");
            assertValid(singulars);
        }
    }

    @Nested
    @DisplayName("a message field")
    class MessageField {

        protected static final String FIELD = "not_default";

        @Test
        @DisplayName("cannot have a default message value")
        void defaultValue() {
            var singulars = Singulars.newBuilder();
            checkViolation(singulars, FIELD);
        }

        @Test
        @DisplayName("must have a not-default message value")
        void nonDefaultMessage() {
            var singulars = Singulars.newBuilder()
                    .setNotVegetable(CHICKEN)
                    .setOneOrMoreBytes(ByteString.copyFromUtf8("lalala"))
                    .setNotDefault(Enclosed.newBuilder().setValue(newUuid()))
                    .setNotEmptyString(" ");
            assertValid(singulars);
        }

        @Test
        @DisplayName("cannot be of type `google.protobuf.Empty`")
        void notAllowEmptyRequired() {
            final var fieldName = "impossible";

            var unset = AlwaysInvalid.newBuilder();
            checkViolation(unset, fieldName);

            var set = AlwaysInvalid
                    .newBuilder()
                    .setImpossible(Empty.getDefaultInstance());
            checkViolation(set, fieldName);
        }
    }

    @Test
    @DisplayName("all violations on a single message are collected")
    void collectManyViolations() {
        var instance = Singulars.getDefaultInstance();
        var error = instance.validate();
        assertThat(error)
                .isPresent();
        assertThat(error.get().getConstraintViolationList())
                .hasSize(4);
    }

    @Nested
    @DisplayName("a repeated number field")
    class RepeatedNumberField {

        protected static final String FIELD = "not_empty_list_of_longs";

        @Test
        @DisplayName("cannot be empty")
        void emptyRepeatedInt() {
            var instance = Collections.newBuilder();
            checkViolation(instance, FIELD);
        }

        @Test
        @DisplayName("can have any items, including zero")
        void repeatedInt() {
            var instance = Collections
                    .newBuilder()
                    .addNotEmptyListOfLongs(123456789L)
                    .putContainsANonEmptyStringValue("222", "111")
                    .addAtLeastOnePieceOfMeat(CHICKEN)
                    .putNotEmptyMapOfInts(42, 42);
            assertValid(instance);
        }
    }

    @Nested
    @DisplayName("a map field with number values")
    class MapNumberField {

        private static final String FIELD = "not_empty_map_of_ints";

        @Test
        @DisplayName("cannot be empty")
        void empty() {
            var instance = Collections.newBuilder();
            checkViolation(instance, FIELD);
        }

        @Test
        @DisplayName("can have entries with any values, including zero")
        void mapOfInts() {
            var instance = Collections
                    .newBuilder()
                    .putNotEmptyMapOfInts(0, 42)
                    .putContainsANonEmptyStringValue("  ", "qwertyuiop")
                    .addAtLeastOnePieceOfMeat(FISH)
                    .addNotEmptyListOfLongs(981L);
            assertValid(instance);
        }
    }

    @Nested
    @DisplayName("a map field with string values")
    class MapStringField {

        private static final String FIELD = "contains_a_non_empty_string_value";

        @Test
        @DisplayName("cannot be empty")
        void empty() {
            var instance = Collections.newBuilder();
            checkViolation(instance, FIELD, Diags.Required.collectionErrorMsg);
        }

        @Test
        @DisplayName("cannot have an empty value entry")
        void nonEmptyValue() {
            var empty = Collections.newBuilder()
                    .putContainsANonEmptyStringValue("", "");
            assertInvalid(empty);

            var nonEmpty = Collections.newBuilder()
                    .putContainsANonEmptyStringValue("bar", "foo")
                    .putContainsANonEmptyStringValue("foo", "bar")
                    .putNotEmptyMapOfInts(111, 314)
                    .addAtLeastOnePieceOfMeat(FISH)
                    .addNotEmptyListOfLongs(42L);
            assertValid(nonEmpty);
        }

        @Test
        @DisplayName("must have at least one non-empty entry")
        void mapOfStrings() {
            var instance = Collections.newBuilder()
                    .addNotEmptyListOfLongs(42L)
                    .putContainsANonEmptyStringValue("", " ")
                    .putNotEmptyMapOfInts(0, 0)
                    .addAtLeastOnePieceOfMeat(CHICKEN);
            assertValid(instance);
        }
    }

    @Nested
    @DisplayName("a repeated enum field")
    class RepeatedEnumField {

        private static final String FIELD = "at_least_one_piece_of_meat";

        @Test
        @DisplayName("cannot be empty")
        void emptyRepeatedEnum() {
            var instance = Collections.newBuilder();
            checkViolation(instance, FIELD, "must not be empty");
        }

        @Test // https://github.com/SpineEventEngine/mc-java/issues/119
        @Disabled("Until we finalize the behavior of the `required` constraint on repeated enums")
        @DisplayName("cannot have all items with zero-index enum item value")
        void repeatedDefaultEnum() {
            var allZero = Collections.newBuilder()
                    .putNotEmptyMapOfInts(42, 314)
                    .addAtLeastOnePieceOfMeat(VEGETABLE)
                    .addAtLeastOnePieceOfMeat(VEGETABLE)
                    .putContainsANonEmptyStringValue("  ", "   ")
                    .addNotEmptyListOfLongs(42L);
            checkViolation(allZero, FIELD, "cannot contain default values");
        }

        @Test // https://github.com/SpineEventEngine/mc-java/issues/119
        @Disabled("Until we finalize the behavior of the `required` constraint on repeated enums")
        @DisplayName("must not have event one value with non-zero enum item value")
        void repeatedEnum() {
            var instance = Collections.newBuilder()
                    .putContainsANonEmptyStringValue("111", "222")
                    .addNotEmptyListOfLongs(0L)
                    .putNotEmptyMapOfInts(0, 0)
                    .addAtLeastOnePieceOfMeat(FISH)
                    .addAtLeastOnePieceOfMeat(CHICKEN)
                    .addAtLeastOnePieceOfMeat(VEGETABLE);
            checkViolation(instance, FIELD, "default values");
        }
    }

    @Nested
    @DisplayName("the first field in a message which is")
    class FirstFieldCheck {

        @Nested
        @DisplayName("a command")
        class InCommand {

            @Test
            @DisplayName("cannot be empty")
            void notSet() {
                var msg = CreateProject.newBuilder();
                checkViolation(msg, "id");
            }

            @Test
            @DisplayName("must have a non-empty value")
            void set() {
                var msg = CreateProject.newBuilder()
                        .setId(newUuid());
                assertValid(msg);
            }
        }

        @Nested
        @DisplayName("an event")
        class InEvent {

            @Test
            @DisplayName("cannot be empty")
            void notSet() {
                var msg = ProjectCreated.newBuilder();
                checkViolation(msg, "id");
            }
        }

        @Nested
        @DisplayName("a rejection")
        class InRejection {

            @Test
            @DisplayName("cannot be empty")
            void notSet() {
                var msg = CannotCreateProject.newBuilder();
                checkViolation(msg, "id");
            }
        }

        @Nested
        @DisplayName("an entity state")
        class InEntityState {

            @Test
            @DisplayName("cannot be empty")
            void notSet() {
                var msg = Project.newBuilder();
                checkViolation(msg, "id");
            }

            @Test
            @DisplayName("must have a non-empty value")
            void set() {
                var msg = Project.newBuilder()
                        .setId(newUuid());
                assertValid(msg);
            }

            @Test
            @DisplayName("allowing to omit, if set as not `required` explicitly")
            void notRequired() {
                var msg = Task.newBuilder();
                assertValid(msg);
            }
        }
    }

    private static void checkViolation(Message.Builder message, String field) {
        checkViolation(message, field, "must be set");
    }

    private static void checkViolation(Message.Builder message,
                                       String field,
                                       String errorMessagePart) {
        var violations = assertInvalid(message);
        var assertViolations = assertThat(violations);
        assertViolations
                .comparingElementsUsing(fieldName)
                .contains(field);
        var violation = violationAtField(violations, field);
        assertThat(violation.getMsgFormat())
                .contains(errorMessagePart);
    }

    private static ConstraintViolation
    violationAtField(List<ConstraintViolation> violations, String fieldName) {
        return violations
                .stream()
                .filter(violation -> violation.getFieldPath()
                                              .getFieldName(0)
                                              .equals(fieldName))
                .findFirst()
                .orElseGet(() -> fail(format(
                        "No violation for field `%s`. Violations: %s", fieldName, violations
                )));
    }
}
