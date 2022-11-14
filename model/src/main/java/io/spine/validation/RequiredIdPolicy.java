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

import io.spine.protodata.Field;
import io.spine.protodata.event.TypeExited;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.RuleAdded;

import static io.spine.validation.RequiredRule.isRequired;
import static io.spine.validation.Rules.toEvent;
import static java.lang.String.format;

/**
 * A policy which defines validation rules for ID fields.
 *
 * <p>An ID field of a signal message or an entity is always required unless the used explicitly
 * specifies otherwise.
 *
 * <p>Implementations define the ways of discovering signal and entity state messages.
 */
abstract class RequiredIdPolicy extends ValidationPolicy<TypeExited> {

    /**
     * Given an ID field, generates the required rule event.
     *
     * <p>If the field is marked with {@code (required) = false}, no rule is generated.
     *
     * @param field
     *         the ID field
     * @return a required rule event or {@code Nothing} if the ID field is not required
     */
    final EitherOf2<RuleAdded, Nothing> withField(Field field) {
        if (!isRequired(field, true)) {
            return withNothing();
        }
        var errorMessage = format("ID field `%s` must be set.", field.getName()
                                                                     .getValue());
        var rule = RequiredRule.forField(field, errorMessage);
        if (rule.isEmpty()) {
            return withNothing();
        }
        return EitherOf2.withA(toEvent(rule.get(), field.getDeclaringType()));
    }
}
