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

import io.spine.test.options.setonce.given.TestEnv.TWENTY
import io.spine.test.options.setonce.given.TestEnv.SEVENTY
import io.spine.test.options.setonce.given.TestEnv.TWO
import io.spine.test.options.setonce.given.TestEnv.EIGHT
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

        private val currentBalance = studentSetOnce { cashUSD = EIGHT }
        private val newBalance = studentSetOnce { cashUSD = TWO }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashUSD(TWO)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_USD"), TWO)
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
        private val newBalance = studentSetOnce { cashUSD = TWO }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashUSD(TWO)
                .setCashUSD(TWO)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_USD"), TWO)
                .setField(field("cash_USD"), TWO)
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
                .setCashUSD(TWO)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'int64'` {

        private val currentBalance = studentSetOnce { cashEUR = SEVENTY }
        private val newBalance = studentSetOnce { cashEUR = TWENTY }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashEUR(TWENTY)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_EUR"), TWENTY)
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
        private val newBalance = studentSetOnce { cashEUR = TWENTY }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashEUR(TWENTY)
                .setCashEUR(TWENTY)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_EUR"), TWENTY)
                .setField(field("cash_EUR"), TWENTY)
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
                .setCashEUR(TWENTY)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'uint32'` {

        private val currentBalance = studentSetOnce { cashJPY = EIGHT }
        private val newBalance = studentSetOnce { cashJPY = TWO }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashJPY(TWO)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_JPY"), TWO)
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
        private val newBalance = studentSetOnce { cashJPY = TWO }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashJPY(TWO)
                .setCashJPY(TWO)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_JPY"), TWO)
                .setField(field("cash_JPY"), TWO)
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
                .setCashJPY(TWO)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'uint64'` {

        private val currentBalance = studentSetOnce { cashGBP = SEVENTY }
        private val newBalance = studentSetOnce { cashGBP = TWENTY }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashGBP(TWENTY)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_GBP"), TWENTY)
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
        private val newBalance = studentSetOnce { cashGBP = TWENTY }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashGBP(TWENTY)
                .setCashGBP(TWENTY)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_GBP"), TWENTY)
                .setField(field("cash_GBP"), TWENTY)
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
                .setCashGBP(TWENTY)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sint32'` {

        private val currentBalance = studentSetOnce { cashAUD = EIGHT }
        private val newBalance = studentSetOnce { cashAUD = TWO }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashAUD(TWO)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_AUD"), TWO)
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
        private val newBalance = studentSetOnce { cashAUD = TWO }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashAUD(TWO)
                .setCashAUD(TWO)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_AUD"), TWO)
                .setField(field("cash_AUD"), TWO)
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
                .setCashAUD(TWO)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sint64'` {

        private val currentBalance = studentSetOnce { cashCAD = SEVENTY }
        private val newBalance = studentSetOnce { cashCAD = TWENTY }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashCAD(TWENTY)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_CAD"), TWENTY)
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
        private val newBalance = studentSetOnce { cashCAD = TWENTY }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashCAD(TWENTY)
                .setCashCAD(TWENTY)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_CAD"), TWENTY)
                .setField(field("cash_CAD"), TWENTY)
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
                .setCashCAD(TWENTY)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'fixed32'` {

        private val currentBalance = studentSetOnce { cashCHF = EIGHT }
        private val newBalance = studentSetOnce { cashCHF = TWO }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashCHF(TWO)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_CHF"), TWO)
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
        private val newBalance = studentSetOnce { cashCHF = TWO }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashCHF(TWO)
                .setCashCHF(TWO)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_CHF"), TWO)
                .setField(field("cash_CHF"), TWO)
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
                .setCashCHF(TWO)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'fixed64'` {

        private val currentBalance = studentSetOnce { cashCNY = SEVENTY }
        private val newBalance = studentSetOnce { cashCNY = TWENTY }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashCNY(TWENTY)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_CNY"), TWENTY)
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
        private val newBalance = studentSetOnce { cashCNY = TWENTY }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashCNY(TWENTY)
                .setCashCNY(TWENTY)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_CNY"), TWENTY)
                .setField(field("cash_CNY"), TWENTY)
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
                .setCashCNY(TWENTY)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sfixed32'` {

        private val currentBalance = studentSetOnce { cashPLN = EIGHT }
        private val newBalance = studentSetOnce { cashPLN = TWO }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashPLN(TWO)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_PLN"), TWO)
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
        private val newBalance = studentSetOnce { cashPLN = TWO }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashPLN(TWO)
                .setCashPLN(TWO)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_PLN"), TWO)
                .setField(field("cash_PLN"), TWO)
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
                .setCashPLN(TWO)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'sfixed64'` {

        private val currentBalance = studentSetOnce { cashNZD = SEVENTY }
        private val newBalance = studentSetOnce { cashNZD = TWENTY }

        @Test
        fun `by value`() = assertValidationFails {
            currentBalance.toBuilder()
                .setCashNZD(TWENTY)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            currentBalance.toBuilder()
                .setField(field("cash_NZD"), TWENTY)
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
        private val newBalance = studentSetOnce { cashNZD = TWENTY }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setCashNZD(TWENTY)
                .setCashNZD(TWENTY)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownBalance.toBuilder()
                .setField(field("cash_NZD"), TWENTY)
                .setField(field("cash_NZD"), TWENTY)
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
                .setCashNZD(TWENTY)
                .build()
        }
    }
}

private fun field(fieldName: String) = StudentSetOnce.getDescriptor().findFieldByName(fieldName)
