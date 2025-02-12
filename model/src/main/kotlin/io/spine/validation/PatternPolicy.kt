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
import io.spine.option.PatternOption
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.File
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isSingular
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.unpack
import io.spine.protodata.plugin.Policy
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.validation.core.asJust
import io.spine.validation.event.PatternFieldDiscovered
import io.spine.validation.event.patternFieldDiscovered
import io.spine.validation.protodata.compilationError

/**
 * Controls whether a field should be validated with the `(pattern)`.
 *
 * Whenever a filed marked with `(pattern)` option is discovered, emits
 * [PatternFieldDiscovered] if the option supports the field type.
 * Otherwise, the policy reports a compilation error.
 */
internal class PatternPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = PATTERN)
        event: FieldOptionDiscovered
    ): Just<PatternFieldDiscovered> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, file)

        val option = event.option.unpack<PatternOption>()
        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return patternFieldDiscovered {
            id = fieldId {
                type = field.declaringType
                name = field.name
            }
            errorMessage = message
            pattern = option.regex
            modifier = option.modifier
            subject = field
        }.asJust()
    }
}

private fun checkFieldType(field: Field, file: File) {
    val type = field.type
    if (!(type.isSingularString || type.isRepeatedString)) {
        compilationError(file, field.span) {
            "The field type `${field.type}` of `${field.qualifiedName}` is not supported " +
                    "by the `($PATTERN)` option."
        }
    }
}

/**
 * Tells if this [FieldType] represents a `repeated string` field.
 *
 * The property is `public` because the option generator also needs it.
 */
public val FieldType.isRepeatedString: Boolean
    get() = isList && list.primitive == TYPE_STRING

/**
 * Tells if this [FieldType] represents a `string` field.
 *
 * The property is `public` because the option generator also needs it.
 */
public val FieldType.isSingularString: Boolean
    get() = isSingular && primitive == TYPE_STRING
