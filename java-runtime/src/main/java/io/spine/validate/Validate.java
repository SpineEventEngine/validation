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

package io.spine.validate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.InlineMe;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.code.proto.FieldContext;
import io.spine.code.proto.FieldDeclaration;
import io.spine.logging.Logger;
import io.spine.logging.LoggingFactory;
import io.spine.protobuf.Diff;
import io.spine.type.KnownTypes;
import io.spine.type.MessageType;
import io.spine.type.TypeUrl;
import io.spine.validate.option.SetOnce;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.validate.RuntimeErrorPlaceholder.FIELD_PATH;
import static io.spine.validate.RuntimeErrorPlaceholder.PARENT_TYPE;
import static java.lang.String.format;

/**
 * This class provides general validation routines.
 */
public final class Validate {

    private static final String SET_ONCE_ERROR_MESSAGE =
            "Attempted to change the value of the field " +
                    "`${" + PARENT_TYPE + "}.${" + FIELD_PATH + "}` which has " +
                    "`(set_once) = true` and already has a non-default value.";
    private static final Logger<?> logger = LoggingFactory.forEnclosingClass();

    /** Prevents instantiation of this utility class. */
    private Validate() {
    }

    /**
     * Validates the given message according to its definition and throws
     * {@code ValidationException} if any constraints are violated.
     *
     * @throws ValidationException
     *         if the passed message does not satisfy the constraints
     *         set for it in its Protobuf definition
     * @deprecated please use {@link #check(Message)}
     */
    @Deprecated
    @InlineMe(replacement = "Validate.check(message)", imports = "io.spine.validate.Validate")
    public static void checkValid(Message message) throws ValidationException {
        check(message);
    }

    /**
     * Validates the given message according to its definition and returns it if so.
     *
     * @throws ValidationException
     *         if the passed message does not satisfy the constraints
     *         set for it in its Protobuf definition
     */
    @CanIgnoreReturnValue
    public static <M extends Message> M check(M message) throws ValidationException {
        checkNotNull(message);
        var violations = violationsOf(message);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
        return message;
    }

    /**
     * Validates the given message according to its definition and returns
     * the constraint violations, if any.
     *
     * @return violations of the validation rules or an empty list if the message is valid
     */
    @SuppressWarnings("ChainOfInstanceofChecks") // A necessity for covering more cases.
    public static List<ConstraintViolation> violationsOf(Message message) {
        var msg = message;
        if (message instanceof Any packed) {
            if (KnownTypes.instance().contains(TypeUrl.ofEnclosed(packed))) {
                msg = unpack(packed);
            } else {
                logger.atWarning().log(() -> format(
                    "Could not validate packed message of an unknown type `%s`.",
                    packed.getTypeUrl()));
            }
        }
        if (msg instanceof ValidatableMessage validatable) {
            var error = validatable.validate();
            return error.map(ValidationError::getConstraintViolationList)
                        .orElse(ImmutableList.of());
        }
        return validateAtRuntime(message);
    }

    private static List<ConstraintViolation> validateAtRuntime(Message message) {
        return validateAtRuntime(message, FieldContext.empty());
    }

    /**
     * Validates the given message ignoring the generated validation code.
     *
     * <p>Use {@link #violationsOf(Message)} over this method. It is declared {@code public} only
     * to be accessible in the generated code.
     *
     * @param message
     *         the message to validate
     * @param context
     *         the validation field context
     * @return violations of the validation rules or an empty list if the message is valid
     * @apiNote This method is used by the generated code, and as such needs to
     *         be {@code public}.
     */
    @Internal
    @SuppressWarnings("WeakerAccess") // see apiNote.
    public static List<ConstraintViolation>
    validateAtRuntime(Message message, FieldContext context) {
        var error = Constraints.of(MessageType.of(message), context)
                               .runThrough(new MessageValidator(message, context));
        var violations = error.map(ValidationError::getConstraintViolationList)
                              .orElse(ImmutableList.of());
        return violations;
    }

    /**
     * Validates the given message according to custom validation constraints.
     *
     * <p>If there are user-defined {@link io.spine.validate.option.ValidatingOptionFactory} in
     * the classpath, they are used to create validating options and assemble constraints. If there
     * are no such factories, this method always returns an empty list.
     *
     * @param message
     *         the message to validate
     * @return a list of violations; an empty list if the message is valid
     */
    public static List<ConstraintViolation> violationsOfCustomConstraints(Message message) {
        checkNotNull(message);
        var error = Constraints.onlyCustom(MessageType.of(message), FieldContext.empty())
                               .runThrough(new MessageValidator(message));
        var violations = error.map(ValidationError::getConstraintViolationList)
                              .orElse(ImmutableList.of());
        return violations;
    }

