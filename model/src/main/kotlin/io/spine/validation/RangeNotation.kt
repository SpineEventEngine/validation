/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.validation

import com.google.common.collect.Range
import io.spine.validate.ComparableNumber
import java.util.regex.Pattern


/**
 * Transforms a string value defined in a field declaration into a [Range] of [ComparableNumber]s.
 */
internal class RangeNotation
private constructor(

    /**
     * If `true`, this lower bound of the range is inclusive, meaning that the threshold value lies
     * within the range.
     */
    val minInclusive: Boolean,

    /**
     * If `true`, this upper bound of the range is inclusive, meaning that the threshold value lies
     * within the range.
     */
    val maxInclusive: Boolean,

    /**
     * The lower threshold.
     */
    val min: Value,

    /**
     * The upper threshold.
     */
    val max: Value
) {

    companion object {

        /**
         * The regular expression for parsing number ranges.
         *
         * Defines four groups:
         *  1. The opening bracket (a `[` or a `(`).
         *  1. The lower numerical bound.
         *  1. The higher numerical bound.
         *  1. The closing bracket (a `]` or a `)`).
         *
         * All the groups as well as a `..` divider between the numerical bounds must be
         * matched. Extra spaces among the groups and the divider are allowed.
         *
         *
         * Examples of a valid number range:
         *  - `[0..1]`
         *  - `( -17.3 .. +146.0 ]`
         *  - `[+1..+100)`
         *
         * Examples of an invalid number range:
         *  - `1..5` - missing brackets.
         *  - `[0 - 1]` - wrong divider.
         *  - `[0 . . 1]` - divider cannot be split by spaces.
         *  - `( .. 0)` - missing lower bound.
         */
        private val NUMBER_RANGE = Pattern.compile(
            "([\\[(])\\s*([+\\-]?[\\d.]+)\\s*\\.\\.\\s*([+\\-]?[\\d.]+)\\s*([])])"
        )

        /**
         * Parses a range expression from the given notation.
         */
        fun parse(rawNotation: String): RangeNotation {
            val rangeMatcher = NUMBER_RANGE.matcher(rawNotation.trim { it <= ' ' })
            if (!rangeMatcher.find()) {
                throw IllegalStateException("Invalid range ${rangeMatcher}.")
            }
            val minInclusive = ("[" == rangeMatcher.group(1))
            val min = rangeMatcher.group(2).parseToNumber()
            val max = rangeMatcher.group(3).parseToNumber()
            val maxInclusive = ("]" == rangeMatcher.group(4))

            return RangeNotation(minInclusive, maxInclusive, min, max)
        }
    }
}
