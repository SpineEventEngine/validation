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

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.base.FieldPath;
import io.spine.type.TypeName;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A message with validation constraints.
 *
 * <p>Please see {@code spine/options.proto} for the definitions of validation options.
 */
@Immutable
public interface ValidatableMessage extends Message {

    /**
     * Validates this message according to the rules in the Protobuf definition.
     *
     * @return an error or {@link Optional#empty()} if no violations found
     */
    default Optional<ValidationError> validate() {
        var noParentPath = FieldPath.getDefaultInstance();
        return validate(noParentPath, null);
    }

    /**
     * Validates this message according to the rules in the Protobuf definition.
     *
     * <p>Use this overload when validating a message as part of another message's validation.
     * Any constraint violations reported by this method will include the path to the original
     * field and the name of the message that initiated in-depth validation.
     *
     * <p>Note: passing {@code parentPath} without {@code parentName}, or vice versa,
     * does not make sense. Both must either be provided or omitted together.
     *
     * @param parentPath
     *         the path to the parent field that initiated in-depth validation.
     *         Can be the default instance, which means no parent path.
     * @param parentName
     *         the name of the parent type that initiated in-depth validation
     *         Can be {@code null}, which means no parent name.
     *
     * @return an error or {@link Optional#empty()} if no violations found
     */
    Optional<ValidationError> validate(FieldPath parentPath, @Nullable TypeName parentName);
}
