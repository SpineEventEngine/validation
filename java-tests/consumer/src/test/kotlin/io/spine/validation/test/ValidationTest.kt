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

package io.spine.validation.test;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import com.google.protobuf.util.Timestamps;
import io.spine.protobuf.AnyPacker;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationException;
import io.spine.validation.test.money.LocalTime;
import io.spine.validation.test.money.Mru;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Generated validation code should")
class ValidationTest {

    private static final String ALLOW_VALID = "and allow valid values";
    private static final String PROHIBIT_INVALID = "and prohibit invalid values";
    private static final String IGNORE_UNKNOWN_INVALID =
            "and ignore invalid packed values if type is unknown";

    @Nested
    @DisplayName("reflect a rule with a less (`<`) sign and")
    class LessRule {

        @Test
        @DisplayName("throw `ValidationException` if actual value is greater than the threshold")
        void throwOnMore() {
            assertValidationException(Mru.newBuilder().setKhoums(6));
        }

        @Test
        @DisplayName("throw `ValidationException` if actual value is equal to the threshold")
        void throwOnEdge() {
            assertValidationException(Mru.newBuilder().setKhoums(5));
        }

        @Test
        @DisplayName("throw no exceptions if actual value is less than the threshold")
        void notThrow() {
            assertNoException(Mru.newBuilder().setKhoums(4));
        }
    }

    @Nested
    @DisplayName("reflect a rule with a greater or equal (`>=`) sign and")
    class GreaterRule {

        @Test
        @DisplayName("throw `ValidationException` if actual value is less than the threshold")
        void throwOnMore() {
            var violation = assertValidationException(
                    LocalTime.newBuilder()
                            .setHours(-1)
            );
            assertThat(violation.getMsgFormat())
                    .contains("cannot be negative");
        }

        @Test
        @DisplayName("throw no exceptions if actual value is equal to the threshold")
        void notThrowOnEdge() {
            assertNoException(LocalTime.newBuilder().setMinutes(0));
        }

        @Test
        @DisplayName("throw no exceptions if actual value is greater than the threshold")
        void notThrow() {
            assertNoException(LocalTime.newBuilder().setMinutes(1));
        }
    }

    @Nested
    @DisplayName("reflect a `(required)` rule and")
    class Required {

        @Test
        @DisplayName("check string field")
        void throwForString() {
            var builder = Author.newBuilder();
            var violation = assertValidationException(builder);
            assertThat(violation.getMsgFormat())
                    .contains("Author must have a name");
        }

        @Test
        @DisplayName("check message field")
        void throwForMessage() {
            var builder = Book.newBuilder();
            var violation = assertValidationException(builder);
            assertThat(violation.getMsgFormat())
                    .contains("value must be set");
            assertThat(violation.getFieldPath()
                                .getFieldName(0))
                    .isEqualTo("author");
        }

        @Test
        @DisplayName("pass if a value is set")
        void passIfSet() {
            var builder = Author.newBuilder().setName("Evans");
            assertNoException(builder);
        }

        @Test
        @DisplayName("pass if not required")
        void passIfNotRequired() {
            var builder = Book.newBuilder().setAuthor(validAuthor());
            assertNoException(builder);
        }

        @Test
        @DisplayName("throw `ValidationException` if a list contains only default values")
        void empty() {
            var builder = Blizzard.newBuilder()
                    .addSnowflake(Snowflake.getDefaultInstance());
            assertValidationException(builder);
        }

        @Test
        @DisplayName("pass if a list contains all non-default values")
        void nonDefaultList() {
            var builder = Blizzard.newBuilder()
                    .addSnowflake(Snowflake.newBuilder()
                                          .setEdges(3)
                                          .setVertices(3));
            assertNoException(builder);
        }

        @Test
        @DisplayName("throw `ValidationException` if a list contains at least one default value")
        void withDefault() {
            var builder = Blizzard.newBuilder()
                    .addSnowflake(Snowflake.newBuilder()
                                          .setEdges(3)
                                          .setVertices(3))
                    .addSnowflake(Snowflake.getDefaultInstance());
            assertValidationException(builder);
        }
    }

    @Nested
    @DisplayName("reflect a `(distinct)` rule and")
    class DistinctRule {

        @Test
        @DisplayName("throw `ValidationException` if a list contains duplicate entries")
        void duplicateInList() {
            var flake = Snowflake.newBuilder()
                    .setEdges(6)
                    .setVertices(6)
                    .build();
            var builder = Blizzard.newBuilder()
                    .addSnowflake(flake)
                    .addSnowflake(flake);
            assertValidationException(builder);
        }

        @Test
        @DisplayName("throw `ValidationException` if a map contains duplicate entries")
        void duplicateInMap() {
            var player = Player.newBuilder()
                    .setShirtName("John Doe")
                    .build();
            var builder = Team.newBuilder()
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
            var player = Player.newBuilder()
                    .setShirtName("R");
            var violation = assertValidationException(player);
            assertThat(violation.getMsgFormat())
                    .contains("Invalid T-Shirt name");
        }

