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

package io.spine.test.options.setonce

import io.spine.test.options.setonce.TestEnv.EIGHTEEN
import io.spine.test.options.setonce.TestEnv.EIGHTY
import io.spine.test.options.setonce.TestEnv.SIXTEEN
import io.spine.test.options.setonce.TestEnv.SIXTY
import io.spine.test.tools.validate.StudentSetOnce
import io.spine.test.tools.validate.studentSetOnce
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

        private val currentBalance = studentSetOnce { cashUSD = SIXTY }
        private val newBalance = studentSetOnce { cashUSD = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashUSD(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_USD"), SIXTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashUSD = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashUSD(SIXTEEN)
                .setCashUSD(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_USD"), SIXTEEN)
                .setField(field("cash_USD"), SIXTEEN)
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
                .clearCashUSD()
                .setCashUSD(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'int64'` {

        private val currentBalance = studentSetOnce { cashEUR = EIGHTY }
        private val newBalance = studentSetOnce { cashEUR = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashEUR(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_EUR"), EIGHTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashEUR = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashEUR(EIGHTEEN)
                .setCashEUR(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_EUR"), EIGHTEEN)
                .setField(field("cash_EUR"), EIGHTEEN)
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
                .clearCashEUR()
                .setCashEUR(EIGHTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'uint32'` {

        private val currentBalance = studentSetOnce { cashJPY = SIXTY }
        private val newBalance = studentSetOnce { cashJPY = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashJPY(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_JPY"), SIXTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashJPY = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashJPY(SIXTEEN)
                .setCashJPY(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_JPY"), SIXTEEN)
                .setField(field("cash_JPY"), SIXTEEN)
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
                .clearCashJPY()
                .setCashJPY(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'uint64'` {

        private val currentBalance = studentSetOnce { cashGBP = EIGHTY }
        private val newBalance = studentSetOnce { cashGBP = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashGBP(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_GBP"), EIGHTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashGBP = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashGBP(EIGHTEEN)
                .setCashGBP(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_GBP"), EIGHTEEN)
                .setField(field("cash_GBP"), EIGHTEEN)
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
                .clearCashGBP()
                .setCashGBP(EIGHTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sint32'` {

        private val currentBalance = studentSetOnce { cashAUD = SIXTY }
        private val newBalance = studentSetOnce { cashAUD = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashAUD(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_AUD"), SIXTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashAUD = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashAUD(SIXTEEN)
                .setCashAUD(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_AUD"), SIXTEEN)
                .setField(field("cash_AUD"), SIXTEEN)
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
                .clearCashAUD()
                .setCashAUD(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sint64'` {

        private val currentBalance = studentSetOnce { cashCAD = EIGHTY }
        private val newBalance = studentSetOnce { cashCAD = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashCAD(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_CAD"), EIGHTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashCAD = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashCAD(EIGHTEEN)
                .setCashCAD(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_CAD"), EIGHTEEN)
                .setField(field("cash_CAD"), EIGHTEEN)
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
                .clearCashCAD()
                .setCashCAD(EIGHTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'fixed32'` {

        private val currentBalance = studentSetOnce { cashCHF = SIXTY }
        private val newBalance = studentSetOnce { cashCHF = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashCHF(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_CHF"), SIXTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashCHF = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashCHF(SIXTEEN)
                .setCashCHF(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_CHF"), SIXTEEN)
                .setField(field("cash_CHF"), SIXTEEN)
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
                .clearCashCHF()
                .setCashCHF(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'fixed64'` {

        private val currentBalance = studentSetOnce { cashCNY = EIGHTY }
        private val newBalance = studentSetOnce { cashCNY = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashCNY(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_CNY"), EIGHTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashCNY = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashCNY(EIGHTEEN)
                .setCashCNY(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_CNY"), EIGHTEEN)
                .setField(field("cash_CNY"), EIGHTEEN)
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
                .clearCashCNY()
                .setCashCNY(EIGHTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sfixed32'` {

        private val currentBalance = studentSetOnce { cashPLN = SIXTY }
        private val newBalance = studentSetOnce { cashPLN = SIXTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashPLN(SIXTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_PLN"), SIXTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashPLN = SIXTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashPLN(SIXTEEN)
                .setCashPLN(SIXTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_PLN"), SIXTEEN)
                .setField(field("cash_PLN"), SIXTEEN)
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
                .clearCashPLN()
                .setCashPLN(SIXTEEN)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sfixed64'` {

        private val currentBalance = studentSetOnce { cashNZD = EIGHTY }
        private val newBalance = studentSetOnce { cashNZD = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashNZD(EIGHTEEN)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_NZD"), EIGHTEEN)
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

        private val unknownBalance = studentSetOnce {  }
        private val newBalance = studentSetOnce { cashNZD = EIGHTEEN }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashNZD(EIGHTEEN)
                .setCashNZD(EIGHTEEN)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_NZD"), EIGHTEEN)
                .setField(field("cash_NZD"), EIGHTEEN)
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
                .clearCashNZD()
                .setCashNZD(EIGHTEEN)
                .build()
        }
    }
}

private fun field(fieldName: String) = StudentSetOnce.getDescriptor().findFieldByName(fieldName)
