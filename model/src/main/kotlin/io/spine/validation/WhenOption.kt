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

package io.spine.validation

import com.google.protobuf.Timestamp
import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.protobuf.unpack
import io.spine.server.entity.alter
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.time.Temporal
import io.spine.time.validation.Time
import io.spine.time.validation.TimeOption
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.FieldRef
import io.spine.tools.compiler.ast.FieldType
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.event.FieldOptionDiscovered
import io.spine.tools.compiler.ast.extractMessageType
import io.spine.tools.compiler.ast.isList
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.ast.ref
import io.spine.tools.compiler.check
import io.spine.tools.compiler.jvm.findJavaClassName
import io.spine.tools.compiler.jvm.javaClass
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.compiler.plugin.View
import io.spine.tools.compiler.type.TypeSystem
import io.spine.validation.ErrorPlaceholder.FIELD_PATH
import io.spine.validation.ErrorPlaceholder.FIELD_TYPE
import io.spine.validation.ErrorPlaceholder.FIELD_VALUE
import io.spine.validation.ErrorPlaceholder.PARENT_TYPE
import io.spine.validation.ErrorPlaceholder.WHEN_IN
import io.spine.validation.TimeFieldType.TFT_TEMPORAL
import io.spine.validation.TimeFieldType.TFT_TIMESTAMP
import io.spine.validation.TimeFieldType.TFT_UNKNOWN
import io.spine.validation.event.WhenFieldDiscovered
import io.spine.validation.event.whenFieldDiscovered

/**
 * Controls whether a field should be validated with the `(when)` option.
 *
 * Whenever a field marked with the `(when)` options is discovered, emits
 * [WhenFieldDiscovered] event if the following conditions are met:
 *
 * 1) The field type is supported by the option.
 * 2) The error message does not contain unsupported placeholders.
 * 3) The option value is other than [Time.TIME_UNDEFINED].
 *
 * If (1) or (2) is violated, the reaction reports a compilation error.
 *
 * Violation of (3) means that the `(when)` option is applied correctly,
 * but effectively disabled. [WhenFieldDiscovered] is not emitted for
 * disabled options. In this case, the reaction emits [NoReaction] meaning
 * that the option is ignored.
 */
internal class WhenReaction : Reaction<FieldOptionDiscovered>() {

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
        message.checkPlaceholders(SUPPORTED_PLACEHOLDERS, field, file, WHEN)

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
    val timeType = typeSystem.determineTimeType(field.type)
    Compilation.check(timeType != TFT_UNKNOWN, file, field.span) {
        "The field type `${field.type.name}` of the `${field.qualifiedName}` field" +
                " is not supported by the `($WHEN)` option. Supported field types:" +
                " `google.protobuf.Timestamp` and types introduced in the `spine.time` package" +
                " that describe time-related concepts."
    }
    return timeType
}

/**
 * Analysis the given [fieldType], determining whether it represents
 * the Protobuf [Timestamp] or Spine [Temporal].
 *
 * For other field types, the method returns [TimeFieldType.TFT_UNKNOWN].
 */
private fun TypeSystem.determineTimeType(fieldType: FieldType): TimeFieldType {
    if (!fieldType.isMessage && !fieldType.isRepeatedMessage) {
        return TFT_UNKNOWN
    }
    val messageType = fieldType.extractMessageType(typeSystem = this)?.name
    val javaClass = messageType?.findJavaClassName(typeSystem = this)?.javaClass()
    return when {
        javaClass == null -> TFT_UNKNOWN
        javaClass == Timestamp::class.java -> TFT_TIMESTAMP
        Temporal::class.java.isAssignableFrom(javaClass) -> TFT_TEMPORAL
        else -> TFT_UNKNOWN
    }
}

/**
 * Tells if this [FieldType] represents a `repeated` of messages.
 *
 * The property is `public` because the option generator also uses it.
 */
public val FieldType.isRepeatedMessage: Boolean
    get() = isList && list.isMessage

/**
 * A view of a field that is marked with the `(when)` option.
 */
internal class WhenFieldView : View<FieldRef, WhenField, WhenField.Builder>() {

    @Subscribe
    fun on(e: WhenFieldDiscovered) = alter {
        subject = e.subject
        errorMessage = e.errorMessage
        bound = e.bound
        type = e.type
    }
}

private val SUPPORTED_PLACEHOLDERS = setOf(
    FIELD_PATH,
    FIELD_TYPE,
    FIELD_VALUE,
    PARENT_TYPE,
    WHEN_IN,
)