        @Test
        @DisplayName("and allow partial matches")
        void partial() {
            var msg = Book.newBuilder()
                    .setAuthor(validAuthor())
                    .setContent("Something Something Pride Something Something");
            assertNoException(msg);
        }

        @Test
        @DisplayName("and allow ignoring case")
        void caseInsensitive() {
            var msg = Book.newBuilder()
                    .setAuthor(validAuthor())
                    .setContent("preJudice");
            assertNoException(msg);
        }

        @Test
        @DisplayName("and still fail even with loose rules")
        void failWithLoose() {
            var msg = Book.newBuilder()
                    .setAuthor(validAuthor())
                    .setContent("something else");
            var violation = assertValidationException(msg);
            assertThat(violation.getFieldPath()
                                .getFieldName(0))
                    .isEqualTo("content");
        }

        @Test
        @DisplayName("and handle special characters in the pattern properly")
        void allowDollarSigns() {
            var msg = Team.newBuilder().setName("Sch 04");
            assertNoException(msg);
        }
    }

    @Nested
    @DisplayName("reflect the (validate) rule")
    class Validate {

        @Nested
        @DisplayName("on a singular field")
        class Singular {

            @Test
            @DisplayName(ALLOW_VALID)
            void pass() {
                var builder = meteoStatsInEurope()
                        .setAverageDrop(validRainDrop());
                assertNoException(builder);
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            void fail() {
                var builder = meteoStatsInEurope()
                        .setAverageDrop(invalidRainDrop());
                checkInvalid(builder);
            }
        }

        @Nested
        @DisplayName("on a singular `Any` field")
        class SingularAny {

            @Test
            @DisplayName(ALLOW_VALID)
            void pass() {
                var builder = meteoStatsInEurope()
                        .setAverageDrop(validRainDrop())
                        .setLastEvent(AnyPacker.pack(validRainDrop()));
                assertNoException(builder);
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            void fail() {
                var builder = meteoStatsInEurope()
                        .setAverageDrop(validRainDrop())
                        .setLastEvent(AnyPacker.pack(invalidCloud()));
                checkInvalid(builder);
            }

            @Test
            @DisplayName(IGNORE_UNKNOWN_INVALID)
            void unknownType() {
                var builder = meteoStatsInEurope()
                        .setAverageDrop(validRainDrop())
                        .setLastEvent(AnyPacker.pack(invalidRainDrop()));
                assertNoException(builder);
            }
        }

        @Nested
        @DisplayName("on a repeated field")
        class Repeated {

            @Test
            @DisplayName(ALLOW_VALID)
            void pass() {
                var builder = Rain.newBuilder()
                        .addRainDrop(validRainDrop());
                assertNoException(builder);
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            void fail() {
                var builder = Rain.newBuilder()
                        .addRainDrop(invalidRainDrop());
                checkInvalid(builder, "Bad rain drop");
            }
        }

        @Nested
        @DisplayName("on a repeated `Any` field")
        class RepeatedAny {

            @Test
            @DisplayName(ALLOW_VALID)
            void pass() {
                var packedValid = AnyPacker.pack(validRainDrop());
                var builder = meteoStatsInEurope()
                        .setAverageDrop(validRainDrop())
                        .addPredictedEvent(packedValid)
                        .addPredictedEvent(packedValid);
                assertNoException(builder);
            }

            @Test
            @DisplayName(PROHIBIT_INVALID)
            void fail() {
                var packedValid = AnyPacker.pack(validCloud());
                var packedInvalid = AnyPacker.pack(invalidCloud());
                var builder = meteoStatsInEurope()
                        .setAverageDrop(validRainDrop())
                        .addPredictedEvent(packedValid)
                        .addPredictedEvent(packedInvalid);
                checkInvalid(builder);
            }

            @Test
            @DisplayName(IGNORE_UNKNOWN_INVALID)
            void unknownValue() {
                var packedValid = AnyPacker.pack(validCloud());
                var packedInvalid = AnyPacker.pack(invalidRainDrop());
                var builder = meteoStatsInEurope()
                        .setAverageDrop(validRainDrop())
                        .addPredictedEvent(packedValid)
                        .addPredictedEvent(packedInvalid);
                assertNoException(builder);
            }
        }

        private MeteoStatistics.Builder meteoStatsInEurope() {
            return MeteoStatistics.newBuilder()
                    .putIncludedRegions("EU", someRegion());
        }

        private RainDrop invalidRainDrop() {
            return RainDrop.newBuilder()
                    .setMassInGrams(-1)
                    .buildPartial();
        }

