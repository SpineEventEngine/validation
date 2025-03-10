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

import com.google.protobuf.ByteString.copyFromUtf8
import io.spine.test.options.setonce.SetOnceTestEnv.DONALD
import io.spine.test.options.setonce.SetOnceTestEnv.EIGHTY_KG
import io.spine.test.options.setonce.SetOnceTestEnv.FIFTY_KG
import io.spine.test.options.setonce.SetOnceTestEnv.FIRST_YEAR
import io.spine.test.options.setonce.SetOnceTestEnv.CERT1
import io.spine.test.options.setonce.SetOnceTestEnv.JACK
import io.spine.test.options.setonce.SetOnceTestEnv.TALL_HEIGHT
import io.spine.test.options.setonce.SetOnceTestEnv.SHORT_HEIGHT
import io.spine.test.options.setonce.SetOnceTestEnv.NO
import io.spine.test.options.setonce.SetOnceTestEnv.CERT2
import io.spine.test.options.setonce.SetOnceTestEnv.STUDENT1
import io.spine.test.options.setonce.SetOnceTestEnv.STUDENT2
import io.spine.test.options.setonce.SetOnceTestEnv.THIRD_YEAR
import io.spine.test.options.setonce.SetOnceTestEnv.YES
import io.spine.test.tools.validate.StudentSetOnce
import io.spine.test.tools.validate.studentSetOnce
import io.spine.validation.assertions.assertValidationFails
import io.spine.validation.assertions.assertValidationPasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests `(set_once)` constraint with different field types.
 *
 * Please note, integer fields are covered in a separate file – [SetOnceIntegerITest].
 * There are many integer types in Protobuf.
 */
@DisplayName("`(set_once)` constraint should")
internal class SetOnceFieldsITest {

