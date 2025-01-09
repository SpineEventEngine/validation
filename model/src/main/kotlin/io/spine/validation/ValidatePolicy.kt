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
import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.File
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.event.FieldExited
import io.spine.protodata.ast.qualifiedName
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.query.select
import io.spine.server.tuple.EitherOf2
import io.spine.validation.event.RuleAdded
import io.spine.validation.event.simpleRuleAdded

/**
 * A policy which, upon encountering a field with the `(validate)` option, generates
 * a validation rule.
 *
 * The validation rule enforces recursive validation for the associated message field.
 *
 * If the field is a list or a map, all the elements (values of map entries) are validated.
 *
 * If the message field is invalid, the containing message is invalid as well.
 */
internal class ValidatePolicy : ValidationPolicy<FieldExited>() {

    @React
    override fun whenever(@External event: FieldExited): EitherOf2<RuleAdded, NoReaction> {
        val id = fieldId {
            name = event.field
            type = event.type
        }
        val field = select<ValidatedField>().findById(id)
        if (field != null) {
            ensureMessageField(event.field, event.type, event.file)
        }
        val shouldValidate = field != null && field.validate
        if (!shouldValidate) {
            return ignore()
        }
        val rule = SimpleRule(
            field = event.field,
            customFeature = RecursiveValidation.getDefaultInstance(),
            description = "A message field is validated by its validation rules. " +
                    "If the field is invalid, the container message is invalid as well.",
            errorMessage = "", // `(validate)` doesn't create an error on its own.
            distribute = true
        )
        return simpleRuleAdded {
            type = event.type
            this.rule = rule
        }.asA()
    }

    private fun ensureMessageField(fieldName: FieldName, typeName: TypeName, file: File) {
        val field = findField(fieldName, typeName, file)
        if (!field.type.refersToMessage()) {
            error(
                "The field `${typeName.qualifiedName}.${fieldName.value}` does not refer" +
                        " to a message type and, therefore, cannot have the `$VALIDATE` option.",
            )
        }
    }
}
