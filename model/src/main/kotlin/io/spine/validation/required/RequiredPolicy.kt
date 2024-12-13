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

import io.spine.core.External
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.declaringFile
import io.spine.protodata.ast.event.FieldExited
import io.spine.protodata.ast.qualifiedName
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.query.select
import io.spine.server.tuple.EitherOf2
import io.spine.validation.REQUIRED
import io.spine.validation.RequiredField
import io.spine.validation.ValidationPolicy
import io.spine.validation.event.RuleAdded
import io.spine.validation.fieldId
import io.spine.validation.findField
import io.spine.validation.toEvent

/**
 * A [ValidationPolicy] which controls whether a field should be validated as `required`.
 *
 * Whenever a field traversal is finished, add the validation rule if
 * all of these conditions are met:
 *   1. The field has the `(required)` option.
 *   2. The value of the option is `true`.
 *   2. The field type allows checking if a value is set or not.
 *
 * If any of these conditions are not met, nothing happens.
 */
internal class RequiredPolicy : ValidationPolicy<FieldExited>() {

    @React
    override fun whenever(@External event: FieldExited): EitherOf2<RuleAdded, NoReaction> {
        val declaringType = event.type
        val fieldName = event.field
        val id = fieldId {
            name = fieldName
            type = declaringType
        }
        val field = select<RequiredField>().findById(id)
        if (field != null && field.required) {
            val declaration = findField(fieldName, declaringType, event.file)
            val rule = requiredRule(declaration, field)
            return rule.asA()
        }
        return ignore()
    }

    private fun requiredRule(declaration: Field, field: RequiredField): RuleAdded {
        val rule = RequiredRule.forField(declaration, field.errorMessage)
            ?: throwDoesNotSupportRequired(declaration)
        return rule.toEvent(declaration.declaringType)
    }

    private fun throwDoesNotSupportRequired(field: Field): Nothing {
        val file = field.declaringFile(typeSystem!!)
        val fieldName = field.name.value
        val declaringType = field.declaringType.qualifiedName
        val type: PrimitiveType = field.type.primitive
        Compilation.error(
            file,
            field.span.startLine, field.span.startColumn,
            "The field `${declaringType}.${fieldName}` of the type `${type}`" +
                    " does not support `($REQUIRED)` validation.",
        )
    }
}

