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

package io.spine.validation.java

import io.spine.base.FieldPath
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.Expression
import io.spine.validate.ConstraintViolation

/**
 * Generates Java code for a specific option.
 */
internal interface OptionGenerator {

    /**
     * Generates validation code for all option applications within the provided
     * message [type].
     *
     * Note: the provided [parent] and [violations] expressions are expected to be valid
     * only within the scope of `validate()` method. Use them "as is" only within
     * the returned [constraint blocks][OptionCode.constraints]. Bypass their values
     * to [additional members][OptionCode.members] explicitly through the method parameters
     * or field re-assignments.
     *
     * @param type The message to generate code for.
     * @param parent A reference to the parent field path.
     * @param violations A reference to a list of discovered violations.
     */
    fun codeFor(
        type: TypeName,
        parent: Expression<FieldPath>,
        violations: Expression<MutableList<ConstraintViolation>>
    ): OptionCode
}
