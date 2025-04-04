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

import com.google.protobuf.Message
import io.spine.protobuf.field
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.findOption
import io.spine.protodata.protobuf.descriptor

/**
 * Resolves the error message for the field option of type [T] and the provided [field].
 *
 * Boolean options declare their error messages using so-called companion options.
 * For example, the `(required)` option has an `(if_missing)` companion that holds
 * the default error message template for `(required)` violations and allows specifying
 * a custom one as follows: `(if_missing).error_msg = "Game is over!"`.
 *
 * This method considers two cases:
 *
 * 1. If the passed [field] has the option of type [T] applied, the value of its
 * `error_msg` field is returned (companion options must declare this field).
 * 2. If the passed [field] does NOT have such an option applied,
 * the [default error message][DefaultErrorMessage] for [T] is returned.
 */
internal inline fun <reified T : Message> resolveErrorMessage(field: Field): String {
    val descriptor = T::class.descriptor
    val option = field.findOption<T>() ?: return descriptor.defaultMessage
    val customMessageField = descriptor.field(CUSTOM_MESSAGE_FIELD)
    val customMessage = option.getField(customMessageField) as String
    return customMessage
}

/**
 * The name of the field that contains a custom error message.
 *
 * This name is a convention, which all companion error message options
 * are expected to follow.
 */
private const val CUSTOM_MESSAGE_FIELD = "error_msg"
