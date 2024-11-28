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

import io.spine.test.options.setonce.TestEnv.CERF1
import io.spine.test.options.setonce.TestEnv.CERF2
import io.spine.test.options.setonce.TestEnv.DONALD
import io.spine.test.options.setonce.TestEnv.EIGHTEEN
import io.spine.test.options.setonce.TestEnv.EIGHTY
import io.spine.test.options.setonce.TestEnv.EIGHTY_KG
import io.spine.test.options.setonce.TestEnv.FIFTY_KG
import io.spine.test.options.setonce.TestEnv.FIRST_YEAR
import io.spine.test.options.setonce.TestEnv.JACK
import io.spine.test.options.setonce.TestEnv.NO
import io.spine.test.options.setonce.TestEnv.SHORT_HEIGHT
import io.spine.test.options.setonce.TestEnv.SIXTEEN
import io.spine.test.options.setonce.TestEnv.SIXTY
import io.spine.test.options.setonce.TestEnv.STUDENT1
import io.spine.test.options.setonce.TestEnv.STUDENT2
import io.spine.test.options.setonce.TestEnv.TALL_HEIGHT
import io.spine.test.options.setonce.TestEnv.THIRD_YEAR
import io.spine.test.options.setonce.TestEnv.YES
import io.spine.test.tools.validate.studentSetOnceFalse
import io.spine.test.tools.validate.studentUnconstrained
import io.spine.validation.assertions.assertValidationPasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(set_once)` constraint should")
internal class SetOnceITest {

    @Test
    fun `not affect fields without the option`() = assertValidationPasses {
        studentUnconstrained {
            name = JACK
            name = DONALD
            id = STUDENT1
            id = STUDENT2
            height = SHORT_HEIGHT
            height = TALL_HEIGHT
            weight = FIFTY_KG
            weight = EIGHTY_KG
            cashUSD = SIXTEEN
            cashUSD = SIXTY
            cashEUR = EIGHTEEN
            cashEUR = EIGHTY
            cashJPY = SIXTEEN
            cashJPY = SIXTY
            cashGBP = EIGHTEEN
            cashGBP = EIGHTY
            cashAUD = SIXTEEN
            cashAUD = SIXTY
            cashCAD = EIGHTEEN
            cashCAD = EIGHTY
            cashCHF = SIXTEEN
            cashCHF = SIXTY
            cashCNY = EIGHTEEN
            cashCNY = EIGHTY
            cashPLN = SIXTEEN
            cashPLN = SIXTY
            cashNZD = EIGHTEEN
            cashNZD = EIGHTY
            hasMedals = YES
            hasMedals = NO
            signature = CERF1
            signature = CERF2
            yearOfStudy = FIRST_YEAR
            yearOfStudy = THIRD_YEAR
        }
    }

    @Test
    fun `not affect fields with the option set to 'false'`() = assertValidationPasses {
        studentSetOnceFalse {
            name = JACK
            name = DONALD
            id = STUDENT1
            id = STUDENT2
            height = SHORT_HEIGHT
            height = TALL_HEIGHT
            weight = FIFTY_KG
            weight = EIGHTY_KG
            cashUSD = SIXTEEN
            cashUSD = SIXTY
            cashEUR = EIGHTEEN
            cashEUR = EIGHTY
            cashJPY = SIXTEEN
            cashJPY = SIXTY
            cashGBP = EIGHTEEN
            cashGBP = EIGHTY
            cashAUD = SIXTEEN
            cashAUD = SIXTY
            cashCAD = EIGHTEEN
            cashCAD = EIGHTY
            cashCHF = SIXTEEN
            cashCHF = SIXTY
            cashCNY = EIGHTEEN
            cashCNY = EIGHTY
            cashPLN = SIXTEEN
            cashPLN = SIXTY
            cashNZD = EIGHTEEN
            cashNZD = EIGHTY
            hasMedals = YES
            hasMedals = NO
            signature = CERF1
            signature = CERF2
            yearOfStudy = FIRST_YEAR
            yearOfStudy = THIRD_YEAR
        }
    }
}
