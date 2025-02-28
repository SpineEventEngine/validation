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

package io.spine.validation.protodata

import com.google.protobuf.Message
import io.spine.protobuf.defaultInstance
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.findOption
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.ast.unpack
import io.spine.string.simply
import io.spine.type.typeName

/**
 * Finds the option with the given type [T] applied to this [Field].
 *
 * @param T The type of the option.
 * @return the option or `null` if there is no option with such a type applied to the field.
 * @see Field.option
 */
public inline fun <reified T : Message> Field.findOption(): T? {
    val typeUrl = T::class.java.defaultInstance.typeName.toUrl().value()
    val option = optionList.find { opt ->
        opt.value.typeUrl == typeUrl
    }
    return option?.unpack()
}

/**
 * Obtains the option with the given type [T] applied to this [Field].
 *
 * Invoke this function if you are sure the option with the type [T] is applied
 * to the receiver field. Otherwise, please use [findOption].
 *
 * @param T The type of the option.
 * @return the option.
 * @throws IllegalStateException if the option is not found.
 * @see Field.findOption
 */
public inline fun <reified T : Message> Field.option(): T = findOption<T>()
    ?: error("The field `${qualifiedName}` must have the `${simply<T>()}` option.")
