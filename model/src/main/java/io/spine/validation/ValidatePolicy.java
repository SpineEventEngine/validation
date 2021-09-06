/*
 * Copyright 2021, TeamDev. All rights reserved.
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
import io.spine.protodata.Field;
import io.spine.protodata.FieldExited;
import io.spine.protodata.FieldName;
import io.spine.protodata.FilePath;
import io.spine.protodata.TypeName;
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.SimpleRuleAdded;

import java.util.Optional;

import static io.spine.protodata.Ast.qualifiedName;
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
final class ValidatePolicy extends Policy<FieldExited> {

    @Override
    @React
    public EitherOf2<SimpleRuleAdded, Nothing> whenever(@External FieldExited event) {
        FieldId id = FieldId
                .newBuilder()
                .setName(event.getField())
                .setType(event.getType())
                .build();
        Optional<ValidatedField> field = select(ValidatedField.class)
                .withId(id);
        if (field.isPresent()) {
            checkMessage(event.getField(), event.getType(), event.getFile());
        }
        if (field.map(ValidatedField::getValidate)
                 .orElse(false)) {
            SimpleRule rule = SimpleRules.withCustom(
                    event.getField(),
                    RecursiveValidation.getDefaultInstance(),
                    "Message field is validated by its validation rules. " +
                            "If the field is invalid, the container message is invalid as well.",
                    field.get()
                         .getErrorMessage(),
                    true);
            return EitherOf2.withA(SimpleRuleAdded
                                           .newBuilder()
                                           .setType(event.getType())
                                           .setRule(rule)
                                           .build());

        }
        return EitherOf2.withB(nothing());
    }

    private void checkMessage(FieldName fieldName, TypeName typeName, FilePath file) {
        Field field = findField(fieldName, typeName, file, this);
        if (!field.getType().hasMessage()) {
            throw newIllegalStateException(
                    "Field `%s.%s` is not a message field and, " +
                            "therefore, should not be marked with `validate`.",
                    qualifiedName(typeName),
                    fieldName.getValue());
        }
    }
}