        private RainDrop validRainDrop() {
            return RainDrop.newBuilder()
                    .setMassInGrams(1)
                    .buildPartial();
        }

        private Cloud validCloud() {
            return Cloud.newBuilder()
                    .setCubicMeters(2)
                    .build();
        }

        private Cloud invalidCloud() {
            return Cloud.newBuilder()
                    .setCubicMeters(-2)
                    .buildPartial();
        }

        private Region someRegion() {
            return Region.newBuilder()
                    .setName("Europe")
                    .build();
        }

        private void checkInvalid(Message.Builder builder) {
            checkInvalid(builder, "message must have valid properties");
        }

        private void checkInvalid(Message.Builder builder, String errorPart) {
            var violation = assertValidationException(builder);
            assertThat(violation.getMsgFormat())
                    .contains(errorPart);
            assertThat(violation.getViolationList())
                    .hasSize(1);
        }
    }

    @Nested
    class IsRequired {

        @Test
        @DisplayName(PROHIBIT_INVALID)
        void fail() {
            var builder = Lunch.newBuilder();
            assertValidationException(builder);
        }

        @Test
        @DisplayName(ALLOW_VALID)
        void pass() {
            var builder = Lunch.newBuilder()
                    .setHotSoup("Minestrone");
            assertNoException(builder);
        }
    }

    @Nested
    @DisplayName("reflect the `(when)` rule")
    class WhenRule {

        @Test
        @DisplayName(PROHIBIT_INVALID)
        void fail() {
            var when = Timestamps.fromSeconds(4_792_687_200L); // 15 Nov 2121
            var player = Player.newBuilder()
                    .setStartedCareerIn(when);
            assertValidationException(player);
        }

        @Test
        @DisplayName(ALLOW_VALID)
        void pass() {
            var when = Timestamps.fromSeconds(59_086_800L); // 15 Nov 1971
            var player = Player.newBuilder()
                    .setStartedCareerIn(when);
            assertNoException(player);
        }
    }

    /**
     * Tests the rules which are currently generated by {@code mc-java}.
     *
     * <p>Note: these tests are now disabled since simultaneous application of ProtoData
     * and {@code mc-java} makes impossible to pass the configuration options
     * to the {@code LaunchProtoData} Gradle task. Effectively, inability to make such
     * a configuration makes these tests fail.
     *
     * <p>The tests are disabled until the code generation required by these tests
     * is fully performed by the Validation library itself without any dependencies
     * onto the code generated by {@code mc-java}. Once this is achieved, {@code mc-java} plugin
     * should be turned off for the corresponding test project.
     */
    @Nested
    @Disabled
    @DisplayName("make the entity and signal IDs required")
    class RequiredIdRule {

        @Test
        @DisplayName("allow valid entities")
        void passEntity() {
            var entity = Fancy.newBuilder()
                    .setId(FancyId.newBuilder().setUuid(newUuid()));
            assertNoException(entity);
        }

        @Test
        @DisplayName("not allow invalid entities")
        void failEntity() {
            var entity = Fancy.newBuilder();
            var violation = assertValidationException(entity);
            assertThat(violation.getFieldPath()
                                .getFieldName(0))
                    .isEqualTo("id");
        }

        @Test
        @DisplayName("allow valid events")
        void passEvent() {
            var event = PrefixEventRecognized.newBuilder()
                    .setId("qwerty");
            assertNoException(event);
        }

        @Test
        @DisplayName("not allow invalid events")
        void failEvent() {
            var event = PrefixEventRecognized.newBuilder();
            var violation = assertValidationException(event);
            assertThat(violation.getFieldPath()
                                .getFieldName(0))
                    .isEqualTo("id");
        }

        @Test
        @DisplayName("allow valid commands")
        void passCommand() {
            var cmd = RecognizeSuffixCommand.newBuilder().setId("42");
            assertNoException(cmd);
        }

        @Test
        @DisplayName("not allow invalid commands")
        void failCommand() {
            var cmd = RecognizeSuffixCommand.newBuilder();
            var violation = assertValidationException(cmd);
            assertThat(violation.getFieldPath()
                                .getFieldName(0))
                    .isEqualTo("id");
        }
    }

    @CanIgnoreReturnValue
    private static ConstraintViolation assertValidationException(Message.Builder builder) {
        var exception = assertThrows(ValidationException.class, builder::build);
        var error = exception.asMessage();
        assertThat(error.getConstraintViolationList())
                .hasSize(1);
        return error.getConstraintViolation(0);
    }

    private static void assertNoException(Message.Builder builder) {
        try {
            Object result = builder.build();
            assertThat(result)
                    .isNotNull();
        } catch (ValidationException e) {
            fail("Unexpected constraint violation: " + e.getConstraintViolations(), e);
        }
    }

    private static Author validAuthor() {
        return Author.newBuilder()
                .setName("Vernon")
                .build();
    }
}
