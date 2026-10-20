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

package io.spine.test.options

import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.test.tools.validate.InterestRate
import io.spine.test.tools.validate.Probability
import io.spine.test.tools.validate.SchoolClass
import io.spine.test.tools.validate.Year
import io.spine.test.tools.validate.targetMetrics
import io.spine.string.format
import io.spine.tools.validation.RangeFieldExtrema
import io.spine.tools.validation.assertions.assertInvalid
import io.spine.tools.validation.assertions.assertValid
import kotlin.random.Random.Default.nextInt
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Number boundaries constraints should be compiled so that")
internal class NumberConstraintsIgTest {

    @Test
    fun `min field value is checked`() {
        val targetAttendance = 0.66
        val actualAttendance = targetAttendance - 0.2
        val targets = targetMetrics {
            attendanceRate = targetAttendance
        }
        assertViolation(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setAttendanceRate(actualAttendance.toFloat()),
            "must be >= targets.attendance_rate ($targetAttendance)"
        )
        assertValid(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setAttendanceRate(targetAttendance.toFloat())
        )
    }

    @Test
    fun `max field value is checked`() {
        val numberOfStudents = 21
        val failingStudents = numberOfStudents + 1
        assertViolation(
            SchoolClass.newBuilder()
                .setNumberOfStudents(numberOfStudents)
                .setFailingStudents(failingStudents),
            "must be <= _number_of_students ($numberOfStudents)"
        )
        assertValid(
            SchoolClass.newBuilder()
                .setNumberOfStudents(numberOfStudents)
                .setFailingStudents(numberOfStudents - 1),
        )
    }

    @Test
    @Suppress("MaxLineLength") // Long range definition.
    fun `range with field values is checked`() {
        val numberOfStudents = 21
        val targetHonors = 5L
        val targets = targetMetrics {
            honorStudents = targetHonors
        }
        val expected = "[targets.honor_students ($targetHonors) .. _number_of_students ($numberOfStudents)]"
        assertViolation(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setNumberOfStudents(numberOfStudents)
                .setHonorStudents((targetHonors - 1).toInt()),
            expected
        )
        assertViolation(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setNumberOfStudents(numberOfStudents)
                .setHonorStudents(numberOfStudents + 1),
            expected
        )
        assertValid(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setNumberOfStudents(numberOfStudents)
                .setHonorStudents(nextInt(targetHonors.toInt(), numberOfStudents))
        )
    }

    @Test
    fun `range with number and field values is checked`() {
        val targetIncidents = 3
        val minimumIncidents = 0
        val targets = targetMetrics {
            disciplinaryIncidents = targetIncidents
        }
        val expected =  "[$minimumIncidents .. targets.disciplinary_incidents ($targetIncidents)]"
        assertViolation(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setDisciplinaryIncidents(minimumIncidents - 1),
           expected
        )
        assertViolation(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setDisciplinaryIncidents(targetIncidents + 1),
            expected
        )
        assertValid(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setDisciplinaryIncidents(targetIncidents - 1)
        )
    }

    @Test
    fun `range with field and number values is checked`() {
        val targetGrade = 66.6f
        val maxGrade = 100.0
        val targets = targetMetrics {
            averageGrade = targetGrade
        }
        val expected =  "[targets.average_grade ($targetGrade) .. $maxGrade]"
        assertViolation(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setAverageGrade(targetGrade - 1.0),
            expected
        )
        assertViolation(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setAverageGrade(maxGrade + 1.0),
            expected
        )
        assertValid(
            SchoolClass.newBuilder()
                .setTargets(targets)
                .setAverageGrade(maxGrade - 1.0)
        )
    }

    @Test
    fun `min value is checked`() {
        assertViolation(
            InterestRate.newBuilder()
                .setPercent(-3f),
            "must be > 0.0"
        )
        assertValid(
            InterestRate.newBuilder()
                .setPercent(117.3f)
        )
    }

    @Test
    fun `min and max values are checked`() {
        assertViolation(
            Year.newBuilder()
                .setDayCount(42),
            "must be >= 365"
        )
        assertViolation(
            Year.newBuilder()
                .setDayCount(420),
            "must be <= 366"
        )
        assertValid(
            Year.newBuilder()
                .setDayCount(365)
        )
        assertValid(
            Year.newBuilder()
                .setDayCount(366)
        )
    }

    @Test
    fun `numerical range is checked`() {
        assertViolation(
            Probability.newBuilder()
                .setValue(1.1),
            "1.1"
        )
        assertViolation(
            Probability.newBuilder()
                .setValue(-0.1),
            "-0.1"
        )
        assertValid(
            Probability.newBuilder()
                .setValue(0.0)
        )
        assertValid(
            Probability.newBuilder()
                .setValue(1.0)
        )
    }

    @Test
    fun `numerical range handles minimum values of the field type`() {
        val intMinValue = Int.MIN_VALUE
        val longMinValue = Long.MIN_VALUE

        // Protobuf API expects `int` and `long` for unsigned fields.
        val uintMinValue = UInt.MIN_VALUE.toInt()
        val ulongMinValue = ULong.MIN_VALUE.toLong()

        assertValid(
            RangeFieldExtrema.newBuilder()
                .setFloat(-Float.MAX_VALUE)
                .setDouble(-Double.MAX_VALUE)
                .setInt32(intMinValue)
                .setInt64(longMinValue)
                .setUint32(uintMinValue)
                .setUint64(ulongMinValue)
                .setSint32(intMinValue)
                .setSint64(longMinValue)
                .setFixed32(uintMinValue)
                .setFixed64(ulongMinValue)
                .setSfixed32(intMinValue)
                .setSfixed64(longMinValue)
        )
    }

    @Test
    fun `numerical range handles maximum values of the field type`() {
        val intMaxValue = Int.MAX_VALUE
        val longMaxValue = Long.MAX_VALUE

        // Protobuf API expects `int` and `long` for unsigned fields.
        val uintMaxValue = UInt.MAX_VALUE.toInt()
        val ulongMaxValue = ULong.MAX_VALUE.toLong()

        assertValid(
            RangeFieldExtrema.newBuilder()
                .setFloat(Float.MAX_VALUE)
                .setDouble(Double.MAX_VALUE)
                .setInt32(intMaxValue)
                .setInt64(longMaxValue)
                .setUint32(uintMaxValue)
                .setUint64(ulongMaxValue)
                .setSint32(intMaxValue)
                .setSint64(longMaxValue)
                .setFixed32(uintMaxValue)
                .setFixed64(ulongMaxValue)
                .setSfixed32(intMaxValue)
                .setSfixed64(longMaxValue)
        )
    }
}

private fun assertViolation(message: Message.Builder, error: String) {
    val violations = assertInvalid(message)
    violations.size shouldBe 1
    violations[0] shouldNotBe null
    violations[0].message.format() shouldContain error
}
