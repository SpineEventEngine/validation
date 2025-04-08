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

import com.google.protobuf.Timestamp
import io.spine.core.External
import io.spine.core.Where
import io.spine.protobuf.unpack
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.File
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.extractMessageType
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.check
import io.spine.protodata.java.javaClass
import io.spine.protodata.java.javaClassName
import io.spine.protodata.plugin.Policy
import io.spine.protodata.type.TypeSystem
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.time.Temporal
import io.spine.time.validation.TimeOption
import io.spine.validation.event.SimpleRuleAdded

/**
 * A policy which, upon encountering a field with the `(when)` option, generates
 * a validation rule.
 *
 * The validation rule ensures that the associated field value is in the future or in the past
 * from the current time (depending on the option definition).
 */
internal class WhenPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = WHEN)
        event: FieldOptionDiscovered
    ): Just<SimpleRuleAdded> {
        val field = event.subject
        val file = event.file
        checkFieldType(field, typeSystem, file)


        val option = event.option.value.unpack<TimeOption>()
        val futureOrPast = option.getIn()
        val feature = inTime { time = futureOrPast }
        val errorMessage = "The time must be in the ${futureOrPast.name.lowercase()}."
        val newRule = SimpleRule(
            field.name,
            feature,
            errorMessage,
            errorMessage,
            true
        )
        return simpleRuleAdded(field.declaringType, newRule)
    }
}

private fun checkFieldType(field: Field, typeSystem: TypeSystem, file: File) =
    Compilation.check(field.type.isSupported(typeSystem), file, field.span) {
        "The field type `${field.type.name}` of the `${field.qualifiedName}` field" +
                " is not supported by the `($GOES)` option. Supported field types: messages," +
                " enums, strings, bytes, repeated, and maps."
    }

/**
 * Tells if this [FieldType] can be validated with the `(when)` option.
 */
private fun FieldType.isSupported(typeSystem: TypeSystem): Boolean {
    if (!isMessage) {
        return false
    }

    val fieldClass = message.javaClassName(typeSystem)
        .javaClass() ?: return false

    return fieldClass == Timestamp::class.java || Temporal::class.java.isAssignableFrom(fieldClass)
}
