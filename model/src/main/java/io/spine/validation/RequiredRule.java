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
import io.spine.option.IfMissingOption;
import io.spine.protodata.ast.Field;
import io.spine.protodata.value.Value;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.option.OptionsProto.required;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protodata.ast.Fields.isList;
import static io.spine.protodata.ast.Fields.isMap;
import static io.spine.validate.Diags.Required.collectionErrorMsg;
import static io.spine.validate.Diags.Required.singularErrorMsg;
import static io.spine.validation.ComparisonOperator.NOT_EQUAL;
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
    static Optional<Rule> forField(Field field, String errorMessage) {
        checkNotNull(field);
        var unsetValue = UnsetValue.forField(field);
        if (unsetValue.isEmpty()) {
            return Optional.empty();
        }
        var integratedRule = rule(
                field, unsetValue.get(), errorMessage, singularErrorMsg, false
        );
        if (!(isList(field) || isMap(field))) {
            return Optional.of(wrap(integratedRule));
        }
        var singularUnsetValue = UnsetValue.singular(field.getType());
        if (singularUnsetValue.isEmpty()) {
            return Optional.of(wrap(integratedRule));
        }
        var collectionRule = collectionRule(integratedRule, errorMessage);
        return Optional.of(collectionRule);
    }

    private static Rule collectionRule(SimpleRule integratedRule, String errorMessage) {
        var msg = collectionErrorMessage(errorMessage);
        var withCustomErrorMessage = integratedRule.toBuilder()
                .setErrorMessage(msg)
                .build();
        return wrap(withCustomErrorMessage);
    }

    /**
     * This method provides a separate default message for the case of a collection field
     * marked as `(required)`.
     *
     * <p>Singular fields obtain the default error message as a value of the `(default_message)`
     * option set for `IfMissing` option type.
     *
     * <p>Event if a custom error message is not set by a {@code (if_missing)} field option,
     * we want to have a different <em>default</em> message for collection fields,
     * so that the user can find an error quicker.
     *
     * <p>If a custom error message is set, we use it as is.
     *
     * @param errorMessage
     *         the error message coming from the {@link RequiredPolicy} which is producing
     *         the rule while this method is called
     * @return an error message to be used for the collection field
     */
    private static String collectionErrorMessage(String errorMessage) {
        if (errorMessage.isEmpty()) {
            return collectionErrorMsg;
        }
        var defaultMessage = DefaultErrorMessage.from(IfMissingOption.getDescriptor());
        if (errorMessage.equals(defaultMessage)) {
            return collectionErrorMsg;
        }
        return errorMessage;
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
                .build();
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
