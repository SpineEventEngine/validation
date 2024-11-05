/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.test.tools.validate.setonce;

import io.spine.test.tools.validate.Balance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.test.tools.validate.setonce.SetOnceAssertions.assertValidationFails;
import static io.spine.test.tools.validate.setonce.SetOnceAssertions.assertValidationPasses;
import static io.spine.test.tools.validate.setonce.SetOnceTestEnv.eighteen;
import static io.spine.test.tools.validate.setonce.SetOnceTestEnv.eighty;
import static io.spine.test.tools.validate.setonce.SetOnceTestEnv.sixteen;
import static io.spine.test.tools.validate.setonce.SetOnceTestEnv.sixty;

/**
 * Tests {@code (set_once)} constraint for integer fields.
 */
@DisplayName("`(set_once)` constraint should")
class SetOnceIntegerFieldsTest {

    @Nested
    @DisplayName("prohibit overriding non-default `int32`")
    class ProhibitOverridingNonDefaultInt32 {

        private final Balance currentBalance = Balance.newBuilder()
                .setUSD(sixty)
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
    @DisplayName("allow overriding default and same-value `int32`")
    class AllowOverridingDefaultAndSameValueInt32 {

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
    @DisplayName("prohibit overriding non-default `int64`")
    class ProhibitOverridingNonDefaultInt64 {

        private final Balance currentBalance = Balance.newBuilder()
                .setEUR(eighty)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setEUR(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setEUR(eighteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("EUR"), eighteen));
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
    @DisplayName("allow overriding default and same-value `int64`")
    class AllowOverridingDefaultAndSameValueInt64 {

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setEUR(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setEUR(eighteen)
                    .setEUR(eighteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("EUR"), eighteen)
                    .setField(Balance.getDescriptor().findFieldByName("EUR"), eighteen)
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
                    .setEUR(eighteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default `uint32`")
    class ProhibitOverridingNonDefaultUInt32 {

        private final Balance currentBalance = Balance.newBuilder()
                .setJPY(sixty)
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
    @DisplayName("allow overriding default and same-value `uint32`")
    class AllowOverridingDefaultAndSameValueUInt32 {

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
    @DisplayName("prohibit overriding non-default `uint64`")
    class ProhibitOverridingNonDefaultUInt64 {

        private final Balance currentBalance = Balance.newBuilder()
                .setGBP(eighty)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setGBP(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setGBP(eighteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("GBP"), eighteen));
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
    @DisplayName("allow overriding default and same-value `uint64`")
    class AllowOverridingDefaultAndSameValueUInt64 {

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setGBP(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setGBP(eighteen)
                    .setGBP(eighteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("GBP"), eighteen)
                    .setField(Balance.getDescriptor().findFieldByName("GBP"), eighteen)
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
                    .setGBP(eighteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default `sint32`")
    class ProhibitOverridingNonDefaultSInt32 {

        private final Balance currentBalance = Balance.newBuilder()
                .setAUD(sixty)
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
    @DisplayName("allow overriding default and same-value `sint32`")
    class AllowOverridingDefaultAndSameValueSInt32 {

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
    @DisplayName("prohibit overriding non-default `sint64`")
    class ProhibitOverridingNonDefaultSInt64 {

        private final Balance currentBalance = Balance.newBuilder()
                .setCAD(eighty)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCAD(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setCAD(eighteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CAD"), eighteen));
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
    @DisplayName("allow overriding default and same-value `sint64`")
    class AllowOverridingDefaultAndSameValueSInt64 {

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCAD(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setCAD(eighteen)
                    .setCAD(eighteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CAD"), eighteen)
                    .setField(Balance.getDescriptor().findFieldByName("CAD"), eighteen)
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
                    .setCAD(eighteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default `fixed32`")
    class ProhibitOverridingNonDefaultFixed32 {

        private final Balance currentBalance = Balance.newBuilder()
                .setCHF(sixty)
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
    @DisplayName("allow overriding default and same-value `fixed32`")
    class AllowOverridingDefaultAndSameValueFixed32 {

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
    @DisplayName("prohibit overriding non-default `fixed64`")
    class ProhibitOverridingNonDefaultFixed64 {

        private final Balance currentBalance = Balance.newBuilder()
                .setCNY(eighty)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCNY(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setCNY(eighteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CNY"), eighteen));
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
    @DisplayName("allow overriding default and same-value `fixed64`")
    class AllowOverridingDefaultAndSameValueFixed64 {

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setCNY(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setCNY(eighteen)
                    .setCNY(eighteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("CNY"), eighteen)
                    .setField(Balance.getDescriptor().findFieldByName("CNY"), eighteen)
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
                    .setCNY(eighteen)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default `sfixed32`")
    class ProhibitOverridingNonDefaultSFixed32 {

        private final Balance currentBalance = Balance.newBuilder()
                .setPLN(sixty)
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
    @DisplayName("allow overriding default and same-value `sfixed32`")
    class AllowOverridingDefaultAndSameValueSFixed32 {

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
    @DisplayName("prohibit overriding non-default `sfixed64`")
    class ProhibitOverridingNonDefaultSFixed64 {

        private final Balance currentBalance = Balance.newBuilder()
                .setNZD(eighty)
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setNZD(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setNZD(eighteen));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> currentBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("NZD"), eighteen));
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
    @DisplayName("allow overriding default and same-value `sfixed64`")
    class AllowOverridingDefaultAndSameValueSFixed64 {

        private final Balance unknownBalance = Balance.newBuilder()
                .build();
        private final Balance newBalance = Balance.newBuilder()
                .setNZD(eighteen)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setNZD(eighteen)
                    .setNZD(eighteen)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownBalance.toBuilder()
                    .setField(Balance.getDescriptor().findFieldByName("NZD"), eighteen)
                    .setField(Balance.getDescriptor().findFieldByName("NZD"), eighteen)
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
                    .setNZD(eighteen)
                    .build());
        }
    }
}
