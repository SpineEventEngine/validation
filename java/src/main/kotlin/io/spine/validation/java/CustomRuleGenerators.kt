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

import com.google.protobuf.Message
import io.spine.protobuf.unpackGuessingType
import io.spine.validation.DistinctCollection
import io.spine.validation.InTime
import io.spine.validation.RecursiveValidation
import io.spine.validation.Regex
import io.spine.validation.RequiredOneof

/**
 * Creates a [CodeGenerator] for a custom validation operator for the given context.
 */
internal fun generatorForCustom(ctx: GenerationContext): CodeGenerator {
    @Suppress("MoveVariableDeclarationIntoWhen") // For better readability.
    val feature = ctx.feature()
    return when (feature) {
        is DistinctCollection -> DistinctGenerator(ctx)
        is RecursiveValidation -> ValidateGenerator(ctx)
        is Regex -> PatternGenerator(feature, ctx)
        is RequiredOneof -> RequiredOneofGenerator(feature.name, ctx)
        is InTime -> inTimeGenerator(feature, ctx)
        else -> UnsupportedRuleGenerator(feature::class.simpleName!!, ctx)
    }
}

private fun GenerationContext.feature(): Message = with(rule) {
    if (hasSimple()) {
        simple.customOperator.feature.unpackGuessingType()
    } else if (hasMessageWide()) {
        messageWide.operator.feature.unpackGuessingType()
    } else {
        throw IllegalStateException("Rule has no custom operator: $rule")
    }
}
