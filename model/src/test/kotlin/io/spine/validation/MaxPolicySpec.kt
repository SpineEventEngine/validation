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

import io.kotest.matchers.string.shouldContain
import io.spine.protodata.ast.qualifiedName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests [MaxPolicy][io.spine.validation.bound.MaxPolicy]-specific conditions.
 *
 * [MaxPolicy][io.spine.validation.bound.MaxPolicy] is not extensively
 * tested here because it largely relies on the implementation of
 * [RangePolicy][io.spine.validation.bound.RangePolicy] and its tests.
 *
 * Both policies share the same mechanism of the option value parsing.
 *
 * @see RangePolicySpec
 */
@DisplayName("`MaxPolicy` should reject the option")
internal class MaxPolicySpec : CompilationErrorTest() {

    @Test
    fun `with empty value`() =
        assertCompilationFails(MaxWithEmptyValue::class) { field ->
            shouldContain(MAX)
            shouldContain(field.qualifiedName)
            shouldContain("the value is empty")
        }
}
