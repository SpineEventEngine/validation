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

import io.spine.protodata.Field;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.Ast.isRepeated;
import static io.spine.validation.ComparisonOperator.NOT_EQUAL;

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
        Optional<Value> unsetValue = UnsetValue.forField(field);
        if (!unsetValue.isPresent()) {
            return Optional.empty();
        }
        SimpleRule integratedRule = SimpleRule
                .newBuilder()
                .setErrorMessage(errorMessage)
                .setField(field.getName())
                .setOperator(NOT_EQUAL)
                .setOtherValue(unsetValue.get())
                .setDistribute(true)
                .vBuild();
        if (!isRepeated(field)) {
            return Optional.of(wrap(integratedRule));
        }
        Optional<Value> singularUnsetValue = UnsetValue.singular(field.getType());
        if (!singularUnsetValue.isPresent()) {
            return Optional.of(wrap(integratedRule));
        }
        SimpleRule differentialRule = SimpleRule
                .newBuilder()
                .setErrorMessage(errorMessage)
                .setField(field.getName())
                .setOperator(NOT_EQUAL)
                .setOtherValue(singularUnsetValue.get())
                .setDistribute(true)
                .vBuild();
        CompositeRule composite = CompositeRule.newBuilder()
                .setLeft(wrap(integratedRule))
                .setOperator(LogicalOperator.AND)
                .setRight(wrap(differentialRule))
                .build();
        return Optional.of(wrap(composite));
    }

    private static Rule wrap(SimpleRule r) {
        return Rule.newBuilder()
                .setSimple(r)
                .build();
    }

    private static Rule wrap(CompositeRule r) {
        return Rule.newBuilder()
                .setComposite(r)
                .build();
    }
}
