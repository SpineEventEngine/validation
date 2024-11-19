/*
 * Copyright 2022, TeamDev. All rights reserved.
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

@file:JvmName("Options")

package io.spine.validation

import com.google.protobuf.BoolValue
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import io.spine.protobuf.unpack
import io.spine.protodata.ast.Option

/**
 * Checks if this option represents the given generated option.
 *
 * @return `true` if both option name and number are the same, `false` otherwise
 */
@Suppress( "FunctionNaming" /* backticked because `is` is the Kotlin keyword. */ )
public fun Option.`is`(generated: GeneratedExtension<*, *>): Boolean {
    return name == generated.descriptor.name
            && number == generated.number
}

/**
 * Unpacks a [BoolValue] from this option.
 *
 * @throws io.spine.type.UnexpectedTypeException If the option stores a value of another type.
 */
public val Option.boolValue: Boolean
    get() = value.unpack<BoolValue>().value
