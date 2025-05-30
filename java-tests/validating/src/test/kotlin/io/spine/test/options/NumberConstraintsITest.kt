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
import io.spine.validate.format
import io.spine.validation.RangeFieldExtrema
import io.spine.validation.assertions.assertInvalid
import io.spine.validation.assertions.assertValid
import kotlin.random.Random.Default.nextInt
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Number boundaries constraints should be compiled so that")
internal class NumberConstraintsITest {

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
