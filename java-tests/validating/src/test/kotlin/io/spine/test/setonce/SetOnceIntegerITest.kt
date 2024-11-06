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

package io.spine.test.setonce

import io.spine.test.setonce.TestEnv.EIGHTEEN
import io.spine.test.setonce.TestEnv.EIGHTY
import io.spine.test.setonce.TestEnv.SIXTEEN
import io.spine.test.setonce.TestEnv.SIXTY
import io.spine.test.tools.validate.Balance
import io.spine.test.tools.validate.balance
import io.spine.validation.assertions.assertValidationFails
import io.spine.validation.assertions.assertValidationPasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests `(set_once)` constraint for integer fields.
 */
@DisplayName("`(set_once)` constraint should")
internal class SetOnceIntegerITest {

    @Nested inner class
    `prohibit overriding non-default 'int32'` {

        private val currentBalance = balance { uSD = SIXTY }
        private val newBalance = balance { uSD = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setUSD(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("USD"), SIXTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'int32'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { uSD = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setUSD(SIXTEEN)
                .setUSD(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("USD"), SIXTEEN)
                .setField(Balance.getDescriptor().findFieldByName("USD"), SIXTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearUSD()
                .setUSD(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'int64'` {

        private val currentBalance = balance { eUR = EIGHTY }
        private val newBalance = balance { eUR = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setEUR(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("EUR"), EIGHTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'int64'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { eUR = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setEUR(EIGHTEEN)
                .setEUR(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("EUR"), EIGHTEEN)
                .setField(Balance.getDescriptor().findFieldByName("EUR"), EIGHTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearEUR()
                .setEUR(EIGHTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'uint32'` {

        private val currentBalance = balance { jPY = SIXTY }
        private val newBalance = balance { jPY = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setJPY(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("JPY"), SIXTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'uint32'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { jPY = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setJPY(SIXTEEN)
                .setJPY(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("JPY"), SIXTEEN)
                .setField(Balance.getDescriptor().findFieldByName("JPY"), SIXTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearJPY()
                .setJPY(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'uint64'` {

        private val currentBalance = balance { gBP = EIGHTY }
        private val newBalance = balance { gBP = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setGBP(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("GBP"), EIGHTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'uint64'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { gBP = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setGBP(EIGHTEEN)
                .setGBP(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("GBP"), EIGHTEEN)
                .setField(Balance.getDescriptor().findFieldByName("GBP"), EIGHTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearGBP()
                .setGBP(EIGHTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sint32'` {

        private val currentBalance = balance { aUD = SIXTY }
        private val newBalance = balance { aUD = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setAUD(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("AUD"), SIXTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'sint32'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { aUD = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setAUD(SIXTEEN)
                .setAUD(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("AUD"), SIXTEEN)
                .setField(Balance.getDescriptor().findFieldByName("AUD"), SIXTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearAUD()
                .setAUD(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sint64'` {

        private val currentBalance = balance { cAD = EIGHTY }
        private val newBalance = balance { cAD = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCAD(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("CAD"), EIGHTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'sint64'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { cAD = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCAD(EIGHTEEN)
                .setCAD(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("CAD"), EIGHTEEN)
                .setField(Balance.getDescriptor().findFieldByName("CAD"), EIGHTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearCAD()
                .setCAD(EIGHTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'fixed32'` {

        private val currentBalance = balance { cHF = SIXTY }
        private val newBalance = balance { cHF = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCHF(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("CHF"), SIXTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'fixed32'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { cHF = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCHF(SIXTEEN)
                .setCHF(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("CHF"), SIXTEEN)
                .setField(Balance.getDescriptor().findFieldByName("CHF"), SIXTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearCHF()
                .setCHF(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'fixed64'` {

        private val currentBalance = balance { cNY = EIGHTY }
        private val newBalance = balance { cNY = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCNY(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("CNY"), EIGHTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'fixed64'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { cNY = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCNY(EIGHTEEN)
                .setCNY(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("CNY"), EIGHTEEN)
                .setField(Balance.getDescriptor().findFieldByName("CNY"), EIGHTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearCNY()
                .setCNY(EIGHTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sfixed32'` {

        private val currentBalance = balance { pLN = SIXTY }
        private val newBalance = balance { pLN = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setPLN(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(
                    Balance.getDescriptor().findFieldByName("PLN"),
                    SIXTEEN
                )
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'sfixed32'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { pLN = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setPLN(SIXTEEN)
                .setPLN(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("PLN"), SIXTEEN)
                .setField(Balance.getDescriptor().findFieldByName("PLN"), SIXTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearPLN()
                .setPLN(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sfixed64'` {

        private val currentBalance = balance { nZD = EIGHTY }
        private val newBalance = balance { nZD = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setNZD(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("NZD"), EIGHTEEN)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            currentBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'sfixed64'` {

        private val unknownBalance = balance {  }
        private val newBalance = balance { nZD = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setNZD(EIGHTEEN)
                .setNZD(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(Balance.getDescriptor().findFieldByName("NZD"), EIGHTEEN)
                .setField(Balance.getDescriptor().findFieldByName("NZD"), EIGHTEEN)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance)
                .mergeFrom(newBalance)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .mergeFrom(newBalance.toByteArray())
                .mergeFrom(newBalance.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            newBalance.toBuilder()
                .clearNZD()
                .setNZD(EIGHTEEN)
                .build()
        }
    }
}
