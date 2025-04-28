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

package io.spine.validation

import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parametrized tests in [io.spine.validation.GoesPolicySpec].
 */
@Suppress("unused") // Data provider for parameterized test.
object GoesPolicyTestEnv {

    /**
     * Test data for [io.spine.validation.GoesPolicySpec.targetFieldHasUnsupportedType].
     */
    @JvmStatic
    fun messagesWithUnsupportedTarget() = listOf(
        "bool" to GoesBoolTarget::class,
        "double" to GoesDoubleTarget::class,
        "float" to GoesFloatTarget::class,
        "int32" to GoesInt32Target::class,
        "int64" to GoesInt64Target::class,
        "uint32" to GoesUInt32Target::class,
        "uint64" to GoesUInt64Target::class,
        "sint32" to GoesSInt32Target::class,
        "sint64" to GoesSInt64Target::class,
        "fixed32" to GoesFixed32Target::class,
        "fixed64" to GoesFixed64Target::class,
        "sfixed32" to GoesSFixed32Target::class,
        "sfixed64" to GoesSFixed64Target::class,
    ).map { arguments(named(it.first, it.second)) }

    /**
     * Test data for [io.spine.validation.GoesPolicySpec.companionFieldHasUnsupportedType].
     */
    @JvmStatic
    fun messagesWithUnsupportedCompanion() = listOf(
        "bool" to GoesBoolCompanion::class,
        "double" to GoesDoubleCompanion::class,
        "float" to GoesFloatCompanion::class,
        "int32" to GoesInt32Companion::class,
        "int64" to GoesInt64Companion::class,
        "uint32" to GoesUInt32Companion::class,
        "uint64" to GoesUInt64Companion::class,
        "sint32" to GoesSInt32Companion::class,
        "sint64" to GoesSInt64Companion::class,
        "fixed32" to GoesFixed32Companion::class,
        "fixed64" to GoesFixed64Companion::class,
        "sfixed32" to GoesSFixed32Companion::class,
        "sfixed64" to GoesSFixed64Companion::class,
    ).map { arguments(named(it.first, it.second)) }
}
