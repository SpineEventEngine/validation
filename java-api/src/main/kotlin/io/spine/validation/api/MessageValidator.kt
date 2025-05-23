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

package io.spine.validation.api

import com.google.protobuf.Message
import io.spine.annotation.SPI
import io.spine.base.FieldPath
import io.spine.protodata.ast.TypeName
import io.spine.validate.ConstraintViolation

/**
 * Defines a validator for Protobuf messages of type [M].
 *
 * Implementations should perform domain-specific validation on the given message.
 *
 * @param M the type of Protobuf [Message] being validated.
 */
@SPI
public interface MessageValidator<M : Message> {

    /**
     * Validates the given [message].
     *
     * Please note that this method can be invoked in the scope of another message's validation.
     * Any constraint violations reported by this method must include the path to the original
     * field and the name of the message that initiated in-depth validation if such takes place.
     * For this, [parentPath] and [parentName] are always provided.
     *
     * @param message The message to validate.
     * @param parentPath The path to the parent field that initiated in-depth validation.
     *   Can be the default instance, which means no parent path.
     * @param parentName The name of the parent type that initiated in-depth validation
     *   Can be {@code null}, which means no parent name.
     *
     * @return zero, one or more violations.
     */
    public fun validate(
        message: M,
        parentPath: FieldPath,
        parentName: TypeName?
    ): List<ConstraintViolation>
}
