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

package io.spine.tools.validation.bound

import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.type.TypeSystem

/**
 * Protobuf metadata related to a numeric option, which restricts the allowed
 * range of the field values.
 *
 * This metadata is required to parse and validate the option value.
 *
 * @param optionName The name of the option.
 * @param field The field to which the option is applied.
 * @param fieldType The field type.
 * @param file The file that contains the field declaration.
 * @param typeSystem The type system used to resolve field references, if any.
 *
 * @see [NumericBoundParser.parse]
 */
internal open class NumericOptionMetadata(
    val optionName: String,
    val field: Field,
    val fieldType: PrimitiveType,
    val file: File,
    val typeSystem: TypeSystem
)
