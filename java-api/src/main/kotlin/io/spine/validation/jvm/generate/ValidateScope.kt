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

package io.spine.validation.jvm.generate

import io.spine.base.FieldPath
import io.spine.tools.compiler.jvm.ReadVar
import io.spine.type.TypeName
import io.spine.validate.ConstraintViolation

/**
 * Scope variables available within the `validate(FieldPath)` method.
 *
 * Use these variables to create an instance of [ConstraintViolation]
 * for the failed option constraint.
 */
public object ValidateScope {

    /**
     * The list of discovered violations.
     */
    public val violations: ReadVar<MutableList<ConstraintViolation>> = ReadVar("violations")

    /**
     * The field path from the root message field that triggered validation
     * down to the field where the violation occurred.
     *
     * The path is nested when a deep validation takes place.
     */
    public val parentPath: ReadVar<FieldPath> = ReadVar("parentPath")

    /**
     * The name of the message type that triggered validation.
     *
     * The first field of the [parentPath] must be declared in this [parentName].
     */
    public val parentName: ReadVar<TypeName?> = ReadVar("parentName")
}
