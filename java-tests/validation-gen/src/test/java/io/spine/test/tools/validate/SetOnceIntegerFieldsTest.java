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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@code (set_once)} constraint for integer fields.
 *
 * <p>It is a smaller subset of {@link SetOnceFieldsTest} cases that cover only integers.
 *
 * <p>These test cases were extracted to a separate class for convenience. They use their own
 * test fixture ({@link Balance} and are focused only on integers.
 *
 * <p>The class is abstract, so it could be executed as a part of {@link SetOnceConstraintTest}.
 */
abstract class SetOnceIntegerFieldsTest {

    @Nested
    @DisplayName("prohibit overriding non-default int32")
    class ProhibitOverridingNonDefaultInt32 {

        private static final int sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setUSD(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setUSD(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setUSD(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("USD"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value int32")
    class AllowOverridingDefaultAndSameValueInt32 {

        private static final int sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setUSD(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setUSD(sixteen)
                    .setUSD(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("USD"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("USD"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearUSD()
                    .setUSD(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default int64")
    class ProhibitOverridingNonDefaultInt64 {

        private static final long sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setEUR(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setEUR(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setEUR(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("EUR"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value int64")
    class AllowOverridingDefaultAndSameValueInt64 {

        private static final long sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setEUR(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setEUR(sixteen)
                    .setEUR(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("EUR"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("EUR"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearEUR()
                    .setEUR(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default uint32")
    class ProhibitOverridingNonDefaultUInt32 {

        private static final int sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setJPY(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setJPY(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setJPY(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("JPY"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value uint32")
    class AllowOverridingDefaultAndSameValueUInt32 {

        private static final int sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setJPY(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setJPY(sixteen)
                    .setJPY(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("JPY"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("JPY"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearJPY()
                    .setJPY(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default uint64")
    class ProhibitOverridingNonDefaultUInt64 {

        private static final long sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setGBP(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setGBP(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setGBP(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("GBP"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value uint64")
    class AllowOverridingDefaultAndSameValueUInt64 {

        private static final long sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setGBP(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setGBP(sixteen)
                    .setGBP(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("GBP"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("GBP"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearGBP()
                    .setGBP(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default sint32")
    class ProhibitOverridingNonDefaultSInt32 {

        private static final int sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setAUD(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setAUD(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setAUD(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("AUD"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value sint32")
    class AllowOverridingDefaultAndSameValueSInt32 {

        private static final int sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setAUD(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setAUD(sixteen)
                    .setAUD(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("AUD"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("AUD"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearAUD()
                    .setAUD(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default sint64")
    class ProhibitOverridingNonDefaultSInt64 {

        private static final long sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setCAD(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCAD(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setCAD(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CAD"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value sint64")
    class AllowOverridingDefaultAndSameValueSInt64 {

        private static final long sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCAD(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setCAD(sixteen)
                    .setCAD(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CAD"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("CAD"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearCAD()
                    .setCAD(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default fixed32")
    class ProhibitOverridingNonDefaultFixed32 {

        private static final int sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setCHF(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCHF(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setCHF(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CHF"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value fixed32")
    class AllowOverridingDefaultAndSameValueFixed32 {

        private static final int sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCHF(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setCHF(sixteen)
                    .setCHF(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CHF"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("CHF"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearCHF()
                    .setCHF(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default fixed64")
    class ProhibitOverridingNonDefaultFixed64 {

        private static final long sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setCNY(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCNY(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setCNY(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CNY"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value fixed64")
    class AllowOverridingDefaultAndSameValueFixed64 {

        private static final long sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCNY(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setCNY(sixteen)
                    .setCNY(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CNY"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("CNY"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearCNY()
                    .setCNY(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default sfixed32")
    class ProhibitOverridingNonDefaultSFixed32 {

        private static final int sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setPLN(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setPLN(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setPLN(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("PLN"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value sfixed32")
    class AllowOverridingDefaultAndSameValueSFixed32 {

        private static final int sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setPLN(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setPLN(sixteen)
                    .setPLN(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("PLN"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("PLN"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearPLN()
                    .setPLN(sixteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default sfixed64")
    class ProhibitOverridingNonDefaultSFixed64 {

        private static final long sixteen = 16;

        private final Balance currentBalance = Balance.newBuilder()
                .setNZD(60)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setNZD(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setNZD(sixteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("NZD"), sixteen));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value sfixed64")
    class AllowOverridingDefaultAndSameValueSFixed64 {

        private static final long sixteen = 16;

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setNZD(sixteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setNZD(sixteen)
                    .setNZD(sixteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("NZD"), sixteen)
                    .setField(Balance.getDescriptor().findFieldByName("NZD"), sixteen)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance)
                    .mergeFrom(newBalance)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .mergeFrom(newBalance.toByteArray())
                    .mergeFrom(newBalance.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> newBalance.toBuilder()
                    .clearNZD()
                    .setNZD(sixteen)
                    .build());
        }
    }

    static void assertValidationFails(Executable executable) {
        assertThrows(ValidationException.class, executable);
    }

    static void assertValidationPasses(Executable executable) {
        assertDoesNotThrow(executable);
    }
}
