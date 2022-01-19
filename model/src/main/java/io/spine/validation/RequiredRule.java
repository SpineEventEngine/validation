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

import com.google.protobuf.BoolValue;
import io.spine.protodata.Field;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.option.OptionsProto.required;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protodata.Ast.isRepeated;
import static io.spine.validation.ComparisonOperator.NOT_EQUAL;
import static io.spine.validation.LogicalOperator.AND;
import static io.spine.validation.Options.is;
import static io.spine.validation.Rules.wrap;

/**
 * A factory of {@link SimpleRule}s which represent the {@code (required)} constraint.
 */
final class RequiredRule {

    /**
     * Prevents the utility class instantiation.
     */
    private RequiredRule() {
    }

    /**
     * Creates a rule for the given field to be required.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection", "RedundantSuppression"})
    // Duplication in generated code.
    static Optional<Rule> forField(Field field, String errorMessage) {
        checkNotNull(field);
        var unsetValue = UnsetValue.forField(field);
        if (unsetValue.isEmpty()) {
            return Optional.empty();
        }
        var integratedRule = rule(
                field, unsetValue.get(), errorMessage, "Field must be set.", false
        );
        if (!isRepeated(field)) {
            return Optional.of(wrap(integratedRule));
        }
        var singularUnsetValue = UnsetValue.singular(field.getType());
        if (singularUnsetValue.isEmpty()) {
            return Optional.of(wrap(integratedRule));
        }
        var differentialRule = rule(
                field, singularUnsetValue.get(), errorMessage, "", true
        );
        var composite = collectionRule(field, integratedRule, differentialRule);
        return Optional.of(wrap(composite));
    }

    private static CompositeRule collectionRule(Field field,
                                                SimpleRule integratedRule,
                                                SimpleRule differentialRule) {
        @SuppressWarnings("DuplicateStringLiteralInspection")
        var composite = CompositeRule.newBuilder()
                .setLeft(wrap(integratedRule))
                .setOperator(AND)
                .setRight(wrap(differentialRule))
                .setErrorMessage("Collection must not be empty and cannot contain default values.")
                .setField(field.getName())
                .build();
        return composite;
    }

    private static SimpleRule rule(Field field,
                                   Value value,
                                   String errorMessage,
                                   String defaultErrorMessage,
                                   boolean distibute) {
        var msg = errorMessage.isEmpty() ? defaultErrorMessage : errorMessage;
        return SimpleRule.newBuilder()
                .setErrorMessage(msg)
                .setField(field.getName())
                .setOperator(NOT_EQUAL)
                .setOtherValue(value)
                .setDistribute(distibute)
                .vBuild();
    }

    /**
     * Checks if the given field is required.
     *
     * @param field
     *         the field
     * @param byDefault
     *         the default value
     * @return {@code true} if the field is marked with {@code (required) = true} or if
     *         the {@code byDefault} is {@code true}, {@code false} otherwise
     */
    static boolean isRequired(Field field, boolean byDefault) {
        return field.getOptionList()
                .stream()
                .filter(opt -> is(opt, required))
                .findAny()
                .map(opt -> unpack(opt.getValue(), BoolValue.class).getValue())
                .orElse(byDefault);
    }
}
