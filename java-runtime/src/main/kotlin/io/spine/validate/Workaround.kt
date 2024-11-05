/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.validate

import com.google.protobuf.DescriptorProtos.FieldOptions
import com.google.protobuf.DescriptorProtos.MessageOptions
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.google.protobuf.Message
import io.spine.base.CommandMessage
import io.spine.base.EntityState
import io.spine.option.OptionsProto.goes
import io.spine.option.OptionsProto.requiredField

/**
 * This file provides workarounds for supporting validation features that
 * are not yet fully migrated to ProtoData-based code generation.
 */
@Suppress("unused")
private const val ABOUT = ""

internal fun Message.requiresRuntimeValidation(): Boolean =
    (this is EntityState<*>)
            || (this is CommandMessage)
            || hasFieldOption(goes)
            || hasTypeOption(requiredField)

private fun Message.hasFieldOption(option: GeneratedExtension<FieldOptions, *>): Boolean {
    val fieldDescriptors = descriptorForType.fields
    return fieldDescriptors.any {
        it.hasOption(option)
    }
}

private fun FieldDescriptor.hasOption(option: GeneratedExtension<FieldOptions, *>): Boolean =
    when (val value = options.getExtension(option)) {
        is Boolean -> value
        is String -> value.isNotEmpty()
        is Message -> !option.messageDefaultInstance.equals(value)
        else -> false
    }

private fun Message.hasTypeOption(option: GeneratedExtension<MessageOptions, *>): Boolean {
    val result = descriptorForType.options.hasExtension(option)
    return result
}
