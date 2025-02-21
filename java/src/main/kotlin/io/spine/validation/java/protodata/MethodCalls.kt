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

package io.spine.validation.java.protodata

import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.MethodCall

/**
 * Constructs a method call upon this [Expression].
 *
 * @param name The name of the method.
 * @param arguments The method arguments.
 * @param generics The method type parameters.
 */
@JvmOverloads
public fun <T> Expression<*>.call(
    name: String,
    arguments: List<Expression<*>> = listOf(),
    generics: List<ClassName> = listOf()
): MethodCall<T> = MethodCall(this, name, arguments, generics)

/**
 * Constructs a method call upon this [Expression].
 *
 * @param name The name of the method.
 * @param argument The method argument.
 * @param generics The method type parameters.
 */
@JvmOverloads
public fun <T> Expression<*>.call(
    name: String,
    argument: Expression<*>,
    generics: List<ClassName> = listOf()
): MethodCall<T> = MethodCall(this, name, argument, generics)

/**
 * Constructs a method call upon this [Expression].
 *
 * @param name The name of the method.
 * @param argument The method argument.
 * @param generics The method type parameters.
 */
@JvmOverloads
public fun <T> Expression<*>.call(
    name: String,
    vararg argument: Expression<*>,
    generics: List<ClassName> = listOf()
): MethodCall<T> = MethodCall(this, name, argument.asList(), generics)
