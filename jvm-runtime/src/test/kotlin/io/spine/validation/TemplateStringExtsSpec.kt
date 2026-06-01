/*
 * Copyright 2026, TeamDev. All rights reserved.
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

import com.google.protobuf.Timestamp
import io.kotest.matchers.maps.shouldContain
import io.spine.code.proto.FieldDeclaration
import io.spine.validation.StandardPlaceholder.FIELD_PATH
import io.spine.validation.StandardPlaceholder.FIELD_TYPE
import io.spine.validation.StandardPlaceholder.GOES_COMPANION
import io.spine.validation.StandardPlaceholder.PARENT_TYPE
import io.spine.validation.StandardPlaceholder.REGEX_PATTERN
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`TemplateStringExts` should")
internal class TemplateStringExtsSpec {

    private val secondsField = FieldDeclaration(
        Timestamp.getDescriptor().findFieldByNumber(Timestamp.SECONDS_FIELD_NUMBER)
    )

    @Test
    fun `fill placeholders from a field declaration`() {
        val template = io.spine.string.TemplateString.newBuilder()
            .withField(secondsField)
            .build()

        template.placeholderValueMap.let {
            it shouldContain (FIELD_PATH.value.name to "seconds")
            it shouldContain (FIELD_TYPE.value.name to "long")
            it shouldContain (PARENT_TYPE.value.name to "google.protobuf.Timestamp")
        }
    }

    @Test
    fun `fill companion placeholder`() {
        val template = io.spine.string.TemplateString.newBuilder()
            .withCompanion(secondsField)
            .build()
        
        template.placeholderValueMap shouldContain (GOES_COMPANION.value.name to "seconds")
    }

    @Test
    fun `fill regex pattern placeholder`() {
        val pattern = "^[a-z]+$"
        val template = io.spine.string.TemplateString.newBuilder()
            .withRegex(pattern)
            .build()
        
        template.placeholderValueMap shouldContain (REGEX_PATTERN.value.name to pattern)
    }
}
