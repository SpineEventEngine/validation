/*
 * Copyright 2026, TeamDev. All rights reserved.
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
import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;

import java.util.List;

/**
 * Implementation base for generated message builders.
 *
 * <p>This interface defines a method {@link #build()} that validates the built message
 * before returning it to the user.
 *
 * <p>If a user specifically needs to skip validation, they should use
 * {@link #buildPartial()} to make the intent explicit.
 *
 * <p>To check the current content of the builder for validity without
 * obtaining the built message, use {@link #validate()}.
 *
 * @param <M>
 *         the type of the message to build
 */
@GeneratedMixin
public interface ValidatingBuilder<M extends Message> extends Message.Builder {

    /**
     * Constructs the message and validates it according to the constraints
     * declared in Protobuf.
     *
     * @return the built message
     * @throws ValidationException
     *         if the message is invalid
     */
    @Override
    @Validated M build() throws ValidationException;

    /**
     * Constructs the message with the given fields without validation.
     *
     * <p>Users should prefer {@link #build()} over this method.
     *
     * @return the build message, potentially invalid
     */
    @Override
    @NonValidated M buildPartial();

    /**
     * Probes the current content of this builder for validity.
     *
     * <p>The content is validated in place — including content that is already
     * invalid, e.g., assembled from untrusted input or restored via
     * {@link Message#toBuilder() toBuilder()} from a message that no longer
     * satisfies its constraints.
     *
     * <p>Unlike {@link #build()}, this method does not throw
     * {@link ValidationException} if the content is invalid. Any constraint
     * violations found are reported via the returned list instead. Unlike
     * {@link #buildPartial()}, which skips validation entirely, this method does
     * run validation, but without handing over a potentially invalid message.
     *
     * <p>Calling this method does not modify the content of this builder.
     *
     * <p>If the message under construction does not support validation, i.e., does not
     * implement {@link ValidatableMessage}, its content is considered valid.
     *
     * @return the violations of the constraints declared in Protobuf,
     *         or an empty list if the current content is valid
     */
    default List<ConstraintViolation> validate() {
        var message = buildPartial();
        if (message instanceof ValidatableMessage validatable) {
            return validatable.validate()
                    .map(ValidationError::getConstraintViolationList)
                    .orElse(ImmutableList.of());
        }
        return ImmutableList.of();
    }

    /**
     * Constructs the message and {@linkplain Validate validates} it according to the constraints
     * declared in Protobuf.
     *
     * @return the built message
     * @throws ValidationException
     *         if the message is invalid
     * @deprecated please use {@link #build()}
     */
    @Deprecated
    default @Validated M vBuild() throws ValidationException {
        return build();
    }
}
