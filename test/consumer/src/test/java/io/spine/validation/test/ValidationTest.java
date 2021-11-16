/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.validation.test;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationError;
import io.spine.validate.ValidationException;
import io.spine.validation.test.money.LocalTime;
import io.spine.validation.test.money.Mru;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Generated validation code should")
class ValidationTest {

    private static final String ALLOW_VALID = "and allow valid values";
    private static final String PROHIBIT_INVALID = "and prohibit invalid values";

    @Nested
    @DisplayName("reflect a rule with a less (`<`) sign and")
    class LessRule {

        @Test
        @DisplayName("throw `ValidationException` if actual value is greater than the threshold")
        void throwOnMore() {
            assertValidationException(Mru.newBuilder()
                    .setKhoums(6));
        }

        @Test
        @DisplayName("throw `ValidationException` if actual value is equal to the threshold")
        void throwOnEdge() {
            assertValidationException(Mru.newBuilder()
                    .setKhoums(5));
        }

        @Test
        @DisplayName("throw no exceptions if actual value is less than the threshold")
        void notThrow() {
            assertNoException(Mru.newBuilder()
                    .setKhoums(4));
        }
    }

    @Nested
    @DisplayName("reflect a rule with a greater or equal (`>=`) sign and")
    class GreaterRule {

        @Test
        @DisplayName("throw `ValidationException` if actual value is less than the threshold")
        void throwOnMore() {
            ConstraintViolation violation = assertValidationException(
                    LocalTime.newBuilder()
                            .setHours(-1)
            );
            assertThat(violation.getMsgFormat())
                    .contains("cannot be negative");
        }

        @Test
        @DisplayName("throw no exceptions if actual value is equal to the threshold")
        void throwOnEdge() {
            assertNoException(LocalTime.newBuilder()
                    .setMinutes(0));
        }

        @Test
        @DisplayName("throw no exceptions if actual value is greater than the threshold")
        void notThrow() {
            assertNoException(LocalTime.newBuilder()
                    .setMinutes(1));
        }
    }

    @Nested
    @DisplayName("reflect a `(required)` rule and")
    class Required {

        @Test
        @DisplayName("check string field")
        void throwForString() {
            Author.Builder builder = Author.newBuilder();
            ConstraintViolation violation = assertValidationException(builder);
            assertThat(violation.getMsgFormat())
                    .contains("Author must have a name");
        }

        @Test
        @DisplayName("check message field")
        void throwForMessage() {
            Book.Builder builder = Book.newBuilder();
            ConstraintViolation violation = assertValidationException(builder);
            assertThat(violation.getMsgFormat())
                    .contains("value must be set");
            assertThat(violation.getFieldPath().getFieldName(0))
                    .isEqualTo("author");
        }

        @Test
        @DisplayName("pass if a value is set")
        void passIfSet() {
            Author.Builder builder = Author.newBuilder()
                    .setName("Evans");
            assertNoException(builder);
        }

        @Test
        @DisplayName("pass if not required")
        void passIfNotRequired() {
            Book.Builder builder = Book.newBuilder()
                    .setAuthor(validAuthor());
            assertNoException(builder);
        }
    }

    @Nested
    @DisplayName("reflect a `(distinct)` rule and")
    class DistinctRule {

        @Test
        @DisplayName("throw `ValidationException` if a list contains duplicate entries")
        void duplicateInList() {
            Snowflake flake = Snowflake.newBuilder()
                    .setEdges(6)
                    .setVertices(6)
                    .build();
            Blizzard.Builder builder = Blizzard.newBuilder()
                    .addSnowflake(flake)
                    .addSnowflake(flake);
            assertValidationException(builder);
        }

        @Test
        @DisplayName("throw `ValidationException` if a map contains duplicate entries")
        void duplicateInMap() {
            Player player = Player
                    .newBuilder()
                    .setShirtName("John Doe")
                    .build();
            Team.Builder builder = Team.newBuilder()
                    .putPlayers(7, player)
                    .putPlayers(10, player);
            assertValidationException(builder);
        }
    }

    @Nested
    @DisplayName("reflect the `(pattern)` rule")
    class PatternRule {

        @Test
        @DisplayName(ALLOW_VALID)
        void pass() {
            Message.Builder player = Player.newBuilder()
                    .setShirtName("Regina Falangee");
            assertNoException(player);
        }

        @Test
        @DisplayName(PROHIBIT_INVALID)
        void fail() {
            Player.Builder player = Player
                    .newBuilder()
                    .setShirtName("R");
            ConstraintViolation violation = assertValidationException(player);
            assertThat(violation.getMsgFormat())
                    .contains("Invalid T-Shirt name");
        }

