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

import io.spine.core.External
import io.spine.core.Where
import io.spine.option.IfHasDuplicatesOption
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.File
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.ref
import io.spine.protodata.check
import io.spine.protodata.plugin.Policy
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.event.DistinctFieldDiscovered
import io.spine.validation.event.distinctFieldDiscovered

/**
 * Controls whether a field should be validated as `(distinct)`.
 *
 * Whenever a field marked with `(distinct)` option is discovered, emits
 * [DistinctFieldDiscovered] event if the following conditions are met:
 *
 * 1. The field type is `repeated` or `map`.
 * 2. The option value is `true`.
 *
 * If (1) is violated, the policy reports a compilation error.
 *
 * Violation of (2) means that the `(distinct)` option is applied correctly,
 * but disabled. In this case, the policy emits [NoReaction] because we
 * actually have a non-distinct field, marked with `(distinct)`.
 */
internal class DistinctPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = DISTINCT)
        event: FieldOptionDiscovered
    ): EitherOf2<DistinctFieldDiscovered, NoReaction> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        if (!event.option.boolValue) {
            return ignore()
        }

        val message = resolveErrorMessage<IfHasDuplicatesOption>(field)
        return distinctFieldDiscovered {
            id = field.ref
            errorMessage = message
            subject = field
        }.asA()
    }
}

private fun checkFieldType(field: Field, file: File) =
    Compilation.check(field.type.isSupported(), file, field.span) {
        "The field type `${field.type.name}` of `${field.qualifiedName}` is not supported" +
                " by the `($DISTINCT)` option. This options supports `map` and `repeated` fields."
    }

private fun FieldType.isSupported(): Boolean = isMap || isList
