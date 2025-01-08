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

@file:JvmName("SimpleRules")

package io.spine.validation

import com.google.protobuf.Message
import io.spine.protobuf.isNotDefault
import io.spine.protobuf.pack
import io.spine.protodata.ast.FieldName

/**
 * Creates a [SimpleRule] with a custom operator.
 *
 * @param field The target field.
 * @param customFeature The feature message describing the custom operator.
 * @param description The human-readable text description of the feature.
 * @param errorMessage The error message for the case of violation.
 * @return a new rule.
 */
public fun SimpleRule(
    field: FieldName,
    customFeature: Message,
    description: String,
    errorMessage: String,
    distribute: Boolean
): SimpleRule {
    require(description.isNotBlank())
    val operator = customOperator {
        this.description = description
        feature = customFeature.pack()
    }
    return simpleRule {
        customOperator = operator
        this.errorMessage = errorMessage
        ignoredIfUnset = true
        this.distribute = distribute
        if (field.isNotDefault()) {
            this.field = field
        }
    }
}
