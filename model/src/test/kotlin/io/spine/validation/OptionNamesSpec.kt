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

package io.spine.validation

import io.kotest.matchers.shouldBe
import io.spine.option.OptionsProto.distinct
import io.spine.option.OptionsProto.isRequired
import io.spine.option.OptionsProto.max
import io.spine.option.OptionsProto.min
import io.spine.option.OptionsProto.pattern
import io.spine.option.OptionsProto.range
import io.spine.option.OptionsProto.required
import io.spine.option.OptionsProto.setOnce
import io.spine.option.OptionsProto.validate
import io.spine.time.validation.TimeOptionsProto.`when`
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`OptionNames` should match a descriptor name for")
internal class OptionNamesSpec {

    @Test
    fun distinct() {
        DISTINCT shouldBe distinct.descriptor.name
    }

    @Test
    fun is_required() {
        IS_REQUIRED shouldBe isRequired.descriptor.name
    }

    @Test
    fun max() {
        MAX shouldBe max.descriptor.name
    }

    @Test
    fun min() {
        MIN shouldBe min.descriptor.name
    }

    @Test
    fun pattern() {
        PATTERN shouldBe pattern.descriptor.name
    }

    @Test
    fun range() {
        RANGE shouldBe range.descriptor.name
    }

    @Test
    fun required() {
        REQUIRED shouldBe required.descriptor.name
    }

    @Test
    fun set_once() {
        SET_ONCE shouldBe setOnce.descriptor.name
    }

    @Test
    fun validate() {
        VALIDATE shouldBe validate.descriptor.name
    }

    @Test
    fun `when`() {
        WHEN shouldBe `when`.descriptor.name
    }
}
