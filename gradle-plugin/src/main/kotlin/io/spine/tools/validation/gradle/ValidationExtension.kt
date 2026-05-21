/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.tools.validation.gradle

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * The extension added to the [RootExtension][io.spine.tools.gradle.root.RootExtension]
 * by the [ValidationGradlePlugin].
 *
 * Configure it under the `spine` block:
 * ```
 * spine {
 *     validation {
 *         enabled = true
 *         java {
 *             warnings {
 *                 unsignedFields = false
 *             }
 *         }
 *     }
 * }
 * ```
 */
public abstract class ValidationExtension @Inject public constructor(project: Project) {

    /**
     * Tells if Validation compiler is enabled in the project.
     *
     * Defaults to `true`. Set to `false` to disable validation code generation
     * for this project, while keeping the runtime dependency.
     */
    public val enabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    /**
     * Configuration for the Java target of the Validation Compiler.
     *
     * Configure via the nested [java] block:
     * ```
     * spine {
     *     validation {
     *         java {
     *             warnings {
     *                 unsignedFields = false
     *             }
     *         }
     *     }
     * }
     * ```
     */
    public val java: Java = project.objects.newInstance(Java::class.java)

    init {
        enabled.convention(true)
    }

    /**
     * Configures the Java target of the Validation Compiler using a Gradle DSL block.
     *
     * Equivalent to mutating [java] directly.
     */
    public fun java(action: Action<Java>) {
        action.execute(java)
    }

    /**
     * Configuration for the Java target of the Validation Compiler.
     *
     * Holds per-target settings consumed by the
     * `io.spine.tools.validation.java.JavaValidationRenderer`.
     */
    public abstract class Java @Inject public constructor(project: Project) {

        /**
         * Per-kind toggles for warnings emitted by the Java target of the
         * Validation Compiler.
         *
         * Configure via the nested [warnings] block:
         * ```
         * spine {
         *     validation {
         *         java {
         *             warnings {
         *                 unsignedFields = false
         *             }
         *         }
         *     }
         * }
         * ```
         */
        public val warnings: Warnings = project.objects.newInstance(Warnings::class.java)

        /**
         * Configures per-kind warning toggles using a Gradle DSL block.
         *
         * Equivalent to mutating [warnings] directly.
         */
        public fun warnings(action: Action<Warnings>) {
            action.execute(warnings)
        }

        /**
         * Holds the per-kind warning toggles for the Java target of the
         * Validation Compiler.
         *
         * Each property defaults to `true` — the warning is emitted. Set a
         * property to `false` to suppress the warning in the build output
         * without disabling validation itself.
         *
         * The Validation Gradle plugin always writes these values to the
         * Spine Compiler settings directory, so the renderer never has to
         * distinguish "field absent" from "field set to `false`".
         */
        public abstract class Warnings @Inject public constructor(project: Project) {

            /**
             * Whether to emit the "unsigned integer types are not supported
             * in Java" warning for `uint32`/`uint64` fields constrained by
             * `(range)`, `(min)`, or `(max)`.
             *
             * Defaults to `true`. Set to `false` to silence the warning when
             * unsigned integers are used intentionally and the Java-side
             * handling has been considered.
             */
            public val unsignedFields: Property<Boolean> =
                project.objects.property(Boolean::class.java)

            init {
                unsignedFields.convention(true)
            }
        }
    }

    public companion object {

        /**
         * The name of the extension.
         */
        public const val NAME: String = "validation"
    }
}
