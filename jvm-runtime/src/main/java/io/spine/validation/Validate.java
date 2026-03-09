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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.type.KnownTypes;
import io.spine.type.TypeUrl;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * This class provides general validation routines.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr" /*
    We do not want the dependency of Validation Runtime on Spine Logging.
    So we use `System.err` for warnings and errors. */
)
public final class Validate {

    /** Prevents instantiation of this utility class. */
    private Validate() {
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
        checkNotNull(message);
        var msg = message;
        if (message instanceof Any packed) {
            if (KnownTypes.instance().contains(TypeUrl.ofEnclosed(packed))) {
                msg = unpack(packed);
            } else {
                System.err.printf(
                    "Could not validate packed message of an unknown type `%s`.%n",
                    packed.getTypeUrl());
            }
        }
        if (msg instanceof ValidatableMessage validatable) {
            var error = validatable.validate();
            return error.map(ValidationError::getConstraintViolationList)
                        .orElse(ImmutableList.of());
        } else {
            return ValidatorRegistry.validate(msg);
        }
    }
}
