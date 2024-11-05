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

package io.spine.test.tools.validate.setonce

import com.google.protobuf.ByteString.copyFromUtf8
import io.spine.test.tools.validate.Student
import io.spine.test.tools.validate.setonce.SetOnceAssertions.assertValidationFails
import io.spine.test.tools.validate.setonce.SetOnceAssertions.assertValidationPasses
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.DONALD
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.EIGHTY_KG
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.FIFTY_KG
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.FIRST_YEAR
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.FULL_SIGNATURE
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.JACK
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.METER_AND_EIGHT
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.METER_AND_HALF
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.NO
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.SHORT_SIGNATURE
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.STUDENT1
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.STUDENT2
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.THIRD_YEAR
import io.spine.test.tools.validate.setonce.SetOnceTestEnv.YES
import io.spine.test.tools.validate.student
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests `(set_once)` constraint with different field types.
 *
 * Please note, integer fields are covered in a separate file – [SetOnceIntegerFieldsTest].
 * There are many integer types in Protobuf.
 */
@DisplayName("`(set_once)` constraint should")
internal class SetOnceFieldsTest {

    @Nested inner class
    `prohibit overriding non-default message` {

        private val studentJack = student { name = JACK }
        private val studentDonald = student { name = DONALD }

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
                .setField(Student.getDescriptor().findFieldByName("name"), DONALD)
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

        private val unnamedStudent = student {  }
        private val studentDonald = student { name = DONALD }

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
                .setField(Student.getDescriptor().findFieldByName("name"), DONALD)
                .setField(Student.getDescriptor().findFieldByName("name"), DONALD)
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

        private val student1 = student { id = STUDENT1 }
        private val student2 = student { id = STUDENT2 }

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
                .setField(Student.getDescriptor().findFieldByName("id"), STUDENT2)
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

        private val unidentifiedStudent = student {  }
        private val student2 = student { id = STUDENT2 }

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
                .setField(Student.getDescriptor().findFieldByName("id"), STUDENT2)
                .setField(Student.getDescriptor().findFieldByName("id"), STUDENT2)
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

        private val tallStudent = student { height = METER_AND_EIGHT }
        private val shortStudent = student { height = METER_AND_HALF }

        @Test
        fun `by value`() = assertValidationFails {
            tallStudent.toBuilder()
                .setHeight(METER_AND_HALF)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            tallStudent.toBuilder()
                .setField(Student.getDescriptor().findFieldByName("height"), METER_AND_HALF)
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

        private val unmeasuredStudent = student {  }
        private val shortStudent = student { height = METER_AND_HALF }

        @Test
        fun `by value`() = assertValidationPasses {
            unmeasuredStudent.toBuilder()
                .setHeight(METER_AND_HALF)
                .setHeight(METER_AND_HALF)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            unmeasuredStudent.toBuilder()
                .setField(Student.getDescriptor().findFieldByName("height"), METER_AND_HALF)
                .setField(Student.getDescriptor().findFieldByName("height"), METER_AND_HALF)
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
                .setHeight(METER_AND_HALF)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default 'float'` {

        private val heavyStudent = student { weight = EIGHTY_KG }
        private val thinStudent = student { weight = FIFTY_KG }

        @Test
        fun `by value`() = assertValidationFails {
            heavyStudent.toBuilder()
                .setWeight(FIFTY_KG)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            heavyStudent.toBuilder()
                .setField(Student.getDescriptor().findFieldByName("weight"), FIFTY_KG)
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

        private val unweightedStudent = student {  }
        private val thinStudent = student { weight = FIFTY_KG }

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
                .setField(Student.getDescriptor().findFieldByName("weight"), FIFTY_KG)
                .setField(Student.getDescriptor().findFieldByName("weight"), FIFTY_KG)
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
     *
     * Please note, for boolean fields, there are no `byMessageMerge`
     * and `byBytesMerge` tests.
     *
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

        private val awardedStudent = student { hasMedals = YES }

        @Test
        fun `by value`() = assertValidationFails {
            awardedStudent.toBuilder()
                .setHasMedals(NO)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            awardedStudent.toBuilder()
                .setField(Student.getDescriptor().findFieldByName("has_medals"), NO)
        }
    }

    @Nested inner class
    `allow overriding default and same-value 'bool'` {

        private val studentWithoutMedals = student { }
        private val awardedStudent = student { hasMedals = YES }

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
                .setField(Student.getDescriptor().findFieldByName("has_medals"), YES)
                .setField(Student.getDescriptor().findFieldByName("has_medals"), YES)
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

        private val studentShortSignature = student { signature = SHORT_SIGNATURE }
        private val studentFullSignature = student { signature = FULL_SIGNATURE }

        @Test
        fun `by value`() = assertValidationFails {
            studentShortSignature.toBuilder()
                .setSignature(FULL_SIGNATURE)
        }

        @Test
        fun `by reflection`() = assertValidationFails {
            studentShortSignature.toBuilder()
                .setField(
                    Student.getDescriptor().findFieldByName("signature"),
                    FULL_SIGNATURE
                )
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

        private val studentNoSignature = student {  }
        private val studentFullSignature = student { signature = FULL_SIGNATURE}

        @Test
        fun `by value`() = assertValidationPasses {
            studentNoSignature.toBuilder()
                .setSignature(FULL_SIGNATURE)
                .setSignature(FULL_SIGNATURE)
                .build()
        }

        @Test
        fun `by reflection`() = assertValidationPasses {
            studentNoSignature.toBuilder()
                .setField(
                    Student.getDescriptor().findFieldByName("signature"),
                    FULL_SIGNATURE
                )
                .setField(
                    Student.getDescriptor().findFieldByName("signature"),
                    FULL_SIGNATURE
                )
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
                .setSignature(FULL_SIGNATURE)
                .build()
        }
    }

    @Nested inner class
    `prohibit overriding non-default enum` {

        private val firstYearStudent = student { yearOfStudy = FIRST_YEAR }
        private val thirdYearStudent = student { yearOfStudy = THIRD_YEAR }

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
                .setField(
                    Student.getDescriptor().findFieldByName("year_of_study"),
                    THIRD_YEAR.valueDescriptor
                )
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

        private val unknownYearStudent = student {  }
        private val thirdYearStudent = student { yearOfStudy = THIRD_YEAR }

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
                .setField(
                    Student.getDescriptor().findFieldByName("year_of_study"),
                    THIRD_YEAR.valueDescriptor
                )
                .setField(
                    Student.getDescriptor().findFieldByName("year_of_study"),
                    THIRD_YEAR.valueDescriptor
                )
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