        @Test
        @DisplayName("and allow partial matches")
        void partial() {
            Book.Builder msg = Book
                    .newBuilder()
                    .setAuthor(validAuthor())
                    .setContent("Something Something Pride Something Something");
            assertNoException(msg);
        }

        @Test
        @DisplayName("and allow ignoring case")
        void caseInsensitive() {
            Book.Builder msg = Book
                    .newBuilder()
                    .setAuthor(validAuthor())
                    .setContent("preJudice");
            assertNoException(msg);
        }

        @Test
        @DisplayName("and still fail even with loose rules")
        void failWithLoose() {
            Book.Builder msg = Book
                    .newBuilder()
                    .setAuthor(validAuthor())
                    .setContent("something else");
            ConstraintViolation violation = assertValidationException(msg);
            assertThat(violation.getFieldPath().getFieldName(0))
                    .isEqualTo("content");
        }
    }

    @Nested
    @DisplayName("reflect the (validate) rule")
    class Validate {

        @Nested
        @DisplayName("on a singular message")
        class Singular {

            @Test
            @DisplayName(ALLOW_VALID)
            void pass() {
                MeteoStatistics.Builder builder = MeteoStatistics
                        .newBuilder()
                        .setAverageDrop(RainDrop.newBuilder().setMassInGrams(1).buildPartial());
                assertNoException(builder);
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            void fail() {
                MeteoStatistics.Builder builder = MeteoStatistics
                        .newBuilder()
                        .setAverageDrop(RainDrop.newBuilder()
                                                .setMassInGrams(-1)
                                                .buildPartial());
                checkInvalid(builder);
            }
        }

        @SuppressWarnings("MethodOnlyUsedFromInnerClass")
        private void checkInvalid(Message.Builder builder) {
            checkInvalid(builder, "message must have valid properties");
        }

        private void checkInvalid(Message.Builder builder, String errorPart) {
            ConstraintViolation violation = assertValidationException(builder);
            assertThat(violation.getMsgFormat())
                    .contains(errorPart);
            assertThat(violation.getViolationList())
                    .hasSize(1);
        }

        @Nested
        @DisplayName("on a repeated message")
        class Repeated {

            @Test
            @DisplayName(ALLOW_VALID)
            void pass() {
                Rain.Builder builder = Rain
                        .newBuilder()
                        .addRainDrop(RainDrop.newBuilder().setMassInGrams(1).buildPartial());
                assertNoException(builder);
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            void fail() {
                Rain.Builder builder = Rain
                        .newBuilder()
                        .addRainDrop(RainDrop.newBuilder().setMassInGrams(-1).buildPartial());
                checkInvalid(builder, "Bad rain drop");
            }
        }
    }

    @Nested
    class IsRequired {

        @Test
        @DisplayName(PROHIBIT_INVALID)
        void fail() {
            Lunch.Builder builder = Lunch.newBuilder();
            assertValidationException(builder);
        }

        @Test
        @DisplayName(ALLOW_VALID)
        void pass() {
            Lunch.Builder builder = Lunch
                    .newBuilder()
                    .setHotSoup("Minestrone");
            assertNoException(builder);
        }
    }

    @Nested
    @DisplayName("reflect the (when) rule")
    class WhenRule {

        @Test
        @DisplayName(PROHIBIT_INVALID)
        void fail() {
            Timestamp when = Timestamps.fromSeconds(4_792_687_200L); // 15 Nov 2121
            Player.Builder player = Player.newBuilder()
                            .setStartedCareerIn(when);
            assertValidationException(player);
        }

        @Test
        @DisplayName(ALLOW_VALID)
        void pass() {
            Timestamp when = Timestamps.fromSeconds(59_086_800L); // 15 Nov 1971
            Player.Builder player = Player.newBuilder()
                    .setStartedCareerIn(when);
            assertNoException(player);
        }
    }

    @CanIgnoreReturnValue
    private static ConstraintViolation assertValidationException(Message.Builder builder) {
        ValidationException exception = assertThrows(ValidationException.class, builder::build);
        ValidationError error = exception.asValidationError();
        assertThat(error.getConstraintViolationList())
             .hasSize(1);
        return error.getConstraintViolation(0);
    }

    private static void assertNoException(Message.Builder builder) {
        Object result = builder.build();
        assertThat(result)
                .isNotNull();
    }

    private static Author validAuthor() {
        return Author
                .newBuilder()
                .setName("Vernon")
                .build();
    }
}
