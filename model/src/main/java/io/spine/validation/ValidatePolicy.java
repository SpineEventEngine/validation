/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.validation;

import io.spine.core.External;
import io.spine.protodata.FieldName;
import io.spine.protodata.File;
import io.spine.protodata.TypeName;
import io.spine.protodata.event.FieldExited;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.RuleAdded;
import io.spine.validation.event.SimpleRuleAdded;

import static io.spine.protodata.TypeNames.getQualifiedName;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validation.SourceFiles.findField;

/**
 * A policy which, upon encountering a field with the {@code (validate)} option, generates
 * a validation rule.
 *
 * <p>The validation rule enforces recursive validation for the associated message field.
 *
 * <p>If the field is a list or a map, all the elements (values, in case of the map) are validated.
 *
 * <p>If the message field is invalid, the containing message is invalid as well.
 */
final class ValidatePolicy extends ValidationPolicy<FieldExited> {

    @Override
    @React
    protected EitherOf2<RuleAdded, Nothing> whenever(@External FieldExited event) {
        var id = FieldId.newBuilder()
                .setName(event.getField())
                .setType(event.getType())
                .build();
        var field = select(ValidatedField.class).findById(id);
        if (field != null) {
            ensureMessageField(event.getField(), event.getType(), event.getFile());
        }
        var shouldValidate = field != null && field.getValidate();
        if (!shouldValidate) {
            return noReaction();
        }
        var rule = SimpleRules.withCustom(
                event.getField(),
                RecursiveValidation.getDefaultInstance(),
                "Message field is validated by its validation rules. " +
                        "If the field is invalid, the container message is invalid as well.",
                field.getErrorMessage(),
                true);
        return EitherOf2.withA(
                SimpleRuleAdded.newBuilder()
                        .setType(event.getType())
                        .setRule(rule)
                        .build()
        );
    }

    private void ensureMessageField(FieldName fieldName, TypeName typeName, File file) {
        var field = findField(fieldName, typeName, file, this);
        if (!field.getType().hasMessage()) {
            throw newIllegalStateException(
                    "Field `%s.%s` is not a message field and, " +
                            "therefore, should not be marked with `validate`.",
                    getQualifiedName(typeName),
                    fieldName.getValue()
            );
        }
    }
}
