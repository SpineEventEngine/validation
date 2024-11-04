/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.validation

import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.Type
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.toType
import io.spine.string.shortly

/**
 * Extracts direct or element type information from this field type.
 */
public fun FieldType.extractType(): Type = when {
    isMessage -> toType()
    isEnum -> toType()
    isPrimitive -> toType()
    isMap -> map.valueType
    isList -> list
    else -> error("Cannot get type info from the field type ${this.shortly()}.")
}

/**
 * Indicates if this field type is a message, or it refers to a message type being
 * a list or a map with such.
 *
 * @see refersToAny
 */
public fun FieldType.refersToMessage(): Boolean = when {
    isMessage -> true
    isMap -> map.valueType.isMessage
    isList -> list.isMessage
    else -> false
}

/**
 * Indicates if this field type refers to [com.google.protobuf.Any].
 *
 * @see refersToMessage
 */
public fun FieldType.refersToAny(): Boolean = when {
    isAny -> true
    isMap -> map.valueType.isAny
    isList -> list.isAny
    else -> false
}
