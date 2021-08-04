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
import io.spine.protodata.FieldOptionDiscovered;
import io.spine.protodata.Option;
import io.spine.protodata.PrimitiveType;
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.SimpleRuleAdded;

import static io.spine.option.OptionsProto.required;
import static io.spine.protodata.Ast.typeUrl;
import static io.spine.validation.ComparisonOperator.NOT_EQUAL;
import static io.spine.validation.Options.is;
import static io.spine.validation.SourceFiles.findField;
import static java.lang.String.format;

/**
 * A {@link Policy} which controls whether or not a field should be validated as {@code required}.
 *
 * <p>Whenever a field option is discovered, if that option is the {@code required} option, and
 * the value is {@code true}, and the field type supports such validation, a validation rule
 * is added. If any of these conditions are not met, nothing happens.
 */
final class RequiredRulePolicy extends Policy<FieldOptionDiscovered> {

    @Override
    @React
    public EitherOf2<SimpleRuleAdded, Nothing> whenever(@External FieldOptionDiscovered event) {
        Option option = event.getOption();
        if (!is(option, required)) {
            return EitherOf2.withB(nothing());
        }
        Field field = findField(event.getField(), event.getType(), event.getFile(), this);
        return EitherOf2.withA(requiredRule(field));
    }

    private static SimpleRuleAdded requiredRule(Field field) {
        Value unsetValue = UnsetValue.forField(field)
                                     .orElseThrow(() -> doesNotSupportRequired(field));
        @SuppressWarnings({"DuplicateStringLiteralInspection", "RedundantSuppression"})
            // Duplication in generated code.
        SimpleRule rule = SimpleRule
                .newBuilder()
                .setErrorMessage("Field must be set.")
                .setField(field.getName())
                .setOperator(NOT_EQUAL)
                .setOtherValue(unsetValue)
                .vBuild();
        return SimpleRuleAdded
                .newBuilder()
                .setType(field.getDeclaringType())
                .setRule(rule)
                .vBuild();
    }

    private static IllegalStateException doesNotSupportRequired(Field field) {
        String fieldName = field.getName()
                                .getValue();
        String typeUrl = typeUrl(field.getDeclaringType());
        PrimitiveType type = field.getType()
                                  .getPrimitive();
        return new IllegalStateException(format(
                "Field `%s.%s` of type `%s` does not support `(required)` validation.",
                typeUrl, fieldName, type
        ));
    }
}
