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

package io.spine.tools.validation.java.generate

import io.spine.server.query.Querying
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.type.TypeSystem

/**
 * Generates Java code for a specific option.
 */
public abstract class OptionGenerator {

    /**
     * A component capable of querying states of views.
     *
     * Note that the class inheritors are not responsible for providing [Querying].
     * The instance is [injected][inject] by the Java Validation Plugin before
     * the first invocation of the [codeFor] method.
     */
    protected lateinit var querying: Querying

    /**
     * A type system with the Protobuf types defined in the current code generation pipeline.
     *
     * Note that the class inheritors are not responsible for providing [TypeSystem].
     * The instance is [injected][inject] by the Java Validation Plugin before
     * the first invocation of the [codeFor] method.
     */
    protected lateinit var typeSystem: TypeSystem

    /**
     * Generates validation code for all option applications within the provided
     * message [type].
     *
     * @param type The message to generate code for.
     */
    public abstract fun codeFor(type: TypeName): List<SingleOptionCode>

    /**
     * Injects [Querying] and [TypeSystem] into this instance of [OptionGenerator].
     *
     * Must be called exactly once before the first invocation of [codeFor].
     */
    public fun inject(querying: Querying, typeSystem: TypeSystem) {
        check(!::querying.isInitialized) {
            "`inject()` must be called exactly once on `${this::class.simpleName}`."
        }
        this.querying = querying
        this.typeSystem = typeSystem
    }
}
