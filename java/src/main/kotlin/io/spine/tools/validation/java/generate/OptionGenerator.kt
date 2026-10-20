/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
     * The order of the returned code defines the order of this option's constraints
     * in the generated validation method. Return the code in a stable order, such as
     * the order of declaration of the fields, so that the generated code stays
     * the same from build to build. Note that the view states selected via [querying]
     * come in no particular order.
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