    @Nested inner class
    `prohibit overriding non-default message` {

        private val studentJack = studentSetOnce { name = JACK }
        private val studentDonald = studentSetOnce { name = DONALD }

        @Test
        fun `by value`() = assertValidationFails {
            studentJack.toBuilder()
                .setName(DONALD)
        }

        @Test
        fun `by builder`() = assertValidationFails {
            studentJack.toBuilder()
                .setName(DONALD.toBuilder())
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            studentJack.toBuilder()
                .setField(field("name"), DONALD)
        }

        @Test
        fun `by field merge`() = assertValidationFails {
            studentJack.toBuilder()
                .mergeName(DONALD)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            studentJack.toBuilder()
                .mergeFrom(studentDonald)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            studentJack.toBuilder()
                .mergeFrom(studentDonald.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value message` {

        private val unnamedStudent = studentSetOnce {  }
        private val studentDonald = studentSetOnce { name = DONALD }

        @Test
        fun `by value`() = assertValidationPasses {
            unnamedStudent.toBuilder()
                .setName(DONALD)
                .setName(DONALD)
                .build()
        }

        @Test
        fun `by builder`() = assertValidationPasses {
            unnamedStudent.toBuilder()
                .setName(DONALD.toBuilder())
                .setName(DONALD.toBuilder())
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unnamedStudent.toBuilder()
                .setField(field("name"), DONALD)
                .setField(field("name"), DONALD)
                .build()
        }

        @Test
        fun `by field merge`() = assertValidationPasses {
            unnamedStudent.toBuilder()
                .mergeName(DONALD)
                .mergeName(DONALD)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unnamedStudent.toBuilder()
                .mergeFrom(studentDonald)
                .mergeFrom(studentDonald)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unnamedStudent.toBuilder()
                .mergeFrom(studentDonald.toByteArray())
                .mergeFrom(studentDonald.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            studentDonald.toBuilder()
                .clearName()
                .setName(DONALD)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'string'` {

        private val student1 = studentSetOnce { id = STUDENT1 }
        private val student2 = studentSetOnce { id = STUDENT2 }

        @Test
        fun `by value`() = assertValidationFails {
            student1.toBuilder()
                .setId(STUDENT2)
        }

        @Test
        fun `by bytes`() = assertValidationFails {
            student1.toBuilder()
                .setIdBytes(copyFromUtf8(STUDENT2))
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            student1.toBuilder()
                .setField(field("id"), STUDENT2)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            student1.toBuilder()
                .mergeFrom(student2)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            student1.toBuilder()
                .mergeFrom(student2.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'string'` {

        private val unidentifiedStudent = studentSetOnce {  }
        private val student2 = studentSetOnce { id = STUDENT2 }

        @Test
        fun `by value`() = assertValidationPasses {
            unidentifiedStudent.toBuilder()
                .setId(STUDENT2)
                .setId(STUDENT2)
                .build()
        }

        @Test
        fun `by bytes`() = assertValidationPasses {
                unidentifiedStudent.toBuilder()
                    .setIdBytes(copyFromUtf8(STUDENT2))
                    .setIdBytes(copyFromUtf8(STUDENT2))
                    .build()
            }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unidentifiedStudent.toBuilder()
                .setField(field("id"), STUDENT2)
                .setField(field("id"), STUDENT2)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unidentifiedStudent.toBuilder()
                .mergeFrom(student2)
                .mergeFrom(student2)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unidentifiedStudent.toBuilder()
                .mergeFrom(student2.toByteArray())
                .mergeFrom(student2.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            student2.toBuilder()
                .clearId()
                .setId(STUDENT2)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'double'` {

        private val tallStudent = studentSetOnce { height = TALL_HEIGHT }
        private val shortStudent = studentSetOnce { height = SHORT_HEIGHT }

        @Test
        fun `by value`() = assertValidationFails {
            tallStudent.toBuilder()
                .setHeight(SHORT_HEIGHT)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            tallStudent.toBuilder()
                .setField(field("height"), SHORT_HEIGHT)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            tallStudent.toBuilder()
                .mergeFrom(shortStudent)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            tallStudent.toBuilder()
                .mergeFrom(shortStudent.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'double'` {

        private val unmeasuredStudent = studentSetOnce {  }
        private val shortStudent = studentSetOnce { height = SHORT_HEIGHT }

        @Test
        fun `by value`() = assertValidationPasses {
            unmeasuredStudent.toBuilder()
                .setHeight(SHORT_HEIGHT)
                .setHeight(SHORT_HEIGHT)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unmeasuredStudent.toBuilder()
                .setField(field("height"), SHORT_HEIGHT)
                .setField(field("height"), SHORT_HEIGHT)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unmeasuredStudent.toBuilder()
                .mergeFrom(shortStudent)
                .mergeFrom(shortStudent)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unmeasuredStudent.toBuilder()
                .mergeFrom(shortStudent.toByteArray())
                .mergeFrom(shortStudent.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            shortStudent.toBuilder()
                .clearHeight()
                .setHeight(SHORT_HEIGHT)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'float'` {

        private val heavyStudent = studentSetOnce { weight = EIGHTY_KG }
        private val thinStudent = studentSetOnce { weight = FIFTY_KG }

        @Test
        fun `by value`() = assertValidationFails {
            heavyStudent.toBuilder()
                .setWeight(FIFTY_KG)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            heavyStudent.toBuilder()
                .setField(field("weight"), FIFTY_KG)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            heavyStudent.toBuilder()
                .mergeFrom(thinStudent)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            heavyStudent.toBuilder()
                .mergeFrom(thinStudent.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'float'` {

        private val unweightedStudent = studentSetOnce {  }
        private val thinStudent = studentSetOnce { weight = FIFTY_KG }

        @Test
        fun `by value`() = assertValidationPasses {
            unweightedStudent.toBuilder()
                .setWeight(FIFTY_KG)
                .setWeight(FIFTY_KG)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unweightedStudent.toBuilder()
                .setField(field("weight"), FIFTY_KG)
                .setField(field("weight"), FIFTY_KG)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unweightedStudent.toBuilder()
                .mergeFrom(thinStudent)
                .mergeFrom(thinStudent)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unweightedStudent.toBuilder()
                .mergeFrom(thinStudent.toByteArray())
                .mergeFrom(thinStudent.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            thinStudent.toBuilder()
                .clearWeight()
                .setWeight(FIFTY_KG)
                .build()
        }
    }

    /**
     * Tests `(set_once)` constraint when the option is applied to a boolean field.
     *
     * Please note, for boolean fields, there are no `byMessageMerge`
     * and `byBytesMerge` tests.
     *
     * It is impossible to override a non-default value by another non-default value
     * for a boolean field. In Protobuf v3, boolean has only one non-default value: `true`.
     * When we try to override `true` with `false` by merging, the merge method does
     * nothing because it doesn't consider fields with the default values. When we override
     * `false` with `true`,we're just effectively assigning an initial non-default
     * value, which is tested by another test suite.
     */
    @Nested inner class
    `prohibit overriding non-default 'bool'` {

        private val awardedStudent = studentSetOnce { hasMedals = YES }

        @Test
        fun `by value`() = assertValidationFails {
            awardedStudent.toBuilder()
                .setHasMedals(NO)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            awardedStudent.toBuilder()
                .setField(field("has_medals"), NO)
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'bool'` {

        private val studentWithoutMedals = studentSetOnce { }
        private val awardedStudent = studentSetOnce { hasMedals = YES }

        @Test
        fun `by value`() = assertValidationPasses {
            studentWithoutMedals.toBuilder()
                .setHasMedals(YES)
                .setHasMedals(YES)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            studentWithoutMedals.toBuilder()
                .setField(field("has_medals"), YES)
                .setField(field("has_medals"), YES)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            studentWithoutMedals.toBuilder()
                .mergeFrom(awardedStudent)
                .mergeFrom(awardedStudent)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            studentWithoutMedals.toBuilder()
                .mergeFrom(awardedStudent.toByteArray())
                .mergeFrom(awardedStudent.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            awardedStudent.toBuilder()
                .clearHasMedals()
                .setHasMedals(YES)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'bytes'` {

        private val studentShortSignature = studentSetOnce { signature = CERT2 }
        private val studentFullSignature = studentSetOnce { signature = CERT1 }

        @Test
        fun `by value`() = assertValidationFails {
            studentShortSignature.toBuilder()
                .setSignature(CERT1)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            studentShortSignature.toBuilder()
                .setField(field("signature"), CERT1)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            studentShortSignature.toBuilder()
                .mergeFrom(studentFullSignature)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            studentShortSignature.toBuilder()
                .mergeFrom(studentFullSignature.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding empty and same-value 'bytes'` {

        private val studentNoSignature = studentSetOnce {  }
        private val studentFullSignature = studentSetOnce { signature = CERT1}

        @Test
        fun `by value`() = assertValidationPasses {
            studentNoSignature.toBuilder()
                .setSignature(CERT1)
                .setSignature(CERT1)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            studentNoSignature.toBuilder()
                .setField(field("signature"), CERT1)
                .setField(field("signature"), CERT1)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            studentNoSignature.toBuilder()
                .mergeFrom(studentFullSignature)
                .mergeFrom(studentFullSignature)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            studentNoSignature.toBuilder()
                .mergeFrom(studentFullSignature.toByteArray())
                .mergeFrom(studentFullSignature.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            studentFullSignature.toBuilder()
                .clearSignature()
                .setSignature(CERT1)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default enum` {

        private val firstYearStudent = studentSetOnce { yearOfStudy = FIRST_YEAR }
        private val thirdYearStudent = studentSetOnce { yearOfStudy = THIRD_YEAR }

        @Test
        fun `by value`() = assertValidationFails {
            firstYearStudent.toBuilder()
                .setYearOfStudy(THIRD_YEAR)
        }

        @Test
        fun `by ordinal number`() = assertValidationFails {
            firstYearStudent.toBuilder()
                .setYearOfStudyValue(3) // Third year.
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            firstYearStudent.toBuilder()
                .setField(field("year_of_study"), THIRD_YEAR.valueDescriptor)
        }

        @Test
        fun `by message merge`() = assertValidationFails {
            firstYearStudent.toBuilder()
                .mergeFrom(thirdYearStudent)
        }

        @Test
        fun `by bytes merge`() = assertValidationFails {
            firstYearStudent.toBuilder()
                .mergeFrom(thirdYearStudent.toByteArray())
        }
    }

    @Nested inner class
    `allow overriding default and same-value enum` {

        private val unknownYearStudent = studentSetOnce {  }
        private val thirdYearStudent = studentSetOnce { yearOfStudy = THIRD_YEAR }

        @Test
        fun `by value`() = assertValidationPasses {
            unknownYearStudent.toBuilder()
                .setYearOfStudy(THIRD_YEAR)
                .setYearOfStudy(THIRD_YEAR)
                .build()
        }

        @Test
        fun `by ordinal number`() = assertValidationPasses {
            unknownYearStudent.toBuilder()
                .setYearOfStudyValue(3) // Third year.
                .setYearOfStudyValue(3) // Third year.
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unknownYearStudent.toBuilder()
                .setField(field("year_of_study"), THIRD_YEAR.valueDescriptor)
                .setField(field("year_of_study"), THIRD_YEAR.valueDescriptor)
                .build()
        }

        @Test
        fun `by message merge`() = assertValidationPasses {
            unknownYearStudent.toBuilder()
                .mergeFrom(thirdYearStudent)
                .mergeFrom(thirdYearStudent)
                .build()
        }

        @Test
        fun `by bytes merge`() = assertValidationPasses {
            unknownYearStudent.toBuilder()
                .mergeFrom(thirdYearStudent.toByteArray())
                .mergeFrom(thirdYearStudent.toByteArray())
                .build()
        }

        @Test
        fun `after clearing`() = assertValidationPasses {
            thirdYearStudent.toBuilder()
                .clearYearOfStudy()
                .setYearOfStudy(THIRD_YEAR)
                .build()
        }
    }
}

private fun field(fieldName: String) = StudentSetOnce.getDescriptor().findFieldByName(fieldName)
