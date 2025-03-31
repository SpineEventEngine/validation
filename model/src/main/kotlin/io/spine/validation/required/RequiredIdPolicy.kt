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

package io.spine.validation.required

import io.spine.option.OptionsProto
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.ast.ref
import io.spine.protodata.plugin.Policy
import io.spine.protodata.settings.loadSettings
import io.spine.server.event.NoReaction
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.ValidationConfig
import io.spine.validation.ValidationPluginPart
import io.spine.validation.event.RequiredFieldDiscovered
import io.spine.validation.event.requiredFieldDiscovered
import io.spine.validation.findOption
import io.spine.validation.required.RequiredField.isSupported

/**
 * An abstract base for policies that control whether an ID field
 * should be validated as `(required)`.
 *
 * The ID of a signal message or an entity state is the first field
 * declared in the type, disregarding the index of the proto field.
 *
 * The ID field is assumed as required for commands and entity states,
 * unless it is specifically marked otherwise using the field options.
 *
 * Implementations define the ways of discovering signal and entity
 * state messages.
 */
internal abstract class RequiredIdPolicy : Policy<TypeDiscovered>(), ValidationPluginPart {

    /**
     * The validation config.
     */
    protected val config: ValidationConfig? by lazy {
        if (settingsAvailable()) {
            loadSettings<ValidationConfig>()
        } else {
            null
        }
    }

    /**
     * Controls whether the given ID [field] should be implicitly validated
     * as required.
     *
     * The method emits [RequiredFieldDiscovered] event if the following
     * conditions are met:
     *
     * 1. The field does not have the  `(required)` option applied explicitly.
     *   If it has, the field is handled by the [RequiredPolicy] policy then.
     * 2. The field type is supported by the option.
     *
     * The method emits [NoReaction] in case of violation of the above conditions.
     *
     * @param field The ID field.
     */
    fun withField(field: Field): EitherOf2<RequiredFieldDiscovered, NoReaction> {
        val requiredOption = field.findOption(OptionsProto.required)
        if (requiredOption != null) {
            return ignore()
        }

        val fieldTypeUnsupported = field.type.isSupported().not()
        if (fieldTypeUnsupported) {
            return ignore()
        }

        return requiredFieldDiscovered {
            id = field.ref
            errorMessage = ID_FIELD_MUST_BE_SET
            subject = field
        }.asA()
    }
}

/**
 * The error message template used for violations.
 *
 * Unlike the one provided by [IfMissingOption][io.spine.option.IfMissingOption],
 * this explicitly states that this is `The ID field`.
 */
private const val ID_FIELD_MUST_BE_SET = "The ID field `\${parent.type}.\${field.path}`" +
        " of the type `\${field.type}` must have a non-default value."
