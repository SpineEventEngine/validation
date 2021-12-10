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
import io.spine.protodata.PrimitiveType;
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;

import java.util.Optional;

import static io.spine.protodata.Ast.typeUrl;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validation.SourceFiles.findField;

/**
 * A {@link Policy} which controls whether or not a field should be validated as {@code required}.
 *
 * <p>Whenever a field option is discovered, if that option is the {@code required} option, and
 * the value is {@code true}, and the field type supports such validation, a validation rule
 * is added. If any of these conditions are not met, nothing happens.
 */
final class RequiredPolicy extends ValidationPolicy<FieldExited> {

    @Override
    @React
    protected EitherOf2<RuleAdded, Nothing> whenever(@External FieldExited event) {
        FieldId id = FieldId
                .newBuilder()
                .setName(event.getField())
                .setType(event.getType())
                .build();
        Optional<RequiredField> field = select(RequiredField.class)
                .withId(id);
        if (field.map(RequiredField::getRequired).orElse(false)) {
            Field declaration = findField(event.getField(),
                                          event.getType(),
                                          event.getFile(),
                                          this);
            return EitherOf2.withA(requiredRule(declaration, field.get()));
        }
        return withNothing();
    }

    private static RuleAdded requiredRule(Field declaration, RequiredField field) {
        Rule rule = RequiredRule.forField(declaration, field.getErrorMessage())
                                .orElseThrow(() -> doesNotSupportRequired(declaration));
        return Rules.toEvent(rule, declaration.getDeclaringType());
    }

    private static IllegalStateException doesNotSupportRequired(Field field) {
        String fieldName = field.getName()
                                .getValue();
        String typeUrl = typeUrl(field.getDeclaringType());
        PrimitiveType type = field.getType()
                                  .getPrimitive();
        return newIllegalStateException(
                "Field `%s.%s` of type `%s` does not support `(required)` validation.",
                typeUrl, fieldName, type
        );
    }
}
