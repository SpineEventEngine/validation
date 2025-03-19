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

package io.spine.validation.range

import io.spine.protodata.Compilation
import io.spine.protodata.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_INT64
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT64
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT64
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.validation.RANGE

internal fun ParsingContext.numericBound(value: String, inclusive: Boolean): NumericBound {
    val number = when (primitiveType) {
        TYPE_FLOAT -> value.toFloatOrNull()
        TYPE_DOUBLE -> value.toDoubleOrNull()
        TYPE_INT32, TYPE_SINT32, TYPE_SFIXED32 -> value.toIntOrNull()
        TYPE_INT64, TYPE_SINT64, TYPE_SFIXED64 -> value.toLongOrNull()
        TYPE_UINT32, TYPE_FIXED32 -> value.toUIntOrNull()
        TYPE_UINT64, TYPE_FIXED64 -> value.toULongOrNull()
        else -> error(
            "`NumericBound` cannot be created for `$primitiveType` field type." +
                    " Please make sure `RangePolicy` correctly filtered unsupported field types."
        )
    }
    if (number == null) {
        Compilation.error(file, field.span) {
            "The `($RANGE)` option could not parse the range value `$range` specified for" +
                    " `${field.qualifiedName}` field. The `$value` bound value has" +
                    " an incorrect format. Please make sure the number has a correct format" +
                    " and it is within the range of the field type (`${field.type.name}`)" +
                    " the option is applied to."
        }
    }
    return NumericBound(number, inclusive)
}
