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

package io.spine.validation.bound

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.type.TypeSystem
import io.spine.validation.RANGE

/**
 * A container for auxiliary data required to parse and validate
 * a [numeric bound][KotlinNumericBound] from a string value.
 *
 * @param optionName The name of the option that contains the bound definition.
 * @param field The field, to which the option is applied.
 * @param fieldType The field type.
 * @param file The file that contains the field declaration.
 * @param typeSystem The type system used to resolve field references, if any.
 *
 * @see [NumericBoundDetails.checkNumericBound]
 */
internal open class NumericBoundDetails(
    val optionName: String,
    val field: Field,
    val fieldType: PrimitiveType,
    val file: File,
    val typeSystem: TypeSystem
)

/**
 * A container for auxiliary data required to parse and validate
 * a [numeric bound][KotlinNumericBound] of the `(range)` option.
 *
 * Introduces the [range] property to allow reporting of the originally
 * passed range definition in compilation errors. Although the whole
 * range definition is passed to this class, usually only one bound
 * (lower or upper) is parsed at once. We store the whole definition
 * in details class for error messages, so not to confuse a user.
 *
 * @param range The value of the `(range)` option as passed by a user.
 * @param field The field, to which the option is applied.
 * @param fieldType The field type.
 * @param file The file that contains the field declaration.
 * @param typeSystem The type system used to resolve field references, if any.
 *
 * @see [NumericBoundDetails.checkNumericBound]
 */
internal class RangeBoundDetails(
    val range: String,
    field: Field,
    fieldType: PrimitiveType,
    file: File,
    typeSystem: TypeSystem,
) : NumericBoundDetails(RANGE, field, fieldType, file, typeSystem)
