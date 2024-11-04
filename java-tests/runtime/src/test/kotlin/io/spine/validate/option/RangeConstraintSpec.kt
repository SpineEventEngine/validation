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
package io.spine.validate.option

import com.google.common.collect.BoundType
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import io.kotest.matchers.shouldBe
import io.spine.code.proto.FieldDeclaration
import io.spine.test.type.Url
import io.spine.validate.option.RangeConstraint.rangeFromOption
import java.util.stream.Stream
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("Range constraint should")
internal class RangeConstraintSpec {

    @ParameterizedTest
    @MethodSource("validRanges")
    fun `be able to parse valid range strings`(range: String, expected: BoundType) {
        val result = rangeFromOption(range, fieldDecl())

        result.upperBoundType() shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("badRanges")
    fun `throw on incorrectly defined ranges`(badRange: String) {
        // Exceptions would be `NumberFormatException` or `IllegalStateException`.
        assertThrows<Exception> {
            rangeFromOption(badRange, fieldDecl())
        }
    }

    @ParameterizedTest
    @MethodSource("emptyRanges")
    fun `throw on empty ranges`(emptyRange: String) {
        assertThrows<IllegalArgumentException> {
            rangeFromOption(emptyRange, fieldDecl())
        }
    }

    @Suppress("unused") /* Methods used via `@MethodSource`. */
    companion object {

        @JvmStatic
        fun validRanges(): Stream<Arguments> = Stream.of(
            Arguments.of("[1..2]", BoundType.CLOSED),
            Arguments.of("(1..2)", BoundType.OPEN),
            Arguments.of("[1..2)", BoundType.OPEN),
            Arguments.of("(1..2]", BoundType.CLOSED)
        )

        @JvmStatic
        fun badRanges(): ImmutableSet<Arguments> = argumentsFrom(
            "{3..5]",
            "[3..5}",
            "{3..5}",
            "(3..5",
            "3..5)",
            "((3..5]",
            "(3..5]]",
            "(3,5..5)",
            "(3 5..5",
            "[3..5 5]",
            "[3..5,5]",
            "[3;5]",
            "[3...5]"
        )

        @JvmStatic
        fun emptyRanges(): Set<Arguments> {
            val right = 0
            val left = right + 1
            val leftGreaterThanRight =
                rangeCombinationsFor(
                    left,
                    right,
                    ImmutableSet.of('[', '('),
                    ImmutableSet.of(']', ')')
                )
            val closedWithSameNumber = Arguments.arguments("(0..0)")
            return Sets.union(leftGreaterThanRight, ImmutableSet.of(closedWithSameNumber))
        }
    }
}

private fun rangeCombinationsFor(
    left: Number,
    right: Number,
    leftBoundary: ImmutableSet<Char>,
    rightBoundary: ImmutableSet<Char>
): ImmutableSet<Arguments> {
    val lefts = leftBoundary.stream()
        .map { boundary: Char -> "$boundary$left.." }
        .collect(ImmutableSet.toImmutableSet())
    val rights = rightBoundary.stream()
        .map { boundary: Char -> right.toString() + boundary }
        .collect(ImmutableSet.toImmutableSet())
    val result = Sets.cartesianProduct(lefts, rights).stream()
        .flatMap { product: List<String> ->
            Stream.of(product[0] + product[1])
        }
        .map { arguments: String -> Arguments.of(arguments) }
        .collect(ImmutableSet.toImmutableSet())
    return result
}

private fun argumentsFrom(vararg elements: Any): ImmutableSet<Arguments> {
    val builder = ImmutableSet.builder<Arguments>()
    for (element in elements) {
        builder.add(Arguments.of(element))
    }
    return builder.build()
}

private fun fieldDecl(): FieldDeclaration = FieldDeclaration(
    Url.getDescriptor().fields[0]
)