    /**
     * Checks that when transitioning a message state from {@code previous} to {@code current},
     * the {@code set_once} constrains are met and throws a {@link ValidationException} if
     * the value transition is not valid.
     *
     * @param previous
     *         the previous state of the message
     * @param current
     *         the new state of the message
     * @param <M>
     *         the type of the message
     * @throws ValidationException
     *          if the value transition is not valid
     * @deprecated the {@code set_once} constraint is enforced by the {@link Message} builder now.
     *              Just remove usages of this method without providing any replacement.
     */
    @Deprecated
    public static <M extends Message> void checkValidChange(M previous, M current) {
        checkNotNull(previous);
        checkNotNull(current);
        var setOnceViolations = validateChange(previous, current);
        if (!setOnceViolations.isEmpty()) {
            throw new ValidationException(setOnceViolations);
        }
    }

    /**
     * Checks that when transitioning a message state from {@code previous} to {@code current},
     * the {@code set_once} constrains are met.
     *
     * @param previous
     *         the previous state of the message
     * @param current
     *         the new state of the message
     * @param <M>
     *         the type of the message
     * @return a set of constraint violations, if the transaction is invalid,
     *         an empty set otherwise
     * @deprecated the {@code set_once} constraint is enforced by the {@link Message} builder now.
     *              Just remove usages of this method without providing any replacement.
     */
    @Deprecated
    @SuppressWarnings("WeakerAccess") // part of public API.
    public static <M extends Message>
    ImmutableSet<ConstraintViolation> validateChange(M previous, M current) {
        checkNotNull(previous);
        checkNotNull(current);

        var diff = Diff.between(previous, current);
        var violations = current.getDescriptorForType().getFields()
                .stream()
                .map(FieldDeclaration::new)
                .filter(Validate::isNonOverridable)
                .filter(diff::contains)
                .filter(field -> {
                    var fieldValue = previous.getField(field.descriptor());
                    return !field.isDefault(fieldValue);
                })
                .map(Validate::violatedSetOnce)
                .collect(toImmutableSet());
        return violations;
    }

    /**
     * Checks if the given field, once set, may not be changed.
     *
     * <p>This property is defined by the {@code (set_once)} option. If the option is set to
     * {@code true} on a non-{@code repeated} and non-{@code map} field, this field is
     * <strong>non-overridable</strong>.
     *
     * <p>Logs if the option is set but the field is {@code repeated} or a {@code map}.
     *
     * @param field
     *         the field to check
     * @return {@code true} if the field is neither {@code repeated} nor {@code map} and is
     *         {@code (set_once)}
     */
    private static boolean isNonOverridable(FieldDeclaration field) {
        checkNotNull(field);

        var marked = markedSetOnce(field);
        if (marked) {
            var setOnceInapplicable = field.isCollection();
            if (setOnceInapplicable) {
                onSetOnceMisuse(field);
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean markedSetOnce(FieldDeclaration declaration) {
        var setOnceDeclaration = SetOnce.from(declaration.descriptor());
        boolean setOnceValue = setOnceDeclaration.orElse(false);
        return setOnceValue;
    }

    private static void onSetOnceMisuse(FieldDeclaration field) {
        var fieldName = field.name();
        logger.atError().log(() -> format(
                "Error found in `%s`. " +
                        "Repeated and map fields cannot be marked as `(set_once) = true`.",
                fieldName));
    }

    private static ConstraintViolation violatedSetOnce(FieldDeclaration declaration) {
        var declaringTypeName = declaration.declaringType().name().value();
        var fieldName = declaration.name().value();
        var message = TemplateString.newBuilder()
                .setWithPlaceholders(SET_ONCE_ERROR_MESSAGE)
                .putPlaceholderValue(PARENT_TYPE.toString(), declaringTypeName)
                .putPlaceholderValue(FIELD_PATH.toString(), fieldName)
                .build();
        var violation = ConstraintViolation.newBuilder()
                .setMessage(message)
                .setFieldPath(declaration.name().asPath())
                .setTypeName(declaration.declaringType().name().value())
                .build();
        return violation;
    }
}
