/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.kotlin.dsl.ScriptHandlerScope

/**
 * Forces the version of McJava and ProtoData for a module.
 *
 * The usage scenario is put the following code snippet at the top of a `build.gradle.kts` file:
 *
 * ```kotlin
 * buildscript {
 *     forceCodegenPlugins()
 * }
 * ```
 */
fun ScriptHandlerScope.forceCodegenPlugins() {
    standardSpineSdkRepositories()
    dependencies {
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib)
        classpath(io.spine.internal.dependency.ProtoData.pluginLib)
    }
    configurations.forceProtoData()
}

/**
 * Forces the version of McJava and ProtoData for a module with this `ConfigurationContainer`.
 */
fun ConfigurationContainer.forceProtoData() = all {
    resolutionStrategy {
        val protoData = io.spine.internal.dependency.ProtoData
        force(
            protoData.fatCli,
            protoData.pluginLib,
            protoData.java,
            protoData.backend,
            protoData.protocPlugin,
        )
    }
}
