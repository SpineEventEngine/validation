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

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.protobuf.ByteString.copyFromUtf8;

@DisplayName("`(set_once)` constraint should be compiled so that")
class SetOnceConstraintTest extends SetOnceIntegerConstraintTest {

    @Test
    @DisplayName("not affect fields without the option")
    void notAffectFieldsWithoutOption() {
        assertValidationPasses(() -> SetOnceImplicitFalse.newBuilder()
                .setMessage(Name.newBuilder().setValue("MyName1").build())
                .setMessage(Name.newBuilder().setValue("MyName2").build())
                .setString("string-1")
                .setString("string-2")
                .setDouble(0.25)
                .setDouble(0.75)
                .setFloat(0.25f)
                .setFloat(0.75f)
                .setInt32(5)
                .setInt32(10)
                .setInt64(5)
                .setInt64(10)
                .setUint32(5)
                .setUint32(10)
                .setUint64(5)
                .setUint64(10)
                .setSint32(5)
                .setSint32(10)
                .setSint64(5)
                .setSint64(10)
                .setFixed32(5)
                .setFixed32(10)
                .setFixed64(5)
                .setFixed64(10)
                .setSfixed32(5)
                .setSfixed32(10)
                .setSfixed64(5)
                .setSfixed64(10)
                .build());
    }

    @Test
    @DisplayName("not affect fields with the option set to `false`")
    void notAffectFieldsWithTheOptionSetToFalse() {
        assertValidationPasses(() -> SetOnceExplicitFalse.newBuilder()
                .setMessage(Name.newBuilder().setValue("MyName1").build())
                .setMessage(Name.newBuilder().setValue("MyName2").build())
                .setString("string-1")
                .setString("string-2")
                .setDouble(0.25)
                .setDouble(0.75)
                .setFloat(0.25f)
                .setFloat(0.75f)
                .setInt32(5)
                .setInt32(10)
                .setInt64(5)
                .setInt64(10)
                .setUint32(5)
                .setUint32(10)
                .setUint64(5)
                .setUint64(10)
                .setSint32(5)
                .setSint32(10)
                .setSint64(5)
                .setSint64(10)
                .setFixed32(5)
                .setFixed32(10)
                .setFixed64(5)
                .setFixed64(10)
                .setSfixed32(5)
                .setSfixed32(10)
                .setSfixed64(5)
                .setSfixed64(10)
                .build());
    }

    @Nested
    @DisplayName("prohibit overriding non-default message")
    class ProhibitOverridingNonDefaultMessage {

