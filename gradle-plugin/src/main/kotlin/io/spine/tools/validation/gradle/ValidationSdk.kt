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

package io.spine.tools.validation.gradle

import io.spine.tools.meta.LazyMeta
import io.spine.tools.meta.MavenArtifact
import io.spine.tools.meta.Module

/**
 * The meta information about the Validation Gradle Plugin.
 */
public object Meta : LazyMeta(Module("io.spine.tools", "validation-gradle-plugin"))

/**
 * Artifacts of the Spine Validation SDK.
 */
@Suppress("ConstPropertyName")
public object ValidationSdk {

    private const val toolsGroup = "io.spine.tools"
    private const val prefix = "validation"
    private val javaCodegenBundle = Module(toolsGroup, "$prefix-java-bundle")
    private val jvmRuntime = Module("io.spine", "$prefix-java-runtime")
    private val configuration = Module(toolsGroup, "$prefix-configuration")

    private fun MavenArtifact.withVersion(version: String): MavenArtifact {
        version.ifEmpty {
            return this
        }
        return MavenArtifact(group, name, version, classifier, extension)
    }

    /**
     * The Maven artifact containing the `spine-validation-java-bundle` module.
     *
     * @param version The version of the Validation library to be used.
     *        If empty, the version of the build-time dependency is used.
     */
    @JvmStatic
    public fun javaCodegenBundle(version: String = ""): MavenArtifact =
        Meta.dependency(javaCodegenBundle).withVersion(version)

    /**
     * The Maven artifact containing the `spine-validation-java-runtime` module.
     *
     * @param version The version of the Validation library to be used.
     *        If empty, the version of the build-time dependency is used.
     * @see javaCodegenBundle
     */
    @JvmStatic
    public fun jvmRuntime(version: String = ""): MavenArtifact =
        Meta.dependency(jvmRuntime).withVersion(version)

    /**
     * The Maven artifact containing the `spine-validation-configuration` module.
     *
     * @param version The version of the Validation library to be used.
     *        If empty, the version of the build-time dependency is used.
     * @see javaCodegenBundle
     */
    @JvmStatic
    public fun configuration(version: String = ""): MavenArtifact =
        Meta.dependency(configuration).withVersion(version)
}
