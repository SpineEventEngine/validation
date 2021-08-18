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

import com.google.protobuf.Message;
import io.spine.protodata.FieldName;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;

/**
 * A factory of {@link SimpleRule}s.
 */
public final class SimpleRules {

    /**
     * Prevents the utility class instantiation.
     */
    private SimpleRules() {
    }

    /**
     * Creates a {@link SimpleRule} with a custom operator.
     *
     * @param field
     *         the target field
     * @param customFeature
     *         the feature message describing the custom operator
     * @param description
     *         the human-readable text description of the feature
     * @param errorMessage
     *         the error message for the case of violation
     * @return a new rule
     */
    public static SimpleRule withCustom(
            FieldName field,
            Message customFeature,
            String description,
            String errorMessage,
            boolean distribute
    ) {
        checkNotNull(field);
        checkNotNull(customFeature);
        checkNotEmptyOrBlank(description);
        checkNotEmptyOrBlank(errorMessage);
        CustomOperator operator = CustomOperator
                .newBuilder()
                .setDescription(description)
                .setFeature(pack(customFeature))
                .build();
        Value other = Values.from(customFeature);
        SimpleRule rule = SimpleRule
                .newBuilder()
                .setField(field)
                .setCustomOperator(operator)
                .setErrorMessage(errorMessage)
                .setIgnoredIfUnset(true)
                .setOtherValue(other)
                .setDistribute(distribute)
                .build();
        return rule;
    }
}