        private final Name donald = Name.newBuilder()
                .setValue("Donald")
                .build();
        private final Student studentJack = Student.newBuilder()
                .setName(Name.newBuilder().setValue("Jack").build())
                .build();
        private final Student studentDonald = Student.newBuilder()
                .setName(donald)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .setName(donald));
        }

        @Test
        @DisplayName("by builder")
        void byBuilder() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .setName(donald.toBuilder()));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("name"), donald));
        }

        @Test
        @DisplayName("by field merge")
        void byFieldMerge() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .mergeName(donald));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .mergeFrom(studentDonald));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> studentJack.toBuilder()
                    .mergeFrom(studentDonald.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value message")
    class AllowOverridingDefaultAndSameValueMessage {

        private final Name donald = Name.newBuilder()
                .setValue("Donald")
                .build();
        private final Student unnamedStudent = Student.newBuilder()
                .build();
        private final Student studentDonald = Student.newBuilder()
                .setName(donald)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unnamedStudent.toBuilder()
                    .setName(donald)
                    .setName(donald)
                    .build());
        }

        @Test
        @DisplayName("by builder")
        void byBuilder() {
            assertValidationPasses(() -> unnamedStudent.toBuilder()
                    .setName(donald.toBuilder())
                    .setName(donald.toBuilder())
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unnamedStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("name"), donald)
                    .setField(Student.getDescriptor().findFieldByName("name"), donald)
                    .build());
        }

        @Test
        @DisplayName("by field merge")
        void byFieldMerge() {
            assertValidationPasses(() -> unnamedStudent.toBuilder()
                    .mergeName(donald)
                    .mergeName(donald)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unnamedStudent.toBuilder()
                    .mergeFrom(studentDonald)
                    .mergeFrom(studentDonald)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unnamedStudent.toBuilder()
                    .mergeFrom(studentDonald.toByteArray())
                    .mergeFrom(studentDonald.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> studentDonald.toBuilder()
                    .clearName()
                    .setName(donald)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default string")
    class ProhibitOverridingNonDefaultString {

        private static final String STUDENT2 = "student-2";

        private final Student student1 = Student.newBuilder()
                .setId("student-1")
                .build();
        private final Student student2 = Student.newBuilder()
                .setId(STUDENT2)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> student1.toBuilder()
                    .setId(STUDENT2));
        }

        @Test
        @DisplayName("by bytes")
        void byBytes() {
            assertValidationFails(() -> student1.toBuilder()
                    .setIdBytes(copyFromUtf8(STUDENT2)));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> student1.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("id"), STUDENT2));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> student1.toBuilder()
                    .mergeFrom(student2));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> student1.toBuilder()
                    .mergeFrom(student2.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value string")
    class AllowOverridingDefaultAndSameValueString {

        private static final String STUDENT2 = "student-2";

        private final Student undentifiedStudent = Student.newBuilder()
                .build();
        private final Student student2 = Student.newBuilder()
                .setId(STUDENT2)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> undentifiedStudent.toBuilder()
                    .setId(STUDENT2)
                    .setId(STUDENT2)
                    .build());
        }

        @Test
        @DisplayName("by bytes")
        void byBytes() {
            assertValidationPasses(() -> undentifiedStudent.toBuilder()
                    .setIdBytes(copyFromUtf8(STUDENT2))
                    .setIdBytes(copyFromUtf8(STUDENT2))
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> undentifiedStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("id"), STUDENT2)
                    .setField(Student.getDescriptor().findFieldByName("id"), STUDENT2)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> undentifiedStudent.toBuilder()
                    .mergeFrom(student2)
                    .mergeFrom(student2)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> undentifiedStudent.toBuilder()
                    .mergeFrom(student2.toByteArray())
                    .mergeFrom(student2.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> student2.toBuilder()
                    .clearId()
                    .setId(STUDENT2)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default double")
    class ProhibitOverridingNonDefaultDouble {

        private static final double halfOfMeter = 0.5;

        private final Student tallStudent = Student.newBuilder()
                .setHeight(188.5)
                .build();
        private final Student shortStudent = Student.newBuilder()
                .setHeight(halfOfMeter)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> tallStudent.toBuilder()
                    .setHeight(halfOfMeter));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> tallStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("height"), halfOfMeter));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> tallStudent.toBuilder()
                    .mergeFrom(shortStudent));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> tallStudent.toBuilder()
                    .mergeFrom(shortStudent.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value double")
    class AllowOverridingDefaultAndSameValueDouble {

        private static final double halfOfMeter = 0.5;

        private final Student unheightedStudent = Student.newBuilder()
                .build();
        private final Student shortStudent = Student.newBuilder()
                .setHeight(halfOfMeter)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unheightedStudent.toBuilder()
                    .setHeight(halfOfMeter)
                    .setHeight(halfOfMeter)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unheightedStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("height"), halfOfMeter)
                    .setField(Student.getDescriptor().findFieldByName("height"), halfOfMeter)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unheightedStudent.toBuilder()
                    .mergeFrom(shortStudent)
                    .mergeFrom(shortStudent)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unheightedStudent.toBuilder()
                    .mergeFrom(shortStudent.toByteArray())
                    .mergeFrom(shortStudent.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> shortStudent.toBuilder()
                    .clearHeight()
                    .setHeight(halfOfMeter)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default float")
    class ProhibitOverridingNonDefaultFloat {

        private static final float fiftyKilograms = 0.5f;

        private final Student heavyStudent = Student.newBuilder()
                .setWeight(80)
                .build();
        private final Student thinStudent = Student.newBuilder()
                .setWeight(fiftyKilograms)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> heavyStudent.toBuilder()
                    .setWeight(fiftyKilograms));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> heavyStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("weight"), fiftyKilograms));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> heavyStudent.toBuilder()
                    .mergeFrom(thinStudent));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> heavyStudent.toBuilder()
                    .mergeFrom(thinStudent.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value float")
    class AllowOverridingDefaultAndSameValueFloat {

        private static final float fiftyKilograms = 0.5f;

        private final Student unweightedStudent = Student.newBuilder()
                .build();
        private final Student thinStudent = Student.newBuilder()
                .setWeight(fiftyKilograms)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unweightedStudent.toBuilder()
                    .setHeight(fiftyKilograms)
                    .setHeight(fiftyKilograms)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unweightedStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("weight"), fiftyKilograms)
                    .setField(Student.getDescriptor().findFieldByName("weight"), fiftyKilograms)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unweightedStudent.toBuilder()
                    .mergeFrom(thinStudent)
                    .mergeFrom(thinStudent)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unweightedStudent.toBuilder()
                    .mergeFrom(thinStudent.toByteArray())
                    .mergeFrom(thinStudent.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> thinStudent.toBuilder()
                    .clearHeight()
                    .setHeight(fiftyKilograms)
                    .build());
        }
    }

    /**
     * Please note, for `boolean` field type, there are no `byMessageMerge` and `byBytesMerge`
     * tests intentionally.
     *
     * <p>These tests can't override a non-default value by another non-default value as intended.
     * In Protobuf v3, `boolean` field type has only one non-default value: `true`. When we try
     * to override `true` with `false` by merging, the merge method does nothing because it doesn't
     * consider fields with the default values. When we override `false` with `true`, we're just
     * effectively assigning an initial non-default value, which is covered by another test group.
     * See {@link AllowOverridingDefaultValueBooleans}.
     */
    @Nested
    @DisplayName("prohibit overriding non-default boolean")
    class ProhibitOverridingNonDefaultBoolean {

        private static final boolean noMedals = false;

        private final Student awardedStudent = Student.newBuilder()
                .setHasMedals(true)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> awardedStudent.toBuilder()
                    .setHasMedals(noMedals));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> awardedStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("has_medals"), noMedals));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value boolean")
    class AllowOverridingDefaultAndSameValueBoolean {

        private static final boolean has = true;

        private final Student studentWithoutMedals = Student.newBuilder()
                .build();
        private final Student awardedStudent = Student.newBuilder()
                .setHasMedals(true)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> studentWithoutMedals.toBuilder()
                    .setHasMedals(has)
                    .setHasMedals(has)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> studentWithoutMedals.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("has_medals"), has)
                    .setField(Student.getDescriptor().findFieldByName("has_medals"), has)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> studentWithoutMedals.toBuilder()
                    .mergeFrom(awardedStudent)
                    .mergeFrom(awardedStudent)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> studentWithoutMedals.toBuilder()
                    .mergeFrom(awardedStudent.toByteArray())
                    .mergeFrom(awardedStudent.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> awardedStudent.toBuilder()
                    .clearHasMedals()
                    .setHasMedals(has)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default bytes")
    class ProhibitOverridingNonDefaultBytes {

        private final ByteString fullSignature = ByteString.copyFromUtf8("full");

        private final Student studentShortSignature = Student.newBuilder()
                .setSignature(ByteString.copyFromUtf8("short"))
                .build();
        private final Student studentFullSignature = Student.newBuilder()
                .setSignature(fullSignature)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> studentShortSignature.toBuilder()
                    .setSignature(fullSignature));
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> studentShortSignature.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("signature"), fullSignature));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> studentShortSignature.toBuilder()
                    .mergeFrom(studentFullSignature));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> studentShortSignature.toBuilder()
                    .mergeFrom(studentFullSignature.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding empty and same-value bytes")
    class AllowOverridingDefaultAndSameValueBytes {

        private final ByteString fullSignature = ByteString.copyFromUtf8("full");

        private final Student studentNoSignature = Student.newBuilder()
                .build();
        private final Student studentFullSignature = Student.newBuilder()
                .setSignature(fullSignature)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> studentNoSignature.toBuilder()
                    .setSignature(fullSignature)
                    .setSignature(fullSignature)
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> studentNoSignature.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("signature"), fullSignature)
                    .setField(Student.getDescriptor().findFieldByName("signature"), fullSignature)
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> studentNoSignature.toBuilder()
                    .mergeFrom(studentFullSignature)
                    .mergeFrom(studentFullSignature)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> studentNoSignature.toBuilder()
                    .mergeFrom(studentFullSignature.toByteArray())
                    .mergeFrom(studentFullSignature.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> studentFullSignature.toBuilder()
                    .clearSignature()
                    .setSignature(fullSignature)
                    .build());
        }
    }

    @Nested
    @DisplayName("prohibit overriding non-default enum")
    class ProhibitOverridingNonDefaultEnum {

        private final YearOfStudy thirdYear = YearOfStudy.YOS_THIRD;

        private final Student firstYearStudent = Student.newBuilder()
                .setYearOfStudy(YearOfStudy.YOS_FIRST)
                .build();
        private final Student thirdYearStudent = Student.newBuilder()
                .setYearOfStudy(thirdYear)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationFails(() -> firstYearStudent.toBuilder()
                    .setYearOfStudy(thirdYear));
        }

        @Test
        @DisplayName("by ordinal number")
        void byOrdinalNumber() {
            assertValidationFails(() -> firstYearStudent.toBuilder()
                    .setYearOfStudyValue(3)); // Third year.
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationFails(() -> firstYearStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("year_of_study"),
                              thirdYear.getValueDescriptor()));
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationFails(() -> firstYearStudent.toBuilder()
                    .mergeFrom(thirdYearStudent));
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationFails(() -> firstYearStudent.toBuilder()
                    .mergeFrom(thirdYearStudent.toByteArray()));
        }
    }

    @Nested
    @DisplayName("allow overriding default and same-value enum")
    class AllowOverridingDefaultAndSameValueEnum {

        private final YearOfStudy thirdYear = YearOfStudy.YOS_THIRD;

        private final Student unknownYearStudent = Student.newBuilder()
                .build();
        private final Student thirdYearStudent = Student.newBuilder()
                .setYearOfStudy(thirdYear)
                .build();

        @Test
        @DisplayName("by value")
        void byValue() {
            assertValidationPasses(() -> unknownYearStudent.toBuilder()
                    .setYearOfStudy(thirdYear)
                    .setYearOfStudy(thirdYear)
                    .build());
        }

        @Test
        @DisplayName("by ordinal number")
        void byOrdinalNumber() {
            assertValidationPasses(() -> unknownYearStudent.toBuilder()
                    .setYearOfStudyValue(3) // Third year.
                    .setYearOfStudyValue(3) // Third year.
                    .build());
        }

        @Test
        @DisplayName("by reflection")
        void byReflection() {
            assertValidationPasses(() -> unknownYearStudent.toBuilder()
                    .setField(Student.getDescriptor().findFieldByName("year_of_study"),
                              thirdYear.getValueDescriptor())
                    .setField(Student.getDescriptor().findFieldByName("year_of_study"),
                              thirdYear.getValueDescriptor())
                    .build());
        }

        @Test
        @DisplayName("by message merge")
        void byMessageMerge() {
            assertValidationPasses(() -> unknownYearStudent.toBuilder()
                    .mergeFrom(thirdYearStudent)
                    .mergeFrom(thirdYearStudent)
                    .build());
        }

        @Test
        @DisplayName("by bytes merge")
        void byBytesMerge() {
            assertValidationPasses(() -> unknownYearStudent.toBuilder()
                    .mergeFrom(thirdYearStudent.toByteArray())
                    .mergeFrom(thirdYearStudent.toByteArray())
                    .build());
        }

        @Test
        @DisplayName("after clearing")
        void afterClearing() {
            assertValidationPasses(() -> thirdYearStudent.toBuilder()
                    .clearYearOfStudy()
                    .setYearOfStudy(thirdYear)
                    .build());
        }
    }
}
