/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.validation.java.rule

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.extractMessageType
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.call
import io.spine.protodata.java.javaClass
import io.spine.time.Temporal
import io.spine.time.validation.Time
import io.spine.time.validation.Time.FUTURE
import io.spine.time.validation.Time.PAST
import io.spine.time.validation.Time.TIME_UNDEFINED
import io.spine.validation.InTime
import io.spine.validation.WHEN

/**
 * Creates a code generator for the [InTime] feature.
 */
internal fun inTimeGenerator(inTime: InTime, ctx: GenerationContext): CodeGenerator {
    val field = ctx.simpleRuleField
    val fieldClass = field.type.extractMessageType(ctx.typeSystem)
        ?.javaClass(ctx.typeSystem)
        ?: unsupportedFieldType(field)
    return when {
        fieldClass == Timestamp::class.java -> {
            TimestampInTimeGenerator(inTime, ctx)
        }

        Temporal::class.java.isAssignableFrom(fieldClass) -> {
            InSpineTimeGenerator(inTime, ctx)
        }

        else -> unsupportedFieldType(field)
    }
}

/**
 * The [InTime] generator for `google.protobuf.Timestamp` fields.
 */
private class TimestampInTimeGenerator(
    inTime: InTime,
    ctx: GenerationContext
) : SimpleRuleGenerator(ctx) {

    private val time = inTime.time

    override fun condition(): Expression<Boolean> {
        val compare = MethodCall<Int>(
            scope = ClassName(Timestamps::class),
            name = "compare",
            arguments = listOf(ctx.fieldOrElement!!, currentTime)
        )
        return time.formatJavaComparison(compare)
    }
}

private val currentTime: Expression<Timestamp> = ClassName(io.spine.base.Time::class)
    .call("currentTime")

/**
 * Formats the comparison expression for the time value.
 *
 * If the current time is being compared to the special [TIME_UNDEFINED] value,
 * the returned result for the formatted expression is always `true`.
 */
private fun Time.formatJavaComparison(compareToCall: Expression<Int>): Expression<Boolean> {
    val operation = when(this) {
        FUTURE -> "> 0"
        PAST -> "< 0"
        TIME_UNDEFINED -> " < 32768"
        else -> error("Unexpected time: `$this`.")
    }
    return Expression("$compareToCall $operation")
}

/**
 * The [InTime] generator for the `spine.time.*` typed fields.
 *
 * The Java class of the field should implement [io.spine.time.Temporal].
 */
private class InSpineTimeGenerator(
    inTime: InTime,
    ctx: GenerationContext
) : SimpleRuleGenerator(ctx) {

    private val time = inTime.time

    override fun condition(): Expression<Boolean> =
        MethodCall(ctx.fieldOrElement!!, time.temporalMethod())
}

private fun Time.temporalMethod(): String = when(this) {
        FUTURE -> "isInFuture"
        PAST -> "isInPast"
        else -> error("Unexpected time: `$this`.")
}

private fun unsupportedFieldType(field: Field): Nothing = error(
    "The `($WHEN)` option is applicable to singular/repeated fields of " +
            "`${Timestamp::class.qualifiedName}`s and types marked as " +
            "`${Temporal::class.qualifiedName}`. The invalid field: `${field.qualifiedName}`."
)
