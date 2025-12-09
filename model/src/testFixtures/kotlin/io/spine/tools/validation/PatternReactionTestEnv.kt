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

package io.spine.tools.validation

import io.spine.tools.validation.given.PatternBoolField
import io.spine.tools.validation.given.PatternDoubleField
import io.spine.tools.validation.given.PatternIntField
import io.spine.tools.validation.given.PatternMessageField
import io.spine.tools.validation.given.PatternRepeatedBoolField
import io.spine.tools.validation.given.PatternRepeatedDoubleField
import io.spine.tools.validation.given.PatternRepeatedIntField
import io.spine.tools.validation.given.PatternRepeatedMessageField
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.provider.Arguments

/**
 * Provides data for parametrized tests in `PatternReactionSpec`.
 */
@Suppress("unused") // Data provider for parameterized test.
object PatternReactionTestEnv {

    /**
     * Test data for `PatternReactionSpec.targetFieldHasUnsupportedType()`.
     */
    @JvmStatic
    fun messagesWithUnsupportedTarget() = listOf(
        "bool" to PatternBoolField::class,
        "repeated double" to PatternRepeatedBoolField::class,
        "int32" to PatternIntField::class,
        "repeated int32" to PatternRepeatedIntField::class,
        "double" to PatternDoubleField::class,
        "repeated double" to PatternRepeatedDoubleField::class,
        "message" to PatternMessageField::class,
        "repeated message" to PatternRepeatedMessageField::class,
    ).map { Arguments.arguments(Named.named(it.first, it.second)) }
}
