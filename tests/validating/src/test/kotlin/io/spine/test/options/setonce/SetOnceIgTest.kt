/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.test.options.setonce

import io.spine.test.options.setonce.SetOnceTestEnv.CERT1
import io.spine.test.options.setonce.SetOnceTestEnv.CERT2
import io.spine.test.options.setonce.SetOnceTestEnv.DONALD
import io.spine.test.options.setonce.SetOnceTestEnv.TWENTY
import io.spine.test.options.setonce.SetOnceTestEnv.SEVENTY
import io.spine.test.options.setonce.SetOnceTestEnv.EIGHTY_KG
import io.spine.test.options.setonce.SetOnceTestEnv.FIFTY_KG
import io.spine.test.options.setonce.SetOnceTestEnv.FIRST_YEAR
import io.spine.test.options.setonce.SetOnceTestEnv.JACK
import io.spine.test.options.setonce.SetOnceTestEnv.NO
import io.spine.test.options.setonce.SetOnceTestEnv.SHORT_HEIGHT
import io.spine.test.options.setonce.SetOnceTestEnv.TWO
import io.spine.test.options.setonce.SetOnceTestEnv.EIGHT
import io.spine.test.options.setonce.SetOnceTestEnv.STUDENT1
import io.spine.test.options.setonce.SetOnceTestEnv.STUDENT2
import io.spine.test.options.setonce.SetOnceTestEnv.TALL_HEIGHT
import io.spine.test.options.setonce.SetOnceTestEnv.THIRD_YEAR
import io.spine.test.options.setonce.SetOnceTestEnv.YES
import io.spine.test.tools.validate.studentSetOnceFalse
import io.spine.test.tools.validate.studentUnconstrained
import io.spine.tools.validation.assertions.assertValidationPasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for `(set_once)` constraint.
 *
 * Notice on the coverage of negative tests: they do not cover all types of setters
 * for each field type intentionally. Take a look on positive [SetOnceFieldsIgTest] and [SetOnceIntegerIgTest].
 * For each field type, they have tests for each kind of setter (direct, reflective,
 * merging, etc.) In order not to write the same extensive tests for negative cases,
 * we rely a bit on the implementation detail. The decision whether the option constraints
 * are added is taken at a "higher level": [io.spine.validation.java.setonce.SetOnceValidationRenderer].
 * So, it is always all setters altered or none. Thus, we only verify the direct field
 * setters in negative tests. We suppose that if it allows re-assignment, the other kinds
 * of setters allow it too.
 */
@DisplayName("`(set_once)` constraint should")
internal class SetOnceIgTest {

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
            cashUSD = TWO
            cashUSD = EIGHT
            cashEUR = TWENTY
            cashEUR = SEVENTY
            cashJPY = TWO
            cashJPY = EIGHT
            cashGBP = TWENTY
            cashGBP = SEVENTY
            cashAUD = TWO
            cashAUD = EIGHT
            cashCAD = TWENTY
            cashCAD = SEVENTY
            cashCHF = TWO
            cashCHF = EIGHT
            cashCNY = TWENTY
            cashCNY = SEVENTY
            cashPLN = TWO
            cashPLN = EIGHT
            cashNZD = TWENTY
            cashNZD = SEVENTY
            hasMedals = YES
            hasMedals = NO
            signature = CERT1
            signature = CERT2
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
            cashUSD = TWO
            cashUSD = EIGHT
            cashEUR = TWENTY
            cashEUR = SEVENTY
            cashJPY = TWO
            cashJPY = EIGHT
            cashGBP = TWENTY
            cashGBP = SEVENTY
            cashAUD = TWO
            cashAUD = EIGHT
            cashCAD = TWENTY
            cashCAD = SEVENTY
            cashCHF = TWO
            cashCHF = EIGHT
            cashCNY = TWENTY
            cashCNY = SEVENTY
            cashPLN = TWO
            cashPLN = EIGHT
            cashNZD = TWENTY
            cashNZD = SEVENTY
            hasMedals = YES
            hasMedals = NO
            signature = CERT1
            signature = CERT2
            yearOfStudy = FIRST_YEAR
            yearOfStudy = THIRD_YEAR
        }
    }
}
