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

import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.isSingular
import io.spine.protodata.ast.name
import io.spine.protodata.java.Expression
import io.spine.protodata.java.call
import io.spine.validation.java.protodata.call

/**
 * Returns an expression that converts the provided field [value] to [String].
 *
 * How the value is converted to [String] is determined by this [FieldType].
 */
internal fun FieldType.stringValueOf(value: Expression<*>): Expression<String> =
    when {
        isSingular -> when {
            isMessage || isEnum -> value.stringify()
            isPrimitive -> value.stringifyPrimitive(primitive)
            else -> error(
                "Cannot convert `$value` expression to `String` expression. " +
                        "Unsupported field type: `${name}`."
            )
        }
        isList || isMap -> value.stringify()
        else -> error(
            "Cannot convert `$value` expression to `String` expression. " +
                    "Unsupported field type: `${name}`."
        )
    }

@Suppress("UNCHECKED_CAST") // Casting string field's value to `String` is safe.
private fun Expression<*>.stringifyPrimitive(primitive: PrimitiveType) =
    when (primitive) {
        TYPE_STRING -> this as Expression<String>
        TYPE_BYTES -> stringify()
        else -> StringClass.call("valueOf", this)
    }

private fun Expression<*>.stringify(): Expression<String> = call("toString")
