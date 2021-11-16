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

package io.spine.validation.java

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.Expression
import io.spine.protodata.codegen.java.Literal
import io.spine.protodata.codegen.java.MethodCall
import io.spine.protodata.typeUrl
import io.spine.time.validation.Time
import io.spine.type.TypeUrl
import io.spine.validation.InTime

/**
 * Creates a code generator for the [InTime] feature.
 */
internal fun inTimeGenerator(inTime: InTime, ctx: GenerationContext): CodeGenerator {
    val fieldType = ctx.fieldFromSimpleRule!!.type
    return if (fieldType.message.typeUrl() == TIMESTAMP_TYPE) {
        TimestampInTimeGenerator(inTime, ctx)
    } else {
        InSpineTimeGenerator(inTime, ctx)
    }
}

private val TIMESTAMP_TYPE = TypeUrl.of(Timestamp::class.java).value()

/**
 * The [InTime] generator for `google.protobuf.Timestamp` fields.
 */
private class TimestampInTimeGenerator(
    inTime: InTime,
    ctx: GenerationContext
) : SimpleRuleGenerator(ctx) {

    private val time = inTime.time

    override fun condition(): Expression {
        val compare = MethodCall(
            ClassName(Timestamps::class), "compare", arguments = listOf(
            ctx.fieldOrElement!!,
            currentTime
        ))
        return time.formatJavaComparison(compare)
    }
}

private fun Time.formatJavaComparison(compareToCall: Expression): Expression {
    val operation = when(this) {
        Time.FUTURE -> "> 0"
        Time.PAST -> "< 0"
        else -> throw IllegalStateException("Unexpected time: `$this`.")
    }
    return Literal("$compareToCall $operation")
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
    override fun condition(): Expression {
        val compareTo = MethodCall(
            ctx.fieldOrElement!!,
            time.temporalMethod()
        )
        return time.formatJavaComparison(compareTo)
    }
}

private fun Time.temporalMethod(): String = when(this) {
        Time.FUTURE -> "isInFuture"
        Time.PAST -> "isInPast"
        else -> throw IllegalStateException("Unexpected time: `$this`.")
}

private val currentTime: Expression =
    MethodCall(ClassName(io.spine.base.Time::class), "currentTime")
