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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.code.proto.FieldContext;
import io.spine.code.proto.FieldDeclaration;
import io.spine.logging.WithLogging;
import io.spine.option.OptionsProto;
import io.spine.validate.Constraint;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.BYTE_STRING;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.ENUM;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;
import static java.lang.String.format;

/**
 * An option that makes a field {@code required}.
 *
 * <p>If a {@code required} field is missing, an error is produced.
 */
@Immutable
public class Required extends FieldValidatingOption<Boolean> implements WithLogging {

    static final ImmutableSet<JavaType> CAN_BE_REQUIRED = ImmutableSet.of(
            MESSAGE, ENUM, STRING, BYTE_STRING
    );

    /**
     * Creates a new instance of this option.
     */
    Required() {
        super(OptionsProto.required);
    }

    /**
     * Creates a new instance.
     *
     * <p>If the specified parameter is {@code true}, a returned option always assumes a field to
     * be {@code required}, regardless of the field value.
     * If the specified parameter is {@code false}, a returned option checks the actual value.
     *
     * @param strict
     *         specifies if a field is assumed to be a required one regardless of the actual
     *         Protobuf option value
     * @return a new instance of the {@code Required} option
     */
    public static Required create(boolean strict) {
        return strict
               ? new AlwaysRequired()
               : new Required();
    }

    private boolean notAssumingRequired(FieldContext context) {
        var defaultValue = context.targetDeclaration().isId();
        return valueFrom(context.target())
                .orElse(defaultValue);
    }

    @Override
    public boolean shouldValidate(FieldContext context) {
        return notAssumingRequired(context);
    }

    /**
     * Produces warnings if the {@code required} option was applied incorrectly.
     *
     * <p>Examples of incorrect application include attempting to apply the option to a numeric
     * field.
     *
     * @param field
     *         a value that the option is applied to
     */
    void checkUsage(FieldDeclaration field) {
        var type = field.javaType();
        if (!CAN_BE_REQUIRED.contains(type) && field.isNotCollection()) {
            var isTheFirstField = field.descriptor().getIndex() == 0;
            if (isTheFirstField) {
                // The first field declared in a message type could be assumed as required
                // because by convention it is an ID field of the message.
                // If so, do not log the warning message for this field because ID fields
                // could be of any reasonable type.
                var messageClass = field.declaringType().javaClass();
                if (CommandMessage.class.isAssignableFrom(messageClass)
                        || EntityState.class.isAssignableFrom(messageClass)) {
                    return;
                }
            }
            var typeName = field.descriptor().getType().name();
            logger().atWarning().log(() -> format(
                    "The field `%s.%s` has the type %s and" +
                            " should not be declared as `(required)`.",
                    field.declaringType().name(), field.name(), typeName));
        }
    }

    @Override
    public Constraint constraintFor(FieldContext context) {
        checkUsage(context.targetDeclaration());
        var value = notAssumingRequired(context);
        return new RequiredConstraint(value, context.targetDeclaration());
    }
}
