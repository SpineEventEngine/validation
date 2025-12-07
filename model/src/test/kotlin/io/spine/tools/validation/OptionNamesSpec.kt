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

import io.kotest.matchers.shouldBe
import io.spine.option.OptionsProto
import io.spine.time.validation.TimeOptionsProto
import io.spine.validation.DISTINCT
import io.spine.validation.IS_REQUIRED
import io.spine.validation.MAX
import io.spine.validation.MIN
import io.spine.validation.PATTERN
import io.spine.validation.RANGE
import io.spine.validation.REQUIRED
import io.spine.validation.SET_ONCE
import io.spine.validation.VALIDATE
import io.spine.validation.WHEN
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`OptionNames` should match a descriptor name for")
internal class OptionNamesSpec {

    @Test
    fun distinct() {
        DISTINCT shouldBe OptionsProto.distinct.descriptor.name
    }

    @Test
    fun is_required() {
        IS_REQUIRED shouldBe OptionsProto.isRequired.descriptor.name
    }

    @Test
    fun max() {
        MAX shouldBe OptionsProto.max.descriptor.name
    }

    @Test
    fun min() {
        MIN shouldBe OptionsProto.min.descriptor.name
    }

    @Test
    fun pattern() {
        PATTERN shouldBe OptionsProto.pattern.descriptor.name
    }

    @Test
    fun range() {
        RANGE shouldBe OptionsProto.range.descriptor.name
    }

    @Test
    fun required() {
        REQUIRED shouldBe OptionsProto.required.descriptor.name
    }

    @Test
    fun set_once() {
        SET_ONCE shouldBe OptionsProto.setOnce.descriptor.name
    }

    @Test
    fun validate() {
        VALIDATE shouldBe OptionsProto.validate.descriptor.name
    }

    @Test
    fun `when`() {
        WHEN shouldBe TimeOptionsProto.`when`.descriptor.name
    }
}
