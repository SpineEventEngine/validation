/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.ImmutableSet;
import io.spine.code.proto.FieldContext;
import io.spine.validation.option.FieldValidatingOption;
import io.spine.validation.option.StandardOptionFactory;
import io.spine.validation.option.ValidatingOptionFactory;
import io.spine.validation.option.ValidatingOptionsLoader;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * A factory of field validation {@link Constraint}s.
 */
final class FieldConstraints {

    private static final ImmutableSet<ValidatingOptionFactory> allFactories =
            ValidatingOptionsLoader.INSTANCE.implementations();
    private static final ImmutableSet<ValidatingOptionFactory> customFactories =
            allFactories.stream()
                    .filter(factory -> !(factory instanceof StandardOptionFactory))
                    .collect(toImmutableSet());

    /**
     * Prevents the utility class instantiation.
     */
    private FieldConstraints() {
    }

    /**
     * Assembles {@link Constraint}s for a given field.
     *
     * @param field
     *         field to validate
     * @return validation constraints
     */
    static Stream<Constraint> of(FieldContext field) {
        return findConstraints(field, allFactories);
    }

    static boolean customFactoriesExist() {
        return !customFactories.isEmpty();
    }

    static Stream<Constraint> customConstraintsFor(FieldContext field) {
        return findConstraints(field, customFactories);
    }

    // Assembles many options and option factories for all field types.
    private static Stream<Constraint>
    findConstraints(FieldContext field, ImmutableSet<ValidatingOptionFactory> factories) {
        checkNotNull(field);
        var declaration = field.targetDeclaration();
        var type = declaration.javaType();
        return switch (type) {
            case INT -> constraintsFrom(factories, ValidatingOptionFactory::forInt, field);
            case LONG -> constraintsFrom(factories, ValidatingOptionFactory::forLong, field);
            case FLOAT -> constraintsFrom(factories, ValidatingOptionFactory::forFloat, field);
            case DOUBLE -> constraintsFrom(factories, ValidatingOptionFactory::forDouble, field);
            case BOOLEAN -> constraintsFrom(factories, ValidatingOptionFactory::forBoolean, field);
            case STRING -> constraintsFrom(factories, ValidatingOptionFactory::forString, field);
            case BYTE_STRING ->
                    constraintsFrom(factories, ValidatingOptionFactory::forByteString, field);
            case ENUM -> constraintsFrom(factories, ValidatingOptionFactory::forEnum, field);
            case MESSAGE -> constraintsFrom(factories, ValidatingOptionFactory::forMessage, field);
        };
    }

    private static Stream<Constraint>
    constraintsFrom(ImmutableSet<ValidatingOptionFactory> factories,
                    Function<ValidatingOptionFactory, Set<FieldValidatingOption<?>>> selector,
                    FieldContext field) {
        return factories.stream()
                .map(selector)
                .flatMap(Set::stream)
                .filter(option -> option.shouldValidate(field))
                .map(option -> option.constraintFor(field));
    }
}
