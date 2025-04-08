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
import io.spine.protodata.ast.File
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.ref
import io.spine.protodata.check
import io.spine.protodata.java.javaClass
import io.spine.protodata.plugin.Policy
import io.spine.protodata.type.TypeSystem
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.time.Temporal
import io.spine.time.validation.Time
import io.spine.time.validation.TimeOption
import io.spine.validation.event.WhenFieldDiscovered
import io.spine.validation.event.whenFieldDiscovered
import io.spine.validation.protodata.findJavaClassName

/**
 * Controls whether a field should be validated with the `(when)` option.
 *
 * Whenever a field marked with the `(when)` options is discovered, emits
 * [WhenFieldDiscovered] event if the following conditions are met:
 *
 * 1) The field type is supported by the option.
 * 2) The option value is other than [Time.TIME_UNDEFINED].
 *
 * If (1) is violated, the policy reports a compilation error.
 *
 * Violation of (2) means that the `(when)` option is applied correctly,
 * but disabled. In this case, the policy emits [NoReaction].
 */
internal class WhenPolicy : Policy<FieldOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = WHEN)
        event: FieldOptionDiscovered
    ): EitherOf2<WhenFieldDiscovered, NoReaction> {
        val field = event.subject
        val file = event.file
        val timeType = checkFieldType(field, typeSystem, file)

        val option = event.option.value.unpack<TimeOption>()
        val timeBound = option.`in`
        if (timeBound == Time.TIME_UNDEFINED) {
            return ignore()
        }

        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return whenFieldDiscovered {
            id = field.ref
            subject = field
            errorMessage = message
            bound = timeBound
            type = timeType
        }.asA()
    }
}

private fun checkFieldType(field: Field, typeSystem: TypeSystem, file: File): TimeFieldType {
    val fieldType = field.type
    val javaClass = typeSystem.findJavaClassName(fieldType.message)?.javaClass()
    val timeType = when {
        javaClass == null -> TimeFieldType.WFT_UNKNOWN
        javaClass == Timestamp::class.java -> TimeFieldType.WFT_TimeStamp
        Temporal::class.java.isAssignableFrom(javaClass) -> TimeFieldType.WFT_Temporal
        else -> TimeFieldType.WFT_UNKNOWN
    }
    Compilation.check(timeType != TimeFieldType.WFT_UNKNOWN, file, field.span) {
        "The field type `${field.type.name}` of the `${field.qualifiedName}` field" +
                " is not supported by the `($GOES)` option. Supported field types: messages," +
                " enums, strings, bytes, repeated, and maps."
    }
    return timeType
}
