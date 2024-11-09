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

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.settings.loadSettings
import io.spine.server.event.NoReaction
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.RequiredRule.isRequired
import io.spine.validation.event.RuleAdded

/**
 * A policy which defines validation rules for ID fields.
 *
 * An ID of a signal message or an entity state is the first field
 * declared in the type, disregarding the index of the proto field.
 *
 * The ID field is assumed as required for commands and entity states,
 * unless it is specifically marked otherwise using the field options.
 *
 * Implementations define the ways of discovering signal and entity state messages.
 */
internal abstract class RequiredIdPolicy : ValidationPolicy<TypeDiscovered>() {

    protected val config: ValidationConfig? by lazy {
        if (!settingsAvailable()) {
            null
        } else {
            loadSettings<ValidationConfig>()
        }
    }

    /**
     * Given an ID field, generates the required rule event.
     *
     * If the field is marked with `(required) = false`, no rule is generated.
     *
     * @param field ID field.
     * @return a required rule event or `NoReaction`, if the ID field is not required.
     */
    @Suppress("ReturnCount") // prefer sooner exit and precise conditions.
    fun withField(field: Field): EitherOf2<RuleAdded, NoReaction> {
        if (!isRequired(field, true)) {
            return ignore()
        }
        val errorMessage = "The ID field `${field.name.value}` must be set."
        val rule = RequiredRule.forField(field, errorMessage)
            ?: return ignore()
        return rule.toEvent(field.declaringType).asA()
    }
}
