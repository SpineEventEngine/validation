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
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationError;
import io.spine.validate.ValidationException;
import io.spine.validation.test.money.LocalTime;
import io.spine.validation.test.money.Mru;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.function.Supplier;

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
            assertValidationException(() -> Mru.newBuilder()
                    .setKhoums(6)
                    .build());
        }

        @Test
        @DisplayName("throw `ValidationException` if actual value is equal to the threshold")
        void throwOnEdge() {
            assertValidationException(() -> Mru.newBuilder()
                    .setKhoums(5)
                    .build());
        }

        @Test
        @DisplayName("throw no exceptions if actual value is less than the threshold")
        void notThrow() {
            noException(() -> Mru.newBuilder()
                    .setKhoums(4)
                    .build());
        }
    }

    @Nested
    @DisplayName("reflect a rule with a greater or equal (`>=`) sign and")
    class GreaterRule {

        @Test
        @DisplayName("throw `ValidationException` if actual value is less than the threshold")
        void throwOnMore() {
            assertValidationException(() -> LocalTime.newBuilder()
                    .setMinutes(-1)
                    .build());
        }

        @Test
        @DisplayName("throw no exceptions if actual value is equal to the threshold")
        void throwOnEdge() {
            noException(() -> LocalTime.newBuilder()
                    .setMinutes(0)
                    .build());
        }

        @Test
        @DisplayName("throw no exceptions if actual value is greater than the threshold")
        void notThrow() {
            noException(() -> LocalTime.newBuilder()
                    .setMinutes(1)
                    .build());
        }
    }

    @Nested
    @DisplayName("reflect a `(distinct)` rule and")
    class DistinctFeature {

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
            assertValidationException(builder::build);
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
            assertValidationException(builder::build);
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
            noException(player::build);
        }

        @Test
        @DisplayName(PROHIBIT_INVALID)
        void fail() {
            Player.Builder player = Player
                    .newBuilder()
                    .setShirtName("R");
            assertValidationException(player::build);
        }

        @Test
        @DisplayName("and allow partial matches")
        void partial() {
            Book.Builder msg = Book
                    .newBuilder()
                    .setContent("Something Something Pride Something Something");
            noException(msg::build);
        }

        @Test
        @DisplayName("and allow to ignore case")
        void caseInsensitive() {
            Book.Builder msg = Book
                    .newBuilder()
                    .setContent("preJudice");
            noException(msg::build);
        }

        @Test
        @DisplayName("and still fail even with loose rules")
        void failWithLoose() {
            Book.Builder msg = Book
                    .newBuilder()
                    .setContent("something else");
            assertValidationException(msg::build);
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
                noException(builder::build);
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            void fail() {
                MeteoStatistics.Builder builder = MeteoStatistics
                        .newBuilder()
                        .setAverageDrop(RainDrop.newBuilder().setMassInGrams(-1).buildPartial());
                checkInvalid(builder);
            }
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
                noException(builder::build);
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            void fail() {
                Rain.Builder builder = Rain
                        .newBuilder()
                        .addRainDrop(RainDrop.newBuilder().setMassInGrams(-1).buildPartial());
                checkInvalid(builder);
            }
        }

        @SuppressWarnings("MethodOnlyUsedFromInnerClass")
        private void checkInvalid(Message.Builder builder) {
            ConstraintViolation violation = assertValidationException(builder::build);
            assertThat(violation.getMsgFormat())
                    .contains("must be valid");
            assertThat(violation.getViolationList())
                    .hasSize(1);
        }
    }

    @CanIgnoreReturnValue
    private static ConstraintViolation assertValidationException(Executable fun) {
        ValidationException exception = assertThrows(ValidationException.class, fun);
        ValidationError error = exception.asValidationError();
        assertThat(error.getConstraintViolationList())
             .hasSize(1);
        return error.getConstraintViolation(0);
    }

    private static void noException(Supplier<?> fun) {
        Object result = fun.get();
        assertThat(result)
                .isNotNull();
    }
}
