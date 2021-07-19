/*
 * Copyright 2021, TeamDev. All rights reserved.
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

internal class Template(template: String) {

    private var spans: List<Span> = listOf(LiteralSpan(template))
        set(value) {
            println(value)
            field = value
        }

    fun joinExpression(): String =
        spans.joinToString(separator = " + ")

    fun formatDynamic(placeholder: Placeholder, expression: String) {
        spans = spans.flatMap { span ->
            if (span is LiteralSpan) {
                span.splitOn(placeholder)
                    .interlaced(ExpressionSpan(expression))
                    .toList()
            } else {
                listOf(span)
            }
        }
    }

    fun formatStatic(placeholder: Placeholder, value: String) {
        spans = spans.map { span ->
            if (span is LiteralSpan) {
                span.replace(placeholder, value)
            } else {
                span
            }
        }
    }
}

private sealed class Span

private class LiteralSpan(private val value: String): Span() {

    override fun toString(): String = "\"$value\""

    fun replace(placeholder: Placeholder, replacement: String): LiteralSpan {
        val newValue = value.replace(placeholder.fmt, replacement)
        return LiteralSpan(newValue)
    }

    fun splitOn(placeholder: Placeholder): List<Span> =
        value.split(placeholder.fmt).map { LiteralSpan(it) }
}

private class ExpressionSpan(private val code: String): Span() {

    override fun toString(): String = code
}

private fun <T> Iterable<T>.interlaced(infix: T): Sequence<T> = sequence {
    forEachIndexed { index, element ->
        if (index != 0) {
            yield(infix)
        }
        yield(element)
    }
}
