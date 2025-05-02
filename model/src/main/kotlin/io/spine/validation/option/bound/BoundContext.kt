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

package io.spine.validation.option.bound

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.PrimitiveType
import io.spine.validation.RANGE

/**
 * The context of validating a numeric option that constrains a field's value
 * with a minimum or maximum bound.
 *
 * Contains the data required to report a compilation error for the option.
 */
internal open class BoundContext(
    val optionName: String,
    val primitiveType: PrimitiveType,
    val field: Field,
    val file: File
)

/**
 * The [BoundContext] for the `(range)` option.
 *
 * Introduces the [range] property to report the originally passed range definition
 * in compilation errors.
 */
internal class RangeContext(
    val range: String,
    primitiveType: PrimitiveType,
    field: Field,
    file: File
) : BoundContext(RANGE, primitiveType, field, file)
