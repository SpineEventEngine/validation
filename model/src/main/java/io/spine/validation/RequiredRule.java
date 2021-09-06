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
import io.spine.protodata.PrimitiveType;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.Ast.typeUrl;
import static io.spine.validation.ComparisonOperator.NOT_EQUAL;
import static java.lang.String.format;

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
    static SimpleRule forField(Field field, String errorMessage) {
        checkNotNull(field);
        Value unsetValue = UnsetValue.forField(field)
                                     .orElseThrow(() -> doesNotSupportRequired(field));
        @SuppressWarnings({"DuplicateStringLiteralInspection", "RedundantSuppression"})
        // Duplication in generated code.
        SimpleRule rule = SimpleRule
                .newBuilder()
                .setErrorMessage(errorMessage)
                .setField(field.getName())
                .setOperator(NOT_EQUAL)
                .setOtherValue(unsetValue)
                .setDistribute(false)
                .vBuild();
        return rule;
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
