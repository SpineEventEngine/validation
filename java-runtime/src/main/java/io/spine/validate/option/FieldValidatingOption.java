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

package io.spine.validate.option;

import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.ImmutableTypeParameter;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import io.spine.code.proto.FieldContext;
import io.spine.code.proto.FieldOption;

import java.util.Optional;

import static java.lang.String.format;

/**
 * An option that validates a field.
 *
 * <p>Validating options impose constraint on fields that they are applied to.
 *
 * @param <T>
 *         type of value held by this option
 */
@Immutable
public abstract class FieldValidatingOption<@ImmutableTypeParameter T>
        extends FieldOption<T>
        implements ValidatingOption<T, FieldContext, FieldDescriptor> {

    /** Specifies the extension that corresponds to this option. */
    protected FieldValidatingOption(GeneratedExtension<FieldOptions, T> optionExtension) {
        super(optionExtension);
    }

    /**
     * Returns a value of the option.
     *
     * @apiNote Should only be called by subclasses in circumstances that assume presence of
     *         the option. For all other cases please refer to
     *         {@link #valueFrom(FieldContext)}.
     */
    protected T optionValue(FieldContext field) throws IllegalStateException {
        var option = valueFrom(field);
        return option.orElseThrow(() -> {
            var descriptor = extension().getDescriptor();

            var fieldName = field.targetDeclaration()
                                 .name()
                                 .value();
            var containingTypeName = descriptor.getContainingType()
                                               .getName();
            return couldNotGetOptionValueFrom(fieldName, containingTypeName);
        });
    }

    private IllegalStateException couldNotGetOptionValueFrom(String fieldName,
                                                             String containingTypeName) {
        var optionName = extension().getDescriptor().getName();
        var message = format("Could not get value of option %s from field %s in message %s.",
                             optionName,
                             fieldName,
                             containingTypeName);
        return new IllegalStateException(message);
    }

    /**
     * Takes the value of the option from the given descriptor, given the specified context.
     *
     * @param context
     *         context of the field
     * @return an {@code Optional} with an option value if such exists, otherwise an empty
     *         {@code Optional}
     * @apiNote Use this in favour of {@link
     *         FieldOption#optionsFrom(com.google.protobuf.Descriptors.FieldDescriptor)
     *         optionsFrom(FieldDescriptor)} when {@code FieldContext} matters.
     */
    public Optional<T> valueFrom(FieldContext context) {
        return valueFrom(context.target());
    }

    /**
     * Returns {@code true} if this option exists for the specified field, {@code false} otherwise.
     *
     * @param context
     *         the value to be validated
     */
    public boolean shouldValidate(FieldContext context) {
        return valueFrom(context).isPresent();
    }
}
